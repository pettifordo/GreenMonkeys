import Foundation

/// A judged commitment, reduced to a value type so pattern logic stays pure and testable.
struct CommitmentRecord: Equatable {
    let kind: CommitmentKind
    let wasBroken: Bool
}

struct PromiseHistory: Equatable {
    let kind: CommitmentKind
    let timesPromised: Int
    let timesBroken: Int

    var timesKept: Int { timesPromised - timesBroken }
}

/// Computes the user's track record: how often they were an idiot and which
/// promises they break repeatedly (SPEC §1.4). Operates on value types only.
enum PatternService {

    /// History for one promise kind across all judged sessions.
    static func history(for kind: CommitmentKind, in records: [CommitmentRecord]) -> PromiseHistory {
        let matching = records.filter { $0.kind == kind }
        return PromiseHistory(
            kind: kind,
            timesPromised: matching.count,
            timesBroken: matching.filter(\.wasBroken).count
        )
    }

    /// Promise kinds broken more than once, worst first — the repeat offenders.
    static func repeatOffenders(in records: [CommitmentRecord]) -> [PromiseHistory] {
        CommitmentKind.allCases
            .map { history(for: $0, in: records) }
            .filter { $0.timesBroken >= 2 }
            .sorted { $0.timesBroken > $1.timesBroken }
    }

    /// Fraction of judged sessions that ended in an idiot verdict. nil when no history.
    static func idiotRate(verdicts: [Bool]) -> Double? {
        guard !verdicts.isEmpty else { return nil }
        return Double(verdicts.filter { $0 }.count) / Double(verdicts.count)
    }
}
