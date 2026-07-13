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

    /// Bumped by "Finish — go face the day"; every presenting view unwinds to Home.
    var goHomeSignal = 0

    func finishToHome() {
        verdictPlan = nil
        goHomeSignal += 1
    }
}
