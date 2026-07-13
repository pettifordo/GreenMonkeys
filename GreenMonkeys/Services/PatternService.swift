import Foundation

/// A judged commitment, reduced to a value type so pattern logic stays pure and
/// testable. `key` groups built-ins by kind and custom promises by their text.
struct CommitmentRecord: Equatable {
    let key: String
    let label: String
    let wasBroken: Bool
}

struct PromiseHistory: Equatable {
    let key: String
    let label: String
    let timesPromised: Int
    let timesBroken: Int

    var timesKept: Int { timesPromised - timesBroken }
}

/// Computes the user's track record: the composite idiot score, which promises
/// they break repeatedly, and the booze-crime charge sheet (SPEC §1.4).
/// Operates on value types only.
enum PatternService {

    /// History for one promise (built-in kind or custom text) across all judged sessions.
    static func history(forKey key: String, in records: [CommitmentRecord]) -> PromiseHistory {
        let matching = records.filter { $0.key == key }
        return PromiseHistory(
            key: key,
            label: matching.first?.label ?? key,
            timesPromised: matching.count,
            timesBroken: matching.filter(\.wasBroken).count
        )
    }

    /// Promises broken more than once, worst first — the repeat offenders.
    static func repeatOffenders(in records: [CommitmentRecord]) -> [PromiseHistory] {
        var seen = Set<String>()
        var keys: [String] = []
        for record in records where !seen.contains(record.key) {
            seen.insert(record.key)
            keys.append(record.key)
        }
        return keys
            .map { history(forKey: $0, in: records) }
            .filter { $0.timesBroken >= 2 }
            .sorted { $0.timesBroken > $1.timesBroken }
    }

    /// Fraction of judged sessions that scored ≥ 1. nil when no history.
    static func idiotRate(scores: [Int]) -> Double? {
        guard !scores.isEmpty else { return nil }
        return Double(scores.filter { $0 > 0 }.count) / Double(scores.count)
    }

    /// Mean idiot score across judged sessions. nil when no history.
    static func averageScore(scores: [Int]) -> Double? {
        guard !scores.isEmpty else { return nil }
        return Double(scores.reduce(0, +)) / Double(scores.count)
    }

    /// Every crime confessed to, tallied, most-committed first.
    static func crimeCounts(crimeLists: [[String]]) -> [(crime: String, count: Int)] {
        var counts: [String: Int] = [:]
        for crimes in crimeLists {
            for crime in crimes {
                counts[crime, default: 0] += 1
            }
        }
        return counts
            .map { (crime: $0.key, count: $0.value) }
            .sorted { $0.count == $1.count ? $0.crime < $1.crime : $0.count > $1.count }
    }
}
