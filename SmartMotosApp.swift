import SwiftUI
import FirebaseCore

@main
struct SmartMotosApp: App {
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
