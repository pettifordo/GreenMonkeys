import Foundation
import WatchConnectivity

/// Pushes the streak + tonight's plan to the Watch. Fire-and-forget: the
/// application context always holds the latest snapshot.
final class PhoneConnectivityService: NSObject {
    static let shared = PhoneConnectivityService()

    func activate() {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    func push(_ context: WatchContext) {
        guard WCSession.isSupported(), WCSession.default.activationState == .activated else { return }
        try? WCSession.default.updateApplicationContext(context.encoded())
    }
}

// MARK: - WCSessionDelegate

extension PhoneConnectivityService: WCSessionDelegate {
    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {}
    func sessionDidBecomeInactive(_ session: WCSession) {}
    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
}
