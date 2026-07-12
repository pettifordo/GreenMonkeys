import Foundation

/// The streak state shared with the widget via the App Group. The widget
/// computes the day count itself from `anchorDate`, so it ticks over at
/// midnight without the app being opened. Only the anchor and the word are
/// shared — never any video or session content.
struct StreakSnapshot: Codable {
    /// sessionStart of the most recent idiot-verdict night, if any.
    var anchorDate: Date?
    var hasIdiotHistory: Bool
    var firstUseDate: Date
    var insultWord: String

    static let appGroupID = "group.com.strive4it.greenmonkeys"
    private static let key = "streakSnapshot"

    func days(now: Date = Date()) -> Int {
        StreakService.daysSince(lastIdiotDate: anchorDate, firstUseDate: firstUseDate, now: now)
    }

    func save() {
        guard let defaults = UserDefaults(suiteName: Self.appGroupID),
              let data = try? JSONEncoder().encode(self) else { return }
        defaults.set(data, forKey: Self.key)
    }

    static func load() -> StreakSnapshot {
        if let defaults = UserDefaults(suiteName: appGroupID),
           let data = defaults.data(forKey: key),
           let snapshot = try? JSONDecoder().decode(StreakSnapshot.self, from: data) {
            return snapshot
        }
        return StreakSnapshot(anchorDate: nil, hasIdiotHistory: false, firstUseDate: Date(), insultWord: "idiot")
    }
}
