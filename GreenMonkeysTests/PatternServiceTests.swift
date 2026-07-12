import Testing
@testable import GreenMonkeys

@Suite("PatternService")
struct PatternServiceTests {

    @Test func historyCountsPromisesAndBreaks() {
        let records: [CommitmentRecord] = [
            CommitmentRecord(kind: .leaveBy, wasBroken: true),
            CommitmentRecord(kind: .leaveBy, wasBroken: true),
            CommitmentRecord(kind: .leaveBy, wasBroken: false),
            CommitmentRecord(kind: .maxDrinks, wasBroken: true),
        ]
        let history = PatternService.history(for: .leaveBy, in: records)
        #expect(history.timesPromised == 3)
        #expect(history.timesBroken == 2)
        #expect(history.timesKept == 1)
    }

    @Test func historyForNeverMadePromiseIsEmpty() {
        let history = PatternService.history(for: .noShots, in: [])
        #expect(history.timesPromised == 0)
        #expect(history.timesBroken == 0)
    }

    @Test func repeatOffendersNeedTwoBreaksAndSortWorstFirst() {
        let records: [CommitmentRecord] = [
            CommitmentRecord(kind: .leaveBy, wasBroken: true),
            CommitmentRecord(kind: .leaveBy, wasBroken: true),
            CommitmentRecord(kind: .maxDrinks, wasBroken: true),
            CommitmentRecord(kind: .maxDrinks, wasBroken: true),
            CommitmentRecord(kind: .maxDrinks, wasBroken: true),
            CommitmentRecord(kind: .waterBetween, wasBroken: true), // only once — not a repeat offender
        ]
        let offenders = PatternService.repeatOffenders(in: records)
        #expect(offenders.map(\.kind) == [.maxDrinks, .leaveBy])
    }

    @Test func idiotRate() {
        #expect(PatternService.idiotRate(verdicts: []) == nil)
        #expect(PatternService.idiotRate(verdicts: [true, true, false, false]) == 0.5)
        #expect(PatternService.idiotRate(verdicts: [false]) == 0)
    }
}
