package com.strive4it.greenmonkeys.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Status is derived from dates + verdict, never stored (SPEC §4). */
class PlanStatusTests {

    private val start: Instant = Instant.parse("2026-07-10T19:00:00Z")
    private val end: Instant = Instant.parse("2026-07-10T23:00:00Z")

    @Test
    fun beforeStartIsPlanned() {
        val status = PlanStatus.derive(start, end, hasVerdict = false, now = start.minusSeconds(60))
        assertEquals(PlanStatus.PLANNED, status)
    }

    @Test
    fun atStartIsActive() {
        assertEquals(PlanStatus.ACTIVE, PlanStatus.derive(start, end, hasVerdict = false, now = start))
    }

    @Test
    fun withinSixHourGraceIsStillActive() {
        val insideGrace = end.plusSeconds(6 * 3600L)
        assertEquals(PlanStatus.ACTIVE, PlanStatus.derive(start, end, hasVerdict = false, now = insideGrace))
    }

    @Test
    fun pastGraceAwaitsVerdict() {
        val pastGrace = end.plusSeconds(6 * 3600L + 1)
        assertEquals(PlanStatus.AWAITING_VERDICT, PlanStatus.derive(start, end, hasVerdict = false, now = pastGrace))
    }

    @Test
    fun verdictAlwaysMeansCompleted() {
        for (now in listOf(start.minusSeconds(60), start, end.plusSeconds(7 * 3600L))) {
            assertEquals(PlanStatus.COMPLETED, PlanStatus.derive(start, end, hasVerdict = true, now = now))
        }
    }
}
