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

    /// The best clean run ever achieved, using the same day conventions as
    /// `daysSince`: install → first incident counts plainly; runs after an
    /// incident start from the day AFTER the hangover morning; the current
    /// run counts too (the record might be happening right now).
    static func longestStreak(idiotDates: [Date], firstUseDate: Date, now: Date = Date(), calendar: Calendar = .current) -> Int {
        let incidentDays = Set(idiotDates.map { calendar.startOfDay(for: $0) }).sorted()
        guard let firstIncident = incidentDays.first else {
            return daysSince(lastIdiotDate: nil, firstUseDate: firstUseDate, now: now, calendar: calendar)
        }

        let firstUseDay = calendar.startOfDay(for: firstUseDate)
        var best = max(0, calendar.dateComponents([.day], from: firstUseDay, to: firstIncident).day ?? 0)

        for (previous, next) in zip(incidentDays, incidentDays.dropFirst()) {
            let gap = (calendar.dateComponents([.day], from: previous, to: next).day ?? 0) - 1
            best = max(best, gap)
        }

        let current = daysSince(lastIdiotDate: incidentDays.last, firstUseDate: firstUseDate, now: now, calendar: calendar)
        return max(best, current)
    }
}
