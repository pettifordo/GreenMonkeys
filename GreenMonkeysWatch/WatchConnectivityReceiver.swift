import Foundation
import Observation
import WatchConnectivity

/// Receives the phone's snapshot (streak + tonight's plan). Session reminders
/// themselves arrive via standard notification mirroring — no work needed here.
@Observable
final class WatchConnectivityReceiver: NSObject {
    var context: WatchContext?

    private let sessionDelegate = SessionDelegate()

    override init() {
        super.init()
        sessionDelegate.onContext = { [weak self] context in
            Task { @MainActor in
                self?.context = context
            }
        }
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = sessionDelegate
        WCSession.default.activate()
    }

    /// Plain NSObject delegate kept outside the @Observable class — the
    /// Observation macro and WCSessionDelegate's ObjC protocol don't mix well.
    private final class SessionDelegate: NSObject, WCSessionDelegate {
        var onContext: ((WatchContext) -> Void)?

        func session(_ session: WCSession,
                     activationDidCompleteWith activationState: WCSessionActivationState,
                     error: Error?) {
            if let context = WatchContext.decode(from: session.receivedApplicationContext) {
                onContext?(context)
            }
        }

        func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
            if let context = WatchContext.decode(from: applicationContext) {
                onContext?(context)
            }
        }
    }
}
