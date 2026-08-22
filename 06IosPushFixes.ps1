# 06-Ios-Push-Fixes.ps1
# Three iOS push-notification defects. Run from the repo root. Nothing is pushed.
$ErrorActionPreference = 'Stop'
if (-not (Test-Path .git)) { throw 'Run from the repo root.' }

function Save([string]$rel, [string]$text) {
  $full = Join-Path $PWD.Path $rel
  New-Item -ItemType Directory -Force -Path (Split-Path $full -Parent) | Out-Null
  [System.IO.File]::WriteAllText($full, $text)
  Write-Host "  wrote   $rel" -ForegroundColor DarkGreen
}

function Patch([string]$rel, [string]$find, [string]$replace, [string]$guard) {
  $full = Join-Path $PWD.Path $rel
  if (-not (Test-Path $full)) { Write-Host "  SKIP    $rel (not found)" -ForegroundColor Yellow; return }
  $raw  = [System.IO.File]::ReadAllText($full)
  $crlf = $raw.Contains("`r`n")
  $s    = $raw.Replace("`r`n", "`n")
  $f    = $find.Replace("`r`n", "`n")
  $r    = $replace.Replace("`r`n", "`n")
  if ($s.Contains($guard)) { Write-Host "  ok      $rel (already patched)" -ForegroundColor DarkGray; return }
  if (-not $s.Contains($f)) { throw "Anchor not found in $rel." }
  $s = $s.Replace($f, $r)
  if ($crlf) { $s = $s.Replace("`n", "`r`n") }
  [System.IO.File]::WriteAllText($full, $s)
  Write-Host "  patched $rel" -ForegroundColor DarkGreen
}

Write-Host '[06] iOS push notification lifecycle' -ForegroundColor Cyan

Save 'ios\SmartMotos\App\SmartMotosApp.swift' @'
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
'@
Save 'ios\SmartMotos\Services\SmartMessagingService.swift' @'
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

// The delegate callback above only fires when the token is created or rotates, which
// can easily happen before anyone is signed in. Calling this once after sign-in is
// what actually gets the token onto the user's document.
func registerFcmTokenForCurrentUser() {
    guard let uid = Auth.auth().currentUser?.uid else { return }
    Messaging.messaging().token { token, _ in
        guard let token = token else { return }
        Firestore.firestore().collection("users").document(uid).updateData(["fcmToken": token])
    }
}

// Must run BEFORE Auth.signOut(): the write needs the signed-in user's credentials,
// and firestore.rules rejects it once auth is gone. Leaving the token behind means
// this device keeps receiving pushes for an account no longer signed in on it.
func clearFcmTokenForCurrentUser(completion: @escaping () -> Void) {
    guard let uid = Auth.auth().currentUser?.uid else {
        completion()
        return
    }
    Firestore.firestore().collection("users").document(uid)
        .updateData(["fcmToken": FieldValue.delete()]) { _ in completion() }
}
'@

# Register the token once the auth listener sees a signed-in user.
$f1 = @'
            if let uid = user?.uid {
                self.attachListeners(uid: uid)
'@
$r1 = @'
            if let uid = user?.uid {
                self.attachListeners(uid: uid)
                registerFcmTokenForCurrentUser()
'@
Patch 'ios\SmartMotos\Services\SmartRepository.swift' $f1 $r1 'registerFcmTokenForCurrentUser()'

# Clear the token while the credentials to make that write still exist.
$f2 = @'
                    Button(action: {
                        try? FirebaseAuth.Auth.auth().signOut()
                    }) {
'@
$r2 = @'
                    Button(action: {
                        clearFcmTokenForCurrentUser {
                            try? FirebaseAuth.Auth.auth().signOut()
                        }
                    }) {
'@
Patch 'ios\SmartMotos\Views\MainTabView.swift' $f2 $r2 'clearFcmTokenForCurrentUser'

# Android: same ordering constraint, documented (no sign-out entry point yet).
$f3 = @'
    // Clear the token on sign-out so pushes for this account stop landing on a device
    // that is no longer signed in to it.
'@
$r3 = @'
    // Must be called BEFORE FirebaseAuth.signOut(): the write needs the signed-in
    // user's credentials, and firestore.rules rejects it once auth is gone. Not yet
    // wired up because Android has no sign-out entry point; iOS does, and clears it.
    // Leaving the token behind means the device keeps receiving pushes for an account
    // that is no longer signed in on it.
'@
Patch 'android\app\src\main\java\com\nihonor\smartmotosapp\data\SmartRepository.kt' $f3 $r3 'Must be called BEFORE FirebaseAuth.signOut()'

Write-Host '[06] done' -ForegroundColor Green
git status --short
