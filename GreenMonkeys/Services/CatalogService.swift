import Foundation

/// User-extendable lists: custom promises and the booze-crime charge sheet.
/// Anything typed once while planning or confessing is remembered for later
/// (SPEC §1.4 — the Monkeys never forget).
enum CatalogService {

    private static let customPromisesKey = "customPromises"
    private static let customCrimesKey = "customCrimes"

    // MARK: - Promises

    static var customPromises: [String] {
        UserDefaults.standard.stringArray(forKey: customPromisesKey) ?? []
    }

    static func rememberPromise(_ text: String) {
        remember(text, key: customPromisesKey, existing: customPromises)
    }

    static func removeCustomPromise(_ text: String) {
        UserDefaults.standard.set(customPromises.filter { $0 != text }, forKey: customPromisesKey)
    }

    // MARK: - Booze crimes

    static let builtInCrimes = [
        "Blacked out",
        "Insulted someone",
        "Offended someone",
        "Threw up",
        "Damaged a relationship",
        "Got in a fight",
        "Criminal offence",
        "Drunk texting",
        "Lost phone / wallet / keys",
    ]

    static var customCrimes: [String] {
        UserDefaults.standard.stringArray(forKey: customCrimesKey) ?? []
    }

    static var allCrimes: [String] {
        builtInCrimes + customCrimes
    }

    static func rememberCrime(_ text: String) {
        let all = builtInCrimes + customCrimes
        guard !all.contains(where: { $0.caseInsensitiveCompare(text) == .orderedSame }) else { return }
        remember(text, key: customCrimesKey, existing: customCrimes)
    }

    static func removeCustomCrime(_ text: String) {
        UserDefaults.standard.set(customCrimes.filter { $0 != text }, forKey: customCrimesKey)
    }

    // MARK: - Private

    private static func remember(_ text: String, key: String, existing: [String]) {
        let trimmed = text.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty,
              !existing.contains(where: { $0.caseInsensitiveCompare(trimmed) == .orderedSame }) else { return }
        UserDefaults.standard.set(existing + [trimmed], forKey: key)
    }
}
