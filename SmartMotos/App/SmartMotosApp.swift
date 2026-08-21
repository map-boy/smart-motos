import SwiftUI
import FirebaseCore

@main
struct SmartMotosApp: App {
    init() {
        FirebaseApp.configure()
        SmartMessagingService.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
