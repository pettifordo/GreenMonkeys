package com.strive4it.greenmonkeys.logic

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure streak arithmetic: days since the last self-confessed idiot night.
 * Ported line-for-line from iOS `Shared/StreakService.swift`.
 */
object StreakService {

    /**
     * Whole clean calendar days earned since the last incident.
     *
     * Convention (owner-confirmed): the hangover morning counts as day ZERO —
     * you haven't earned a day yet just by waking up ashamed. The first full
     * day after the incident night is day 1.
     *
     * @param lastIdiotDate `sessionStart` of the most recent plan whose verdict scored >= 1.
     * @param firstUseDate fallback anchor when no idiot verdict exists yet (counts
     *   plainly — no hangover-day shift, nothing happened).
     */
    fun daysSince(
        lastIdiotDate: Instant?,
        firstUseDate: Instant,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val end: LocalDate = now.atZone(zone).toLocalDate()
        if (lastIdiotDate != null) {
            val incidentDay = lastIdiotDate.atZone(zone).toLocalDate()
            val days = ChronoUnit.DAYS.between(incidentDay, end).toInt() - 1
            return maxOf(0, days)
        }
        val start = firstUseDate.atZone(zone).toLocalDate()
        return maxOf(0, ChronoUnit.DAYS.between(start, end).toInt())
    }

    /** The anchor date for the streak given all completed plans. */
    fun lastIdiotDate(idiotVerdictSessionStarts: List<Instant>): Instant? =
        idiotVerdictSessionStarts.maxOrNull()
}
