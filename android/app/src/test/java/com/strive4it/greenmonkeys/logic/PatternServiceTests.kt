package com.strive4it.greenmonkeys.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Ported from iOS `GreenMonkeysTests/PatternServiceTests.swift`. */
class PatternServiceTests {

    private fun record(key: String, broken: Boolean, label: String? = null) =
        CommitmentRecord(key = key, label = label ?: key, wasBroken = broken)

    @Test
    fun historyCountsPromisesAndBreaks() {
        val records = listOf(
            record("leaveBy", broken = true, label = "Leave by"),
            record("leaveBy", broken = true, label = "Leave by"),
            record("leaveBy", broken = false, label = "Leave by"),
            record("maxDrinks", broken = true, label = "Max drinks"),
        )
        val history = PatternService.history(forKey = "leaveBy", records = records)
        assertEquals(3, history.timesPromised)
        assertEquals(2, history.timesBroken)
        assertEquals(1, history.timesKept)
        assertEquals("Leave by", history.label)
    }

    @Test
    fun customPromisesTrackByTheirText() {
        val records = listOf(
            record("no karaoke", broken = true, label = "No karaoke"),
            record("no karaoke", broken = true, label = "No karaoke"),
        )
        val history = PatternService.history(forKey = "no karaoke", records = records)
        assertEquals(2, history.timesPromised)
        assertEquals(2, history.timesBroken)
        assertEquals("No karaoke", history.label)
    }

    @Test
    fun historyForNeverMadePromiseIsEmpty() {
        val history = PatternService.history(forKey = "noShots", records = emptyList())
        assertEquals(0, history.timesPromised)
        assertEquals(0, history.timesBroken)
        assertEquals("noShots", history.label)
    }

    @Test
    fun repeatOffendersNeedTwoBreaksAndSortWorstFirst() {
        val records = listOf(
            record("leaveBy", broken = true),
            record("leaveBy", broken = true),
            record("maxDrinks", broken = true),
            record("maxDrinks", broken = true),
            record("maxDrinks", broken = true),
            record("waterBetween", broken = true), // only once — not a repeat offender
            record("no karaoke", broken = true),
            record("no karaoke", broken = true),
        )
        val offenders = PatternService.repeatOffenders(records)
        assertEquals(listOf("maxDrinks", "leaveBy", "no karaoke"), offenders.map { it.key })
    }

    @Test
    fun idiotRateCountsScoresAboveZero() {
        assertNull(PatternService.idiotRate(emptyList()))
        assertEquals(0.5, PatternService.idiotRate(listOf(3, 1, 0, 0))!!, 0.0)
        assertEquals(0.0, PatternService.idiotRate(listOf(0))!!, 0.0)
    }

    @Test
    fun averageScore() {
        assertNull(PatternService.averageScore(emptyList()))
        assertEquals(2.0, PatternService.averageScore(listOf(0, 5, 1))!!, 0.0)
    }

    @Test
    fun crimeCountsTallyAndSortWorstFirst() {
        val counts = PatternService.crimeCounts(
            listOf(
                listOf("Threw up", "Blacked out"),
                listOf("Threw up"),
                listOf("Threw up", "Insulted someone"),
            )
        )
        assertEquals("Threw up", counts.first().crime)
        assertEquals(3, counts.first().count)
        assertEquals(3, counts.size)
        // Ties break alphabetically for stable display.
        assertEquals("Blacked out", counts[1].crime)
    }
}
