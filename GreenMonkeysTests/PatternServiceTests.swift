import Testing
import Foundation
@testable import GreenMonkeys

@Suite("PatternService")
struct PatternServiceTests {

    private func record(_ key: String, broken: Bool, label: String? = nil) -> CommitmentRecord {
        CommitmentRecord(key: key, label: label ?? key, wasBroken: broken)
    }

    @Test func historyCountsPromisesAndBreaks() {
        let records = [
            record("leaveBy", broken: true, label: "Leave by"),
            record("leaveBy", broken: true, label: "Leave by"),
            record("leaveBy", broken: false, label: "Leave by"),
            record("maxDrinks", broken: true, label: "Max drinks"),
        ]
        let history = PatternService.history(forKey: "leaveBy", in: records)
        #expect(history.timesPromised == 3)
        #expect(history.timesBroken == 2)
        #expect(history.timesKept == 1)
        #expect(history.label == "Leave by")
    }

    @Test func customPromisesTrackByTheirText() {
        let records = [
            record("no karaoke", broken: true, label: "No karaoke"),
            record("no karaoke", broken: true, label: "No karaoke"),
        ]
        let history = PatternService.history(forKey: "no karaoke", in: records)
        #expect(history.timesPromised == 2)
        #expect(history.timesBroken == 2)
        #expect(history.label == "No karaoke")
    }

    @Test func historyForNeverMadePromiseIsEmpty() {
        let history = PatternService.history(forKey: "noShots", in: [])
        #expect(history.timesPromised == 0)
        #expect(history.timesBroken == 0)
        #expect(history.label == "noShots")
    }

    @Test func repeatOffendersNeedTwoBreaksAndSortWorstFirst() {
        let records = [
            record("leaveBy", broken: true),
            record("leaveBy", broken: true),
            record("maxDrinks", broken: true),
            record("maxDrinks", broken: true),
            record("maxDrinks", broken: true),
            record("waterBetween", broken: true), // only once — not a repeat offender
            record("no karaoke", broken: true),
            record("no karaoke", broken: true),
        ]
        let offenders = PatternService.repeatOffenders(in: records)
        #expect(offenders.map(\.key) == ["maxDrinks", "leaveBy", "no karaoke"])
    }

    @Test func idiotRateCountsScoresAboveZero() {
        #expect(PatternService.idiotRate(scores: []) == nil)
        #expect(PatternService.idiotRate(scores: [3, 1, 0, 0]) == 0.5)
        #expect(PatternService.idiotRate(scores: [0]) == 0)
    }

    @Test func averageScore() {
        #expect(PatternService.averageScore(scores: []) == nil)
        #expect(PatternService.averageScore(scores: [0, 5, 1]) == 2.0)
    }

    // MARK: - Aggregation & trend

    private func day(_ string: String) -> Date {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = TimeZone(identifier: "Europe/London")
        return formatter.date(from: string) ?? Date()
    }

    @Test func nightAggregationPassesThroughSorted() {
        let nights = [
            NightRecord(date: day("2026-07-10"), score: 3, crimes: 2, brokenPromises: 1),
            NightRecord(date: day("2026-07-01"), score: 0, crimes: 0, brokenPromises: 0),
        ]
        let points = PatternService.aggregate(nights, by: .night)
        #expect(points.count == 2)
        #expect(points[0].periodStart == day("2026-07-01"))
        #expect(points[1].averageScore == 3.0)
    }

    @Test func monthAggregationAveragesScoresAndSumsMisdeeds() {
        let nights = [
            NightRecord(date: day("2026-06-05"), score: 4, crimes: 3, brokenPromises: 2),
            NightRecord(date: day("2026-06-20"), score: 0, crimes: 0, brokenPromises: 0),
            NightRecord(date: day("2026-07-02"), score: 2, crimes: 1, brokenPromises: 1),
        ]
        let points = PatternService.aggregate(nights, by: .month)
        #expect(points.count == 2)
        #expect(points[0].averageScore == 2.0)   // (4 + 0) / 2
        #expect(points[0].crimes == 3)
        #expect(points[0].brokenPromises == 2)
        #expect(points[0].nights == 2)
        #expect(points[1].averageScore == 2.0)
    }

    @Test func weekAggregationGroupsByCalendarWeek() {
        // Mon 6 July and Wed 8 July 2026 share a week; Mon 13 July doesn't.
        let nights = [
            NightRecord(date: day("2026-07-06"), score: 2, crimes: 0, brokenPromises: 0),
            NightRecord(date: day("2026-07-08"), score: 4, crimes: 1, brokenPromises: 0),
            NightRecord(date: day("2026-07-13"), score: 1, crimes: 0, brokenPromises: 1),
        ]
        let points = PatternService.aggregate(nights, by: .week)
        #expect(points.count == 2)
        #expect(points[0].averageScore == 3.0)
        #expect(points[1].averageScore == 1.0)
    }

    @Test func trendNeedsThreePoints() {
        let dates = [day("2026-07-01"), day("2026-07-08")]
        #expect(PatternService.trend(dates: dates, values: [1, 2]) == nil)
    }

    @Test func trendSlopesMatchTheData() {
        let dates = [day("2026-07-01"), day("2026-07-02"), day("2026-07-03")]
        let rising = PatternService.trend(dates: dates, values: [1, 2, 3])
        #expect(rising != nil)
        #expect((rising?.slope ?? 0) > 0)
        let falling = PatternService.trend(dates: dates, values: [4, 2, 0])
        #expect((falling?.slope ?? 0) < 0)
        // Perfect fit: predicted next day continues the line, clamped to 0...5.
        #expect(abs((rising?.value(at: day("2026-07-04")) ?? 0) - 4.0) < 0.001)
        #expect(falling?.value(at: day("2026-07-05")) == 0)   // would be -4, clamps
    }

    @Test func trendClampsToScoreScale() {
        let dates = [day("2026-07-01"), day("2026-07-02"), day("2026-07-03")]
        let steep = PatternService.trend(dates: dates, values: [3, 4, 5])
        #expect(steep?.value(at: day("2026-07-10")) == 5)
    }

    @Test func crimeCountsTallyAndSortWorstFirst() {
        let counts = PatternService.crimeCounts(crimeLists: [
            ["Threw up", "Blacked out"],
            ["Threw up"],
            ["Threw up", "Insulted someone"],
        ])
        #expect(counts.first?.crime == "Threw up")
        #expect(counts.first?.count == 3)
        #expect(counts.count == 3)
        // Ties break alphabetically for stable display.
        #expect(counts[1].crime == "Blacked out")
    }
}
