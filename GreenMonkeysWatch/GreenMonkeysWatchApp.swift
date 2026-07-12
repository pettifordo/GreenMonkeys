import SwiftUI

@main
struct GreenMonkeysWatchApp: App {
    @State private var receiver = WatchConnectivityReceiver()

    var body: some Scene {
        WindowGroup {
            WatchContentView()
                .environment(receiver)
        }
    }
}
