import Foundation
import SwiftData

@Model
final class Verdict {
    var id: UUID = UUID()
    /// Self-reported severity, 0–5. 0 = behaved like a normal person,
    /// 5 = can't show my face again. The app never sets this on its own
    /// (SPEC §2, hard rule 4).
    var score: Int = 0
    /// Legacy flag kept in sync with `score > 0` (pre-scale verdicts stored only this).
    var wasIdiot: Bool = false
    /// Booze crimes confessed to — labels from the crime catalog plus customs.
    var crimes: [String] = []
    /// The forward-looking commitment: one thing to change next session. Encouraged, not required.
    var oneChange: String = ""
    var note: String = ""
    var recordedAt: Date = Date()
    var plan: SessionPlan?

    init(score: Int, crimes: [String], oneChange: String, note: String = "") {
        self.id = UUID()
        self.score = score
        self.wasIdiot = score > 0
        self.crimes = crimes
        self.oneChange = oneChange
        self.note = note
        self.recordedAt = Date()
    }

    /// Bridges pre-scale verdicts: a legacy `wasIdiot` with no score counts as 1.
    var effectiveScore: Int {
        max(score, wasIdiot ? 1 : 0)
    }
}
