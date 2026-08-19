import Foundation
import UserNotifications
import FirebaseMessaging
import FirebaseFirestore
import FirebaseAuth

class SmartMessagingService: NSObject, UNUserNotificationCenterDelegate, MessagingDelegate {

    static let shared = SmartMessagingService()

    func configure() {
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: authOptions) { _, _ in }
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, let uid = Auth.auth().currentUser?.uid else { return }
        Firestore.firestore().collection("users").document(uid).updateData(["fcmToken": token])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }
}

func registerFcmTokenForCurrentUser() {
    guard let uid = Auth.auth().currentUser?.uid else { return }
    Messaging.messaging().token { token, error in
        if let token = token {
            Firestore.firestore().collection("users").document(uid).updateData(["fcmToken": token])
        }
    }
}
