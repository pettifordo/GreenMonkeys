import Foundation
import SwiftData

/// Derived lifecycle state of a plan. Never stored — always computed from dates + verdict.
enum PlanStatus: String {
    case planned
    case active
    case awaitingVerdict
    case completed
}

@Model
final class SessionPlan {
    var id: UUID = UUID()
    /// e.g. "Dave's birthday", "Friday pub"
    var occasion: String = ""
    var sessionStart: Date = Date()
    var plannedEnd: Date = Date()
    /// Reminder times as minute-offsets from sessionStart, chosen at plan time.
    var reminderOffsetsMinutes: [Int] = []
    var createdAt: Date = Date()

    @Relationship(deleteRule: .cascade) var commitments: [Commitment] = []
    @Relationship(deleteRule: .cascade) var videos: [SessionVideo] = []
    @Relationship(deleteRule: .cascade) var verdict: Verdict?

    init(occasion: String, sessionStart: Date, plannedEnd: Date, reminderOffsetsMinutes: [Int]) {
        self.id = UUID()
        self.occasion = occasion
        self.sessionStart = sessionStart
        self.plannedEnd = plannedEnd
        self.reminderOffsetsMinutes = reminderOffsetsMinutes
        self.createdAt = Date()
    }

    // MARK: - Derived state

    /// Grace period after planned end during which the session still counts as live.
    static let graceInterval: TimeInterval = 6 * 3600

    func status(now: Date = Date()) -> PlanStatus {
        if verdict != nil { return .completed }
        if now < sessionStart { return .planned }
        if now <= plannedEnd.addingTimeInterval(Self.graceInterval) { return .active }
        return .awaitingVerdict
    }

    func video(_ kind: VideoKind) -> SessionVideo? {
        videos.first { $0.kind == kind.rawValue }
    }
}
