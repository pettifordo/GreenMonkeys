package com.strive4it.greenmonkeys.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReminderTimesTests {

    private val zone: ZoneId = ZoneId.of("Europe/London")
    private val start: Instant = Instant.parse("2026-07-10T18:00:00Z") // 19:00 London
    private val end: Instant = Instant.parse("2026-07-10T22:00:00Z")   // 23:00 London

    @Test
    fun nudgesFireAtOffsetsFromSessionStart() {
        val fireTimes = ReminderTimes.nudgeInstants(start, listOf(60, 120, 180), now = start.minusSeconds(3600))
        assertEquals(
            listOf(start.plusSeconds(3600), start.plusSeconds(7200), start.plusSeconds(10800)),
            fireTimes,
        )
    }

    @Test
    fun pastOffsetsAreDroppedNotRescheduled() {
        // Saving/editing a plan mid-session: only the remaining nudges fire.
        val fireTimes = ReminderTimes.nudgeInstants(start, listOf(60, 120, 180), now = start.plusSeconds(5400))
        assertEquals(listOf(start.plusSeconds(7200), start.plusSeconds(10800)), fireTimes)
    }

    @Test
    fun morningAfterLandsNextMorningAtConfiguredHour() {
        val morning = ReminderTimes.morningAfterInstant(end, morningAfterHour = 9, now = end, zone = zone)
        // plannedEnd 23:00 Fri London + 24h → Sat, at 09:00 London (08:00 UTC in BST).
        assertEquals(Instant.parse("2026-07-11T08:00:00Z"), morning)
    }

    @Test
    fun sessionEndingAfterMidnightStillGetsSameMorningPlusDay() {
        // plannedEnd 01:00 Sat London → summons Sunday 09:00 (mirrors iOS +24h-then-floor).
        val lateEnd = Instant.parse("2026-07-11T00:00:00Z")
        val morning = ReminderTimes.morningAfterInstant(lateEnd, morningAfterHour = 9, now = lateEnd, zone = zone)
        assertEquals(Instant.parse("2026-07-12T08:00:00Z"), morning)
    }

    @Test
    fun morningAlreadyPassedReturnsNull() {
        val wayLater = end.plusSeconds(72 * 3600L)
        assertNull(ReminderTimes.morningAfterInstant(end, morningAfterHour = 9, now = wayLater, zone = zone))
    }
}
