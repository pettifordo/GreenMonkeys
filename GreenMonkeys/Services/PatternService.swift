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

/// One night's chartable facts, flattened from a judged plan.
struct NightRecord: Equatable {
    let date: Date
    let score: Int
    let crimes: Int
    let brokenPromises: Int
}

/// Chart aggregation buckets.
enum ChartPeriod: String, CaseIterable, Identifiable {
    case night, week, month, year

    var id: String { rawValue }

    var label: String {
        switch self {
        case .night: return "Night"
        case .week:  return "Week"
        case .month: return "Month"
        case .year:  return "Year"
        }
    }

    var calendarComponent: Calendar.Component? {
        switch self {
        case .night: return nil
        case .week:  return .weekOfYear
        case .month: return .month
        case .year:  return .year
        }
    }

    /// The bar/axis grouping unit for Swift Charts.
    var chartUnit: Calendar.Component {
        calendarComponent ?? .day
    }
}

/// A period bucket for the charts: mean score plus summed misdeeds.
struct PeriodPoint: Equatable, Identifiable {
    var id: Date { periodStart }
    let periodStart: Date
    let averageScore: Double
    let crimes: Int
    let brokenPromises: Int
    let nights: Int
}

/// Simple least-squares trend over time: y = slope × days-since-first + intercept.
struct TrendLine: Equatable {
    let slope: Double
    let intercept: Double
    let firstDate: Date

    /// Projected value at a date, clamped to the 0–5 score scale.
    func value(at date: Date) -> Double {
        let days = date.timeIntervalSince(firstDate) / 86_400
        return min(5, max(0, slope * days + intercept))
    }
}

/// Computes the user's track record: the composite idiot score, which promises
/// they break repeatedly, and the booze-crime charge sheet (SPEC §1.4).
/// Operates on value types only.
enum PatternService {

    // MARK: - Chart aggregation

    /// Groups nights into period buckets (mean score, summed misdeeds),
    /// sorted chronologically. `.night` passes nights through unchanged.
    static func aggregate(_ nights: [NightRecord], by period: ChartPeriod, calendar: Calendar = .current) -> [PeriodPoint] {
        guard let component = period.calendarComponent else {
            return nights
                .sorted { $0.date < $1.date }
                .map {
                    PeriodPoint(periodStart: $0.date, averageScore: Double($0.score),
                                crimes: $0.crimes, brokenPromises: $0.brokenPromises, nights: 1)
                }
        }

        var buckets: [Date: [NightRecord]] = [:]
        for night in nights {
            let start = calendar.dateInterval(of: component, for: night.date)?.start
                ?? calendar.startOfDay(for: night.date)
            buckets[start, default: []].append(night)
        }
        return buckets
            .map { start, group in
                PeriodPoint(
                    periodStart: start,
                    averageScore: Double(group.reduce(0) { $0 + $1.score }) / Double(group.count),
                    crimes: group.reduce(0) { $0 + $1.crimes },
                    brokenPromises: group.reduce(0) { $0 + $1.brokenPromises },
                    nights: group.count
                )
            }
            .sorted { $0.periodStart < $1.periodStart }
    }

    /// Least-squares fit over (date, value). nil with fewer than 3 points or a
    /// zero time spread — predicting from two nights is astrology.
    static func trend(dates: [Date], values: [Double]) -> TrendLine? {
        guard dates.count >= 3, dates.count == values.count,
              let first = dates.min(), let last = dates.max(), last > first else { return nil }

        let xs = dates.map { $0.timeIntervalSince(first) / 86_400 }
        let n = Double(xs.count)
        let sumX = xs.reduce(0, +)
        let sumY = values.reduce(0, +)
        let sumXY = zip(xs, values).reduce(0) { $0 + $1.0 * $1.1 }
        let sumXX = xs.reduce(0) { $0 + $1 * $1 }
        let denominator = n * sumXX - sumX * sumX
        guard denominator != 0 else { return nil }

        let slope = (n * sumXY - sumX * sumY) / denominator
        let intercept = (sumY - slope * sumX) / n
        return TrendLine(slope: slope, intercept: intercept, firstDate: first)
    }

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
