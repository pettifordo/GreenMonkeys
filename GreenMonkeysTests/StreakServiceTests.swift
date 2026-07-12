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

    @Test func daysSinceLastIdiotNight() {
        let result = StreakService.daysSince(
            lastIdiotDate: date(5),
            firstUseDate: date(100)
        )
        #expect(result == 5)
    }

    @Test func fallsBackToFirstUseWhenNoIdiotVerdicts() {
        let result = StreakService.daysSince(
            lastIdiotDate: nil,
            firstUseDate: date(12)
        )
        #expect(result == 12)
    }

    @Test func idiotNightLastNightMeansZero() {
        // Session started yesterday evening; verdict this morning → 1 calendar day since the session date.
        let result = StreakService.daysSince(
            lastIdiotDate: date(1),
            firstUseDate: date(50)
        )
        #expect(result == 1)
    }

    @Test func sameDayIsZero() {
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

    @Test func mostRecentIdiotDateWins() {
        let dates = [date(30), date(3), date(15)]
        #expect(StreakService.lastIdiotDate(idiotVerdictSessionStarts: dates) == date(3))
    }

    @Test func noIdiotDatesReturnsNil() {
        #expect(StreakService.lastIdiotDate(idiotVerdictSessionStarts: []) == nil)
    }
}
