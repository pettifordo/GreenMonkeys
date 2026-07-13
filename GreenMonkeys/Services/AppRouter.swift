import Foundation
import Observation

/// Cross-cutting navigation requests (notification taps, URL scheme) land here;
/// the root view observes and presents the right flow.
@Observable
final class AppRouter {
    enum PendingAction: Equatable {
        case recordDrunkVideo(planID: UUID)
        case morningAfter(planID: UUID)
    }

    var pendingAction: PendingAction?

    /// When set, RootView presents the dedicated verdict screen over everything.
    var verdictPlan: SessionPlan?

    /// Bumped after the verdict cover has fully dismissed; every presenting view
    /// unwinds to Home. (Firing while the cover is still animating gets the
    /// navigation pops silently dropped by SwiftUI.)
    var goHomeSignal = 0

    private var pendingGoHome = false

    func finishToHome() {
        pendingGoHome = true
        verdictPlan = nil
    }

    /// Called from the cover's onDismiss, once the animation has completed.
    func consumePendingGoHome() {
        guard pendingGoHome else { return }
        pendingGoHome = false
        goHomeSignal += 1
    }
}
