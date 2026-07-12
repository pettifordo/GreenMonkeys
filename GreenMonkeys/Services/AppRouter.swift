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
}
