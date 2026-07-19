package com.strive4it.greenmonkeys.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Ported from iOS `GreenMonkeysTests/PatternServiceTests.swift`. */
class PatternServiceTests {

    private val zone: ZoneId = ZoneId.of("Europe/London")

    private fun record(key: String, broken: Boolean, label: String? = null) =
        CommitmentRecord(key = key, label = label ?: key, wasBroken = broken)

    private fun day(iso: String): Instant =
        LocalDate.parse(iso).atStartOfDay(zone).toInstant()

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

    // MARK: - Chart aggregation (ported from iOS 1.1 tests)

    @Test
    fun nightAggregationPassesThroughSorted() {
        val nights = listOf(
            NightRecord(date = day("2026-07-04"), score = 3, crimes = 1, brokenPromises = 0),
            NightRecord(date = day("2026-07-01"), score = 0, crimes = 0, brokenPromises = 0),
        )
        val points = PatternService.aggregate(nights, ChartPeriod.NIGHT, zone)
        assertEquals(2, points.size)
        assertEquals(day("2026-07-01"), points[0].periodStart)
        assertEquals(3.0, points[1].averageScore, 0.0)
    }

    @Test
    fun monthAggregationAveragesScoresAndSumsMisdeeds() {
        val nights = listOf(
            NightRecord(date = day("2026-06-05"), score = 4, crimes = 3, brokenPromises = 2),
            NightRecord(date = day("2026-06-20"), score = 0, crimes = 0, brokenPromises = 0),
            NightRecord(date = day("2026-07-02"), score = 2, crimes = 1, brokenPromises = 1),
        )
        val points = PatternService.aggregate(nights, ChartPeriod.MONTH, zone)
        assertEquals(2, points.size)
        assertEquals(2.0, points[0].averageScore, 0.0) // (4 + 0) / 2
        assertEquals(3, points[0].crimes)
        assertEquals(2, points[0].brokenPromises)
        assertEquals(2, points[0].nights)
        assertEquals(2.0, points[1].averageScore, 0.0)
    }

    @Test
    fun weekAggregationGroupsByCalendarWeek() {
        // Mon 6 July and Wed 8 July 2026 share a week; Mon 13 July doesn't.
        val nights = listOf(
            NightRecord(date = day("2026-07-06"), score = 2, crimes = 0, brokenPromises = 0),
            NightRecord(date = day("2026-07-08"), score = 4, crimes = 1, brokenPromises = 0),
            NightRecord(date = day("2026-07-13"), score = 1, crimes = 0, brokenPromises = 1),
        )
        val points = PatternService.aggregate(nights, ChartPeriod.WEEK, zone)
        assertEquals(2, points.size)
        assertEquals(3.0, points[0].averageScore, 0.0)
        assertEquals(1.0, points[1].averageScore, 0.0)
    }

    @Test
    fun trendNeedsThreePoints() {
        val dates = listOf(day("2026-07-01"), day("2026-07-08"))
        assertNull(PatternService.trend(dates, listOf(1.0, 2.0)))
    }

    @Test
    fun trendSlopesMatchTheData() {
        val dates = listOf(day("2026-07-01"), day("2026-07-02"), day("2026-07-03"))
        val rising = PatternService.trend(dates, listOf(1.0, 2.0, 3.0))
        assertNotNull(rising)
        assertTrue((rising?.slope ?: 0.0) > 0)
        val falling = PatternService.trend(dates, listOf(4.0, 2.0, 0.0))
        assertTrue((falling?.slope ?: 0.0) < 0)
        // Perfect fit: predicted next day continues the line, clamped to 0..5.
        assertEquals(4.0, rising?.valueAt(day("2026-07-04")) ?: 0.0, 0.001)
        assertEquals(0.0, falling?.valueAt(day("2026-07-05")) ?: -1.0, 0.0) // would be -4, clamps
    }

    @Test
    fun trendClampsToScoreScale() {
        val dates = listOf(day("2026-07-01"), day("2026-07-02"), day("2026-07-03"))
        val steep = PatternService.trend(dates, listOf(3.0, 4.0, 5.0))
        assertEquals(5.0, steep?.valueAt(day("2026-07-10")) ?: 0.0, 0.0)
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
