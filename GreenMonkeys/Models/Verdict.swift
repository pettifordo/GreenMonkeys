import Foundation
import SwiftData

@Model
final class Verdict {
    var id: UUID = UUID()
    /// Self-reported. The app never sets this on its own (SPEC §2, hard rule 4).
    var wasIdiot: Bool = false
    /// The forward-looking commitment: one thing to change next session.
    var oneChange: String = ""
    var note: String = ""
    var recordedAt: Date = Date()
    var plan: SessionPlan?

    init(wasIdiot: Bool, oneChange: String, note: String = "") {
        self.id = UUID()
        self.wasIdiot = wasIdiot
        self.oneChange = oneChange
        self.note = note
        self.recordedAt = Date()
    }
}
