import * as admin from "firebase-admin";
import { onDocumentUpdated, onDocumentCreated } from "firebase-functions/v2/firestore";

const REGION = "africa-south1";

function db() {
  return admin.firestore();
}
function messaging() {
  return admin.messaging();
}

// Status strings match dispatch.ts exactly - keep these two files in sync.
const STATUS_MESSAGES: Record<string, { title: string; body: string }> = {
  OFFERED: { title: "Finding your driver", body: "We are contacting a nearby driver for you." },
  DRIVER_ASSIGNED: { title: "Driver Assigned", body: "A driver is on the way to your pickup location." },
  IN_PROGRESS: { title: "Ride Started", body: "Your ride is now in progress." },
  COMPLETED: { title: "Ride Completed", body: "You have arrived. Thanks for riding with Smart Motos!" },
  CANCELLED: { title: "Ride Cancelled", body: "Your ride has been cancelled." },
  NO_DRIVER_FOUND: { title: "No Drivers Available", body: "We could not find a driver nearby. Please try again shortly." },
};

async function sendPushToUser(
  userId: string | undefined,
  title: string,
  body: string,
  data: Record<string, string> = {}
) {
  if (!userId) return;
  const userDoc = await db().collection("users").doc(userId).get();
  const token = userDoc.data()?.fcmToken as string | undefined;
  if (!token) {
    console.log(`No fcmToken for user ${userId}, skipping push.`);
    return;
  }
  try {
    await messaging().send({
      token,
      notification: { title, body },
      data,
      android: { priority: "high" },
    });
  } catch (err: any) {
    console.error(`Failed to send push to ${userId}:`, err.message);
  }
}

export const onTripStatusChange = onDocumentUpdated(
  { document: "trips/{tripId}", region: REGION },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    const tripId = event.params.tripId;
    if (!before || !after) return;
    if (before.status === after.status) return;

    const statusInfo = STATUS_MESSAGES[after.status];
    if (!statusInfo) return;

    const { title, body } = statusInfo;
    const riderId = after.riderId as string | undefined;

    await Promise.all([
      sendPushToUser(riderId, title, body, { tripId, type: "trip", status: after.status }),
      db().collection("notifications").add({
        userId: riderId,
        userType: "passenger",
        title,
        message: body,
        type: "trip",
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      }),
    ]);

    if (after.status === "OFFERED" && after.offeredDriverId) {
      await sendPushToUser(
        after.offeredDriverId,
        "New Ride Request",
        "You have a new ride offer waiting.",
        { tripId, type: "trip", status: after.status }
      );
    }
    if (after.status === "DRIVER_ASSIGNED" && after.driverId) {
      await sendPushToUser(
        after.driverId,
        "Ride Confirmed",
        "You are now assigned to this ride.",
        { tripId, type: "trip", status: after.status }
      );
    }
  }
);

export const onNotificationCreated = onDocumentCreated(
  { document: "notifications/{notificationId}", region: REGION },
  async (event) => {
    const notif = event.data?.data();
    if (!notif?.userId || notif.type === "trip") return;
    await sendPushToUser(notif.userId, notif.title, notif.message, { type: notif.type || "general" });
  }
);

