package com.strive4it.greenmonkeys.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Ported from iOS `GreenMonkeysTests/StreakServiceTests.swift`. */
class StreakServiceTests {
    private val zone: ZoneId = ZoneId.systemDefault()

    private fun date(daysAgo: Long, hour: Int = 20): Instant =
        ZonedDateTime.now(zone)
            .minusDays(daysAgo)
            .withHour(hour).withMinute(0).withSecond(0).withNano(0)
            .toInstant()

    // Owner-confirmed convention: incident night = day -, hangover morning = 0,
    // first full clean day = 1.

    @Test
    fun hangoverMorningIsDayZero() {
        // Idiot night was last night; confessing this morning → streak 0.
        val result = StreakService.daysSince(
            lastIdiotDate = date(1),
            firstUseDate = date(50),
            zone = zone,
        )
        assertEquals(0, result)
    }

    @Test
    fun oneCleanNightAfterIncidentIsDayOne() {
        // Idiot night two nights ago, clean night last night → streak 1 this morning.
        // (The owner's paid-for lesson — brief §6.1.)
        val result = StreakService.daysSince(
            lastIdiotDate = date(2),
            firstUseDate = date(50),
            zone = zone,
        )
        assertEquals(1, result)
    }

    @Test
    fun daysSinceLastIdiotNightShiftsByOne() {
        val result = StreakService.daysSince(
            lastIdiotDate = date(5),
            firstUseDate = date(100),
            zone = zone,
        )
        assertEquals(4, result)
    }

    @Test
    fun incidentEarlierTodayIsStillZero() {
        val result = StreakService.daysSince(
            lastIdiotDate = date(0, hour = 1),
            firstUseDate = date(50),
            zone = zone,
        )
        assertEquals(0, result)
    }

    @Test
    fun neverNegativeForFutureAnchors() {
        val tomorrow = ZonedDateTime.now(zone).plusDays(1).toInstant()
        val result = StreakService.daysSince(
            lastIdiotDate = tomorrow,
            firstUseDate = date(50),
            zone = zone,
        )
        assertEquals(0, result)
    }

    @Test
    fun fallsBackToFirstUseWithoutShiftWhenNoIncidents() {
        // No hangover-day shift here: nothing happened, install day counts plainly.
        val result = StreakService.daysSince(
            lastIdiotDate = null,
            firstUseDate = date(12),
            zone = zone,
        )
        assertEquals(12, result)
    }

    @Test
    fun mostRecentIdiotDateWins() {
        val dates = listOf(date(30), date(3), date(15))
        assertEquals(date(3), StreakService.lastIdiotDate(dates))
    }

    @Test
    fun noIdiotDatesReturnsNull() {
        assertNull(StreakService.lastIdiotDate(emptyList()))
    }

    // MARK: - Longest streak (ported from iOS 1.1 tests)

    @Test
    fun longestWithNoIncidentsIsTheCurrentRun() {
        val result = StreakService.longestStreak(idiotDates = emptyList(), firstUseDate = date(12), zone = zone)
        assertEquals(12, result)
    }

    @Test
    fun longestPicksTheBiggestGapBetweenIncidents() {
        // Install 30 days ago; incidents 25 and 10 days ago.
        // Segments: pre-first = 5, between = 15 - 1 = 14, current = 10 - 1 = 9.
        val result = StreakService.longestStreak(
            idiotDates = listOf(date(25), date(10)),
            firstUseDate = date(30),
            zone = zone,
        )
        assertEquals(14, result)
    }

    @Test
    fun longestCanBeTheCurrentRun() {
        // Install 40 days ago; incidents 35 and 30 days ago.
        // Segments: pre-first = 5, between = 4, current = 29.
        val result = StreakService.longestStreak(
            idiotDates = listOf(date(35), date(30)),
            firstUseDate = date(40),
            zone = zone,
        )
        assertEquals(29, result)
    }

    @Test
    fun longestCanBeThePreFirstIncidentRun() {
        // Install 50 days ago; first incident only 3 days ago: pre-first = 47 wins.
        val result = StreakService.longestStreak(
            idiotDates = listOf(date(3)),
            firstUseDate = date(50),
            zone = zone,
        )
        assertEquals(47, result)
    }

    @Test
    fun sameNightIncidentsCollapseToOneDay() {
        val result = StreakService.longestStreak(
            idiotDates = listOf(date(10, hour = 20), date(10, hour = 22)),
            firstUseDate = date(15),
            zone = zone,
        )
        // Segments: pre-first = 5, current = 9.
        assertEquals(9, result)
    }
}
