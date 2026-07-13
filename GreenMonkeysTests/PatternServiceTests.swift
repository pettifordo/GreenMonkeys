import Testing
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
