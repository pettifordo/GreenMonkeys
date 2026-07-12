import Foundation

/// Pure streak arithmetic: days since the last self-confessed idiot night.
enum StreakService {

    /// Whole calendar days between the reference date and now.
    /// - Parameters:
    ///   - lastIdiotDate: `sessionStart` of the most recent plan whose verdict was "yes, I was one".
    ///   - firstUseDate: fallback anchor when no idiot verdict exists yet.
    static func daysSince(lastIdiotDate: Date?, firstUseDate: Date, now: Date = Date(), calendar: Calendar = .current) -> Int {
        let anchor = lastIdiotDate ?? firstUseDate
        let start = calendar.startOfDay(for: anchor)
        let end = calendar.startOfDay(for: now)
        let days = calendar.dateComponents([.day], from: start, to: end).day ?? 0
        return max(0, days)
    }

    /// The anchor date for the streak given all completed plans.
    static func lastIdiotDate(idiotVerdictSessionStarts: [Date]) -> Date? {
        idiotVerdictSessionStarts.max()
    }
}
