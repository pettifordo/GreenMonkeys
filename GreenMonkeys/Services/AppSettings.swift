import Foundation
import SwiftUI

/// User-configurable settings, backed by UserDefaults via AppStorage keys.
/// Kept as a single source of key names so views and services agree.
enum SettingsKey {
    static let insultWord = "insultWord"
    static let sessionNoun = "sessionNoun"
    static let brutality = "brutality"
    static let morningAfterHour = "morningAfterHour"
    static let appLockEnabled = "appLockEnabled"
    static let firstUseDate = "firstUseDate"
    static let seedLongestStreak = "seedLongestStreak"
    static let hasOnboarded = "hasOnboarded"
    /// Self-declared current-streak start, as timeIntervalSince1970. 0 = unset.
    static let streakStartTime = "streakStartTime"
    /// How many times the start date has been moved — feeds escalating snark.
    static let streakStartEdits = "streakStartEdits"
}

enum AppSettings {
    // Kept deliberately mild — anyone wanting a truly terrible word can type their own.
    static let insultPresets = ["idiot", "drunk", "embarrassment", "liability"]

    static var insultWord: String {
        UserDefaults.standard.string(forKey: SettingsKey.insultWord) ?? "idiot"
    }

    static let sessionNounPresets = ["Session", "Night Out", "Drink", "Cheeky One", "Risk Event"]

    /// What the user calls a drinking occasion — drives the + button, empty
    /// states, and titles ("Plan a Night Out", "No Cheeky One planned").
    static var sessionNoun: String {
        UserDefaults.standard.string(forKey: SettingsKey.sessionNoun) ?? "Session"
    }

    static var brutality: Brutality {
        Brutality(rawValue: UserDefaults.standard.integer(forKey: SettingsKey.brutality)) ?? .standard
    }

    static var morningAfterHour: Int {
        let stored = UserDefaults.standard.integer(forKey: SettingsKey.morningAfterHour)
        return stored == 0 ? 9 : stored
    }

    static var appLockEnabled: Bool {
        // Default ON: this app holds videos of you drunk (SPEC §5).
        UserDefaults.standard.object(forKey: SettingsKey.appLockEnabled) as? Bool ?? true
    }

    /// A pre-app personal record, self-declared in Settings — the challenge to
    /// beat. The app's own history can only start from install day.
    static var seedLongestStreak: Int {
        max(0, UserDefaults.standard.integer(forKey: SettingsKey.seedLongestStreak))
    }

    /// Install date. Set once on first launch.
    static var firstUseDate: Date {
        if let stored = UserDefaults.standard.object(forKey: SettingsKey.firstUseDate) as? Date {
            return stored
        }
        let now = Date()
        UserDefaults.standard.set(now, forKey: SettingsKey.firstUseDate)
        return now
    }

    /// The user's self-declared current-streak start, if they set one in
    /// onboarding or Settings. nil = never set (count from install).
    static var streakStartDate: Date? {
        let stored = UserDefaults.standard.double(forKey: SettingsKey.streakStartTime)
        return stored > 0 ? Date(timeIntervalSince1970: stored) : nil
    }

    /// The clean-run anchor used everywhere streak days are computed:
    /// the declared start if given, otherwise install day.
    static var streakAnchorDate: Date {
        streakStartDate ?? firstUseDate
    }
}
