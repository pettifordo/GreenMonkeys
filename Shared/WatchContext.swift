import Foundation

/// Snapshot pushed from phone to Watch via WCSession application context.
/// Compiled into both targets — keep Foundation-only.
struct WatchContext: Codable {
    var streakDays: Int
    var insultWord: String
    /// Occasion of tonight's (planned or active) session, if any.
    var sessionOccasion: String?
    var sessionStart: Date?
    /// Display strings of the commitments for tonight's session.
    var commitments: [String]

    static let contextKey = "watchContext"

    func encoded() -> [String: Any] {
        guard let data = try? JSONEncoder().encode(self) else { return [:] }
        return [Self.contextKey: data]
    }

    static func decode(from context: [String: Any]) -> WatchContext? {
        guard let data = context[contextKey] as? Data else { return nil }
        return try? JSONDecoder().decode(WatchContext.self, from: data)
    }
}
