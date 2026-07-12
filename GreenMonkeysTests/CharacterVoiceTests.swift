import Testing
@testable import GreenMonkeys

@Suite("CharacterVoice")
struct CharacterVoiceTests {

    @Test func insultWordIsInjected() {
        let line = CharacterVoice.monkeyMorningGreeting(brutality: .savage, word: "liability", variant: 0)
        #expect(line.contains("liability"))
        #expect(!line.contains("[WORD]"))
    }

    @Test func noPlaceholderLeaksAtAnyLevelOrVariant() {
        for brutality in Brutality.allCases {
            for variant in 0..<3 {
                let lines = [
                    CharacterVoice.monkeySessionNudge(brutality: brutality, word: "wally", variant: variant),
                    CharacterVoice.monkeyMorningGreeting(brutality: brutality, word: "wally", variant: variant),
                    CharacterVoice.monkeyAfterIdiotVerdict(brutality: brutality, word: "wally", variant: variant),
                    CharacterVoice.monkeyAfterCleanVerdict(brutality: brutality, word: "wally", variant: variant),
                    CharacterVoice.paranoiaMorningLine(brutality: brutality, variant: variant),
                ]
                for line in lines {
                    #expect(!line.isEmpty)
                    #expect(!line.contains("[WORD]"))
                }
            }
        }
    }

    @Test func idiotVerdictLinesAlwaysPointForward() {
        // Hard rule 5: even at savage, the post-verdict line steers to the one change.
        for brutality in Brutality.allCases {
            for variant in 0..<3 {
                let line = CharacterVoice.monkeyAfterIdiotVerdict(brutality: brutality, word: "idiot", variant: variant)
                #expect(line.lowercased().contains("change"))
            }
        }
    }

    @Test func patternCallbackOnlyFiresWithBrokenHistory() {
        #expect(CharacterVoice.patternCallback(kind: .leaveBy, timesPromised: 0, timesBroken: 0, brutality: .standard, word: "idiot") == nil)
        #expect(CharacterVoice.patternCallback(kind: .leaveBy, timesPromised: 3, timesBroken: 0, brutality: .standard, word: "idiot") == nil)

        let callback = CharacterVoice.patternCallback(kind: .leaveBy, timesPromised: 4, timesBroken: 3, brutality: .standard, word: "idiot")
        #expect(callback?.contains("4") == true)
        #expect(callback?.contains("1") == true) // kept once
    }

    @Test func variantIndexNeverCrashesOutOfRange() {
        let line = CharacterVoice.monkeySessionNudge(brutality: .gentle, word: "idiot", variant: 999)
        #expect(!line.isEmpty)
        let negative = CharacterVoice.monkeySessionNudge(brutality: .gentle, word: "idiot", variant: -1)
        #expect(!negative.isEmpty)
    }
}
