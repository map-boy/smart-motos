import SwiftUI
import UIKit
import FirebaseCore
import FirebaseMessaging

// SwiftUI's App lifecycle provides no UIApplicationDelegate of its own, so without
// this adaptor nothing ever calls registerForRemoteNotifications: the device never
// receives an APNs token, and FCM has nothing to deliver to on real hardware no
// matter how correctly the token is stored in Firestore.
class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        SmartMessagingService.shared.configure()
        application.registerForRemoteNotifications()
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs registration failed: \(error.localizedDescription)")
    }
}

@main
struct SmartMotosApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
