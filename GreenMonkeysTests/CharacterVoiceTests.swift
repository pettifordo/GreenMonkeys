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
        #expect(CharacterVoice.patternCallback(label: "Leave by", timesPromised: 0, timesBroken: 0, brutality: .standard, word: "idiot") == nil)
        #expect(CharacterVoice.patternCallback(label: "Leave by", timesPromised: 3, timesBroken: 0, brutality: .standard, word: "idiot") == nil)

        let callback = CharacterVoice.patternCallback(label: "Leave by", timesPromised: 4, timesBroken: 3, brutality: .standard, word: "idiot")
        #expect(callback?.contains("Leave by") == true)
        #expect(callback?.contains("4") == true)
        #expect(callback?.contains("1") == true) // kept once
    }

    @Test func scoreGradingsCoverTheFullScaleAndInjectTheWord() {
        for score in 0...5 {
            let grading = CharacterVoice.scoreGrading(score, word: "liability")
            #expect(!grading.isEmpty)
            #expect(!grading.contains("[WORD]"))
        }
        // 0 is the only score that doesn't name-call.
        #expect(!CharacterVoice.scoreGrading(0, word: "liability").contains("liability"))
        for score in 1...4 {
            #expect(CharacterVoice.scoreGrading(score, word: "liability").contains("liability"))
        }
    }

    @Test func roastReferencesScoreCrimesAndPromises() {
        let roast = CharacterVoice.roast(
            score: 4,
            crimes: ["Threw up", "Drunk texting"],
            brokenPromises: 2,
            brutality: .savage,
            word: "idiot"
        )
        #expect(roast.opener.contains("Four out of five"))
        #expect(roast.charges.count == 2)
        #expect(roast.charges[0].contains("stomach"))
        #expect(roast.charges[1].contains("sent folder"))
        #expect(roast.promisesLine?.contains("2") == true)
        #expect(!roast.opener.contains("[WORD]"))
    }

    @Test func roastCustomCrimeGetsGenericCharge() {
        let roast = CharacterVoice.roast(
            score: 2, crimes: ["Sang Wonderwall twice"], brokenPromises: 0,
            brutality: .standard, word: "idiot"
        )
        #expect(roast.charges.first?.contains("Sang Wonderwall twice") == true)
        #expect(roast.promisesLine == nil)
    }

    @Test func roastCloserEscalatesToTheHonestQuestion() {
        let mild = CharacterVoice.roast(score: 1, crimes: [], brokenPromises: 1, brutality: .standard, word: "idiot")
        let severe = CharacterVoice.roast(score: 5, crimes: [], brokenPromises: 0, brutality: .standard, word: "idiot")
        let manyCrimes = CharacterVoice.roast(score: 1, crimes: ["a", "b", "c"], brokenPromises: 0, brutality: .standard, word: "idiot")
        #expect(!mild.closer.contains("more than it takes"))
        #expect(mild.closer.contains("Could do better"))
        #expect(severe.closer.contains("more than it takes"))
        #expect(manyCrimes.closer.contains("more than it takes"))
    }

    @Test func cleanNightRoastPraisesInstead() {
        let roast = CharacterVoice.roast(score: 0, crimes: [], brokenPromises: 0, brutality: .savage, word: "idiot")
        #expect(roast.charges.isEmpty)
        #expect(roast.closer.contains("bottle that"))
        #expect(!roast.closer.contains("Could do better"))
    }

    @Test func gentleBrutalityUsesThePlainGradingOpener() {
        let roast = CharacterVoice.roast(score: 3, crimes: [], brokenPromises: 0, brutality: .gentle, word: "idiot")
        #expect(roast.opener.contains("3 out of 5"))
    }

    @Test func articleHandlesVowelsAndConsonants() {
        #expect(CharacterVoice.article(for: "idiot") == "an")
        #expect(CharacterVoice.article(for: "embarrassment") == "an")
        #expect(CharacterVoice.article(for: "drunk") == "a")
        #expect(CharacterVoice.article(for: "liability") == "a")
    }

    @Test func variantIndexNeverCrashesOutOfRange() {
        let line = CharacterVoice.monkeySessionNudge(brutality: .gentle, word: "idiot", variant: 999)
        #expect(!line.isEmpty)
        let negative = CharacterVoice.monkeySessionNudge(brutality: .gentle, word: "idiot", variant: -1)
        #expect(!negative.isEmpty)
    }
}
