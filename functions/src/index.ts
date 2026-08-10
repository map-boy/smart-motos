import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";

admin.initializeApp();

const REGION = "africa-south1";

/**
 * Admin-only: create a real Firebase Auth account + Firestore profile
 * for a user added manually from the Admin panel.
 * Generates a temporary password; in production you'd email/SMS it
 * or trigger a "set your password" reset link instead.
 */
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
    role,
    name: name.trim(),
    email: email.trim(),
    phone: (phone ?? "").trim(),
    walletBalanceRwf: 0,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdByAdmin: true,
    mustResetPassword: true,
  });

  // Generate a password-reset link the admin can share with the new user.
  const resetLink = await admin.auth().generatePasswordResetLink(email.trim());

  return { uid: userRecord.uid, tempPassword, resetLink };
});

/**
 * Admin-only: fully delete a user — both their Firebase Auth account
 * AND their Firestore profile. The client-only deleteUser() call could
 * only remove the Firestore doc; this removes the login too.
 */
export const adminDeleteUser = onCall({ region: REGION }, async (request) => {
  const callerUid = request.auth?.uid;
  if (!callerUid) {
    throw new HttpsError("unauthenticated", "You must be signed in.");
  }

  const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
  if (callerDoc.data()?.role !== "admin") {
    throw new HttpsError("permission-denied", "Only admins can delete users.");
  }

  const { userId } = request.data as { userId: string };
  if (!userId) {
    throw new HttpsError("invalid-argument", "userId is required.");
  }

  await admin.firestore().collection("users").doc(userId).delete();

  try {
    await admin.auth().deleteUser(userId);
  } catch (err: any) {
    // Auth account may already be gone or never existed (e.g. legacy Firestore-only docs).
    if (err.code !== "auth/user-not-found") {
      throw err;
    }
  }

  return { success: true };
});
