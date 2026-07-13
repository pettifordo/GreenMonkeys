import Foundation

/// Pure streak arithmetic: days since the last self-confessed idiot night.
enum StreakService {

    /// Whole clean calendar days earned since the last incident.
    ///
    /// Convention (owner-confirmed): the hangover morning counts as day ZERO —
    /// you haven't earned a day yet just by waking up ashamed. The first full
    /// day after the incident night is day 1.
    /// - Parameters:
    ///   - lastIdiotDate: `sessionStart` of the most recent plan whose verdict scored ≥ 1.
    ///   - firstUseDate: fallback anchor when no idiot verdict exists yet (counts
    ///     plainly — no hangover-day shift, nothing happened).
    static func daysSince(lastIdiotDate: Date?, firstUseDate: Date, now: Date = Date(), calendar: Calendar = .current) -> Int {
        let end = calendar.startOfDay(for: now)
        if let lastIdiotDate {
            let incidentDay = calendar.startOfDay(for: lastIdiotDate)
            let days = (calendar.dateComponents([.day], from: incidentDay, to: end).day ?? 0) - 1
            return max(0, days)
        }
        let start = calendar.startOfDay(for: firstUseDate)
        return max(0, calendar.dateComponents([.day], from: start, to: end).day ?? 0)
    }

    /// The anchor date for the streak given all completed plans.
    static func lastIdiotDate(idiotVerdictSessionStarts: [Date]) -> Date? {
        idiotVerdictSessionStarts.max()
    }
}
