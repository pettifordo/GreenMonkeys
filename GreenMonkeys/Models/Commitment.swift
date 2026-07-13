import Foundation
import SwiftData

/// The kinds of promise sober-you can make. Raw values are stored — don't rename.
enum CommitmentKind: String, CaseIterable, Codable {
    case maxDrinks
    case waterBetween
    case leaveBy
    case noDriving
    case eatFirst
    case noShots
    case custom

    var label: String {
        switch self {
        case .maxDrinks:    return "Max drinks"
        case .waterBetween: return "Water every other drink"
        case .leaveBy:      return "Leave by"
        case .noDriving:    return "No driving"
        case .eatFirst:     return "Eat before drinking"
        case .noShots:      return "No shots"
        case .custom:       return "Custom promise"
        }
    }

    var symbol: String {
        switch self {
        case .maxDrinks:    return "🍺"
        case .waterBetween: return "💧"
        case .leaveBy:      return "🕚"
        case .noDriving:    return "🚕"
        case .eatFirst:     return "🍔"
        case .noShots:      return "🥃"
        case .custom:       return "🤙"
        }
    }
}

@Model
final class Commitment {
    var id: UUID = UUID()
    var kindRaw: String = CommitmentKind.custom.rawValue
    /// The specifics: "6" for maxDrinks, "23:00" for leaveBy, free text for custom.
    var detail: String = ""
    /// Set during the morning-after verdict. nil until judged.
    var wasBroken: Bool?
    var plan: SessionPlan?

    init(kind: CommitmentKind, detail: String) {
        self.id = UUID()
        self.kindRaw = kind.rawValue
        self.detail = detail
    }

    var kind: CommitmentKind {
        CommitmentKind(rawValue: kindRaw) ?? .custom
    }

    /// Stable identity for pattern tracking: built-ins group by kind, customs by their text.
    var patternKey: String {
        kind == .custom
            ? detail.trimmingCharacters(in: .whitespaces).lowercased()
            : kind.rawValue
    }

    /// Human name for pattern tracking.
    var patternLabel: String {
        kind == .custom ? detail : kind.label
    }

    var displayText: String {
        switch kind {
        case .maxDrinks:    return "No more than \(detail) drinks"
        case .waterBetween: return "Water every other drink"
        case .leaveBy:      return "Leave by \(detail)"
        case .noDriving:    return "No driving — taxi or lift home"
        case .eatFirst:     return "Eat before drinking"
        case .noShots:      return "No shots"
        case .custom:       return detail
        }
    }
}
