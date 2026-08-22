import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";

admin.initializeApp();

const REGION = "africa-south1";

export const adminCreateUser = onCall({ region: REGION }, async (request) => {
  const callerUid = request.auth?.uid;
  if (!callerUid) {
    throw new HttpsError("unauthenticated", "You must be signed in.");
  }
  const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
  if (callerDoc.data()?.role !== "admin") {
    throw new HttpsError("permission-denied", "Only admins can create users.");
  }
  const { name, email, phone, role } = request.data as {
    name: string; email: string; phone: string; role: string;
  };
  if (!name || !email) {
    throw new HttpsError("invalid-argument", "Name and email are required.");
  }
  const tempPassword = Math.random().toString(36).slice(-10) + "A1!";
  const userRecord = await admin.auth().createUser({
    email: email.trim(),
    password: tempPassword,
    displayName: name.trim(),
  });
  await admin.firestore().collection("users").doc(userRecord.uid).set({
    name: name.trim(),
    email: email.trim(),
    phone: phone ?? "",
    role: role ?? "passenger",
    walletBalanceRwf: 0,
  });
  return { uid: userRecord.uid, tempPassword };
});

export { dispatchOnTripCreated, dispatchOnDriverResponse, dispatchTimeoutSweep } from "./dispatch";
export { onTripStatusChange, onNotificationCreated } from "./notifications";
