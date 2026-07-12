import Foundation
import SwiftUI

/// User-configurable settings, backed by UserDefaults via AppStorage keys.
/// Kept as a single source of key names so views and services agree.
enum SettingsKey {
    static let insultWord = "insultWord"
    static let brutality = "brutality"
    static let morningAfterHour = "morningAfterHour"
    static let appLockEnabled = "appLockEnabled"
    static let firstUseDate = "firstUseDate"
}

enum AppSettings {
    // Kept deliberately mild — anyone wanting a truly terrible word can type their own.
    static let insultPresets = ["idiot", "drunk", "embarrassment", "liability"]

    static var insultWord: String {
        UserDefaults.standard.string(forKey: SettingsKey.insultWord) ?? "idiot"
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

    /// Anchor for the streak before any idiot verdict exists. Set once on first launch.
    static var firstUseDate: Date {
        if let stored = UserDefaults.standard.object(forKey: SettingsKey.firstUseDate) as? Date {
            return stored
        }
        let now = Date()
        UserDefaults.standard.set(now, forKey: SettingsKey.firstUseDate)
        return now
    }
}
