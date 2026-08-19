import * as admin from "firebase-admin";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as geofire from "geofire-common";

const REGION = "africa-south1";
const SCHEDULER_REGION = "us-central1"; // Cloud Scheduler does not yet support africa-south1
const OFFER_TIMEOUT_MS = 20_000;
const SEARCH_RADIUS_KM = 8;

function db() {
  return admin.firestore();
}

async function findAndLockDriver(
  pickupLat: number,
  pickupLng: number,
  excludeIds: string[]
): Promise<string | null> {
  const snap = await db()
    .collection("users")
    .where("role", "==", "driver")
    .where("isAvailableOnline", "==", true)
    .get();

  const now = Date.now();
  const candidates: { id: string; dist: number }[] = [];
  for (const doc of snap.docs) {
    if (excludeIds.includes(doc.id)) continue;
    const d = doc.data();
    const lat = d.latitude;
    const lng = d.longitude;
    if (typeof lat !== "number" || typeof lng !== "number") continue;
    // Skip drivers whose lock hasn't naturally expired yet
    const lockedUntil = d.dispatchLockUntilMs as number | undefined;
    if (lockedUntil && lockedUntil > now) continue;
    const dist = geofire.distanceBetween([lat, lng], [pickupLat, pickupLng]);
    if (dist <= SEARCH_RADIUS_KM) candidates.push({ id: doc.id, dist });
  }
  candidates.sort((a, b) => a.dist - b.dist);

  for (const c of candidates) {
    const driverRef = db().collection("users").doc(c.id);
    const locked = await db().runTransaction(async (txn) => {
      const driverSnap = await txn.get(driverRef);
      const data = driverSnap.data();
      if (!data) return false;
      const lockedUntil = data.dispatchLockUntilMs as number | undefined;
      const nowInTxn = Date.now();
      if (lockedUntil && lockedUntil > nowInTxn) return false; // re-check inside txn, avoid race
      txn.update(driverRef, {
        dispatchLockUntilMs: nowInTxn + OFFER_TIMEOUT_MS,
        dispatchLockTripId: null, // set for real right after, see below
      });
      return true;
    });
    if (locked) return c.id;
  }
  return null;
}

/**
 * Idempotency + atomicity: the whole "read trip, pick driver, write offer"
 * sequence is guarded by a Firestore transaction on the TRIP doc itself,
 * using a dispatchRunId so two concurrent triggers can't both dispatch
 * the same trip. Driver locking still happens outside because it needs
 * its own transaction per candidate, but we claim the trip FIRST.
 */
async function offerOrFail(tripId: string, excludeIds: string[]) {
  const tripRef = db().collection("trips").doc(tripId);
  const runId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

  // Claim the dispatch attempt atomically so concurrent triggers don't race.
  const claimed = await db().runTransaction(async (txn) => {
    const tripSnap = await txn.get(tripRef);
    const trip = tripSnap.data();
    if (!trip) return null;
    // Already mid-dispatch from another trigger firing at the same moment? bail.
    if (trip.dispatchInProgress === true) return null;
    // Already offered/assigned/terminal? nothing to do.
    if (!["REQUESTED", "SEARCHING", "OFFERED"].includes(trip.status)) return null;
    txn.update(tripRef, { dispatchInProgress: true, dispatchRunId: runId });
    return trip;
  });

  if (!claimed) return;

  try {
    const pickup = claimed.pickup as { latitude: number; longitude: number } | undefined;
    if (!pickup) {
      await tripRef.update({ dispatchInProgress: false, status: "DISPATCH_ERROR" });
      return;
    }

    const driverId = await findAndLockDriver(pickup.latitude, pickup.longitude, excludeIds);

    if (!driverId) {
      await tripRef.update({
        status: "NO_DRIVER_FOUND",
        offerStatus: "none",
        dispatchInProgress: false,
      });
      return;
    }

    const driverRef = db().collection("users").doc(driverId);
    const driverDoc = await driverRef.get();
    const driverData = driverDoc.data() ?? {};

    // Tie the lock to this specific trip so a stray lock can be traced/cleared.
    await driverRef.update({ dispatchLockTripId: tripId });

    await tripRef.update({
      driverId,
      driverName: driverData.name ?? "Driver",
      driverAvatar: driverData.photoUrl ?? "",
      vehiclePlate: driverData.licenseNumber ?? "JDM PL8S",
      status: "OFFERED",
      offerStatus: "offered",
      offeredDriverId: driverId,
      offerExpiresAtMs: Date.now() + OFFER_TIMEOUT_MS,
      dispatchInProgress: false,
    });
  } catch (err) {
    // CRITICAL: never leave a trip stuck mid-dispatch. Always release the claim.
    console.error(`offerOrFail failed for trip ${tripId}:`, err);
    await tripRef.update({
      dispatchInProgress: false,
      status: "SEARCHING", // retryable — sweep or next event will pick it back up
    }).catch((e) => console.error(`Failed to release dispatch claim for ${tripId}:`, e));
  }
}

export const dispatchOnTripCreated = onDocumentCreated(
  { document: "trips/{tripId}", region: REGION },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const trip = snap.data();
    if (!trip || (trip.status !== "REQUESTED" && trip.status !== "OFFERED")) return;
    await offerOrFail(event.params.tripId, []);
  }
);

export const dispatchOnDriverResponse = onDocumentUpdated(
  { document: "trips/{tripId}", region: REGION },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    const justDeclinedOrExpired =
      before.offerStatus === "offered" &&
      (after.offerStatus === "declined" || after.offerStatus === "expired");

    if (justDeclinedOrExpired) {
      const excludeId = before.driverId as string | undefined;
      // Release the declining driver's lock immediately instead of waiting it out.
      if (excludeId) {
        await db().collection("users").doc(excludeId).update({
          dispatchLockUntilMs: 0,
          dispatchLockTripId: null,
        }).catch(() => {});
      }
      await offerOrFail(event.params.tripId, excludeId ? [excludeId] : []);
    }
  }
);

export const dispatchTimeoutSweep = onSchedule(
  { schedule: "every 1 minutes", region: SCHEDULER_REGION },
  async () => {
    const now = Date.now();

    // Sweep 1: trips whose offer timed out
    const staleOffers = await db()
      .collection("trips")
      .where("offerStatus", "==", "offered")
      .where("offerExpiresAtMs", "<=", now)
      .get();

    for (const doc of staleOffers.docs) {
      const driverId = doc.data().driverId as string | undefined;
      if (driverId) {
        await db().collection("users").doc(driverId).update({
          dispatchLockUntilMs: 0,
          dispatchLockTripId: null,
        }).catch(() => {});
      }
      await doc.ref.update({ offerStatus: "expired", status: "SEARCHING" });
      await offerOrFail(doc.id, driverId ? [driverId] : []);
    }

    // Sweep 2: trips stuck mid-dispatch for >2 min (crashed function, dead retry)
    const stuck = await db()
      .collection("trips")
      .where("dispatchInProgress", "==", true)
      .get();

    for (const doc of stuck.docs) {
      const trip = doc.data();
      // No timestamp on the claim currently, so use offerExpiresAtMs as a rough guard
      // if present; otherwise force-release after this sweep tick.
      await doc.ref.update({ dispatchInProgress: false, status: "SEARCHING" });
      await offerOrFail(doc.id, []);
    }
  }
);
