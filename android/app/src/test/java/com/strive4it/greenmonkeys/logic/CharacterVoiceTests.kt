package com.strive4it.greenmonkeys.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ported from iOS `GreenMonkeysTests/CharacterVoiceTests.swift`. */
class CharacterVoiceTests {

    @Test
    fun insultWordIsInjected() {
        val line = CharacterVoice.monkeyMorningGreeting(Brutality.SAVAGE, word = "liability", variant = 0)
        assertTrue(line.contains("liability"))
        assertFalse(line.contains("[WORD]"))
    }

    @Test
    fun noPlaceholderLeaksAtAnyLevelOrVariant() {
        for (brutality in Brutality.entries) {
            for (variant in 0 until 3) {
                val lines = listOf(
                    CharacterVoice.monkeySessionNudge(brutality, word = "wally", variant = variant),
                    CharacterVoice.monkeyMorningGreeting(brutality, word = "wally", variant = variant),
                    CharacterVoice.monkeyAfterIdiotVerdict(brutality, word = "wally", variant = variant),
                    CharacterVoice.monkeyAfterCleanVerdict(brutality, word = "wally", variant = variant),
                    CharacterVoice.paranoiaMorningLine(brutality, variant = variant),
                )
                for (line in lines) {
                    assertTrue(line.isNotEmpty())
                    assertFalse(line.contains("[WORD]"))
                }
            }
        }
    }

    @Test
    fun idiotVerdictLinesAlwaysPointForward() {
        // Hard rule 5: even at savage, the post-verdict line steers to the one change.
        for (brutality in Brutality.entries) {
            for (variant in 0 until 3) {
                val line = CharacterVoice.monkeyAfterIdiotVerdict(brutality, word = "idiot", variant = variant)
                assertTrue(line.lowercase().contains("change"))
            }
        }
    }

    @Test
    fun patternCallbackOnlyFiresWithBrokenHistory() {
        assertNull(CharacterVoice.patternCallback("Leave by", timesPromised = 0, timesBroken = 0, brutality = Brutality.STANDARD, word = "idiot"))
        assertNull(CharacterVoice.patternCallback("Leave by", timesPromised = 3, timesBroken = 0, brutality = Brutality.STANDARD, word = "idiot"))

        val callback = CharacterVoice.patternCallback("Leave by", timesPromised = 4, timesBroken = 3, brutality = Brutality.STANDARD, word = "idiot")
        assertTrue(callback?.contains("Leave by") == true)
        assertTrue(callback?.contains("4") == true)
        assertTrue(callback?.contains("1") == true) // kept once
    }

    @Test
    fun scoreGradingsCoverTheFullScaleAndInjectTheWord() {
        for (score in 0..5) {
            val grading = CharacterVoice.scoreGrading(score, word = "liability")
            assertTrue(grading.isNotEmpty())
            assertFalse(grading.contains("[WORD]"))
        }
        // 0 is the only score that doesn't name-call.
        assertFalse(CharacterVoice.scoreGrading(0, word = "liability").contains("liability"))
        for (score in 1..4) {
            assertTrue(CharacterVoice.scoreGrading(score, word = "liability").contains("liability"))
        }
    }

    @Test
    fun roastReferencesScoreCrimesAndPromises() {
        val roast = CharacterVoice.roast(
            score = 4,
            crimes = listOf("Threw up", "Drunk texting"),
            brokenPromises = 2,
            brutality = Brutality.SAVAGE,
            word = "idiot",
        )
        assertTrue(roast.opener.contains("Four out of five"))
        assertEquals(2, roast.charges.size)
        assertTrue(roast.charges[0].contains("stomach"))
        assertTrue(roast.charges[1].contains("sent folder"))
        assertTrue(roast.promisesLine?.contains("2") == true)
        assertFalse(roast.opener.contains("[WORD]"))
    }

    @Test
    fun roastCustomCrimeGetsGenericCharge() {
        val roast = CharacterVoice.roast(
            score = 2, crimes = listOf("Sang Wonderwall twice"), brokenPromises = 0,
            brutality = Brutality.STANDARD, word = "idiot",
        )
        assertTrue(roast.charges.first().contains("Sang Wonderwall twice"))
        assertNull(roast.promisesLine)
    }

    @Test
    fun roastCloserEscalatesToTheHonestQuestion() {
        val mild = CharacterVoice.roast(score = 1, crimes = emptyList(), brokenPromises = 1, brutality = Brutality.STANDARD, word = "idiot")
        val severe = CharacterVoice.roast(score = 5, crimes = emptyList(), brokenPromises = 0, brutality = Brutality.STANDARD, word = "idiot")
        val manyCrimes = CharacterVoice.roast(score = 1, crimes = listOf("a", "b", "c"), brokenPromises = 0, brutality = Brutality.STANDARD, word = "idiot")
        assertFalse(mild.closer.contains("more than it takes"))
        assertTrue(mild.closer.contains("Could do better"))
        assertTrue(severe.closer.contains("more than it takes"))
        assertTrue(manyCrimes.closer.contains("more than it takes"))
    }

    @Test
    fun cleanNightRoastPraisesInstead() {
        val roast = CharacterVoice.roast(score = 0, crimes = emptyList(), brokenPromises = 0, brutality = Brutality.SAVAGE, word = "idiot")
        assertTrue(roast.charges.isEmpty())
        assertTrue(roast.closer.contains("bottle that"))
        assertFalse(roast.closer.contains("Could do better"))
    }

    @Test
    fun gentleBrutalityUsesThePlainGradingOpener() {
        val roast = CharacterVoice.roast(score = 3, crimes = emptyList(), brokenPromises = 0, brutality = Brutality.GENTLE, word = "idiot")
        assertTrue(roast.opener.contains("3 out of 5"))
    }

    @Test
    fun articleHandlesVowelsAndConsonants() {
        assertEquals("an", CharacterVoice.article("idiot"))
        assertEquals("an", CharacterVoice.article("embarrassment"))
        assertEquals("a", CharacterVoice.article("drunk"))
        assertEquals("a", CharacterVoice.article("liability"))
    }

    @Test
    fun variantIndexNeverCrashesOutOfRange() {
        val line = CharacterVoice.monkeySessionNudge(Brutality.GENTLE, word = "idiot", variant = 999)
        assertTrue(line.isNotEmpty())
        val negative = CharacterVoice.monkeySessionNudge(Brutality.GENTLE, word = "idiot", variant = -1)
        assertTrue(negative.isNotEmpty())
    }
}
