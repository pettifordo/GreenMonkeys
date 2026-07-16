import Testing
import Foundation
@testable import GreenMonkeys

@Suite("StreakService")
struct StreakServiceTests {
    let calendar = Calendar.current

    private func date(_ daysAgo: Int, hour: Int = 20) -> Date {
        let day = calendar.date(byAdding: .day, value: -daysAgo, to: Date()) ?? Date()
        return calendar.date(bySettingHour: hour, minute: 0, second: 0, of: day) ?? day
    }

    // Owner-confirmed convention: incident night = day -, hangover morning = 0,
    // first full clean day = 1.

    @Test func hangoverMorningIsDayZero() {
        // Idiot night was last night; confessing this morning → streak 0.
        let result = StreakService.daysSince(
            lastIdiotDate: date(1),
            firstUseDate: date(50)
        )
        #expect(result == 0)
    }

    @Test func oneCleanNightAfterIncidentIsDayOne() {
        // Idiot night two nights ago, clean night last night → streak 1 this morning.
        let result = StreakService.daysSince(
            lastIdiotDate: date(2),
            firstUseDate: date(50)
        )
        #expect(result == 1)
    }

    @Test func daysSinceLastIdiotNightShiftsByOne() {
        let result = StreakService.daysSince(
            lastIdiotDate: date(5),
            firstUseDate: date(100)
        )
        #expect(result == 4)
    }

    @Test func incidentEarlierTodayIsStillZero() {
        let result = StreakService.daysSince(
            lastIdiotDate: date(0, hour: 1),
            firstUseDate: date(50)
        )
        #expect(result == 0)
    }

    @Test func neverNegativeForFutureAnchors() {
        let tomorrow = calendar.date(byAdding: .day, value: 1, to: Date()) ?? Date()
        let result = StreakService.daysSince(
            lastIdiotDate: tomorrow,
            firstUseDate: date(50)
        )
        #expect(result == 0)
    }

    @Test func fallsBackToFirstUseWithoutShiftWhenNoIncidents() {
        // No hangover-day shift here: nothing happened, install day counts plainly.
        let result = StreakService.daysSince(
            lastIdiotDate: nil,
            firstUseDate: date(12)
        )
        #expect(result == 12)
    }

    @Test func mostRecentIdiotDateWins() {
        let dates = [date(30), date(3), date(15)]
        #expect(StreakService.lastIdiotDate(idiotVerdictSessionStarts: dates) == date(3))
    }

    @Test func noIdiotDatesReturnsNil() {
        #expect(StreakService.lastIdiotDate(idiotVerdictSessionStarts: []) == nil)
    }

    // MARK: - Longest streak

    @Test func longestWithNoIncidentsIsTheCurrentRun() {
        let result = StreakService.longestStreak(idiotDates: [], firstUseDate: date(12))
        #expect(result == 12)
    }

    @Test func longestPicksTheBiggestGapBetweenIncidents() {
        // Install 30 days ago; incidents 25 and 10 days ago.
        // Segments: pre-first = 5, between = 15 - 1 = 14, current = 10 - 1 = 9.
        let result = StreakService.longestStreak(
            idiotDates: [date(25), date(10)],
            firstUseDate: date(30)
        )
        #expect(result == 14)
    }

    @Test func longestCanBeTheCurrentRun() {
        // Install 40 days ago; incidents 35 and 30 days ago.
        // Segments: pre-first = 5, between = 4, current = 29.
        let result = StreakService.longestStreak(
            idiotDates: [date(35), date(30)],
            firstUseDate: date(40)
        )
        #expect(result == 29)
    }

    @Test func longestCanBeThePreFirstIncidentRun() {
        // Install 50 days ago; first incident only 3 days ago: pre-first = 47 wins.
        let result = StreakService.longestStreak(
            idiotDates: [date(3)],
            firstUseDate: date(50)
        )
        #expect(result == 47)
    }

    @Test func sameNightIncidentsCollapseToOneDay() {
        let result = StreakService.longestStreak(
            idiotDates: [date(10, hour: 20), date(10, hour: 22)],
            firstUseDate: date(15)
        )
        // Segments: pre-first = 5, current = 9.
        #expect(result == 9)
    }
}
