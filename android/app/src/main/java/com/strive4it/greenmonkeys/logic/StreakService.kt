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

    /**
     * The best clean run ever achieved, using the same day conventions as
     * [daysSince]: install → first incident counts plainly; runs after an
     * incident start from the day AFTER the hangover morning; the current
     * run counts too (the record might be happening right now).
     */
    fun longestStreak(
        idiotDates: List<Instant>,
        firstUseDate: Instant,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val incidentDays: List<LocalDate> =
            idiotDates.map { it.atZone(zone).toLocalDate() }.distinct().sorted()
        val firstIncident = incidentDays.firstOrNull()
            ?: return daysSince(lastIdiotDate = null, firstUseDate = firstUseDate, now = now, zone = zone)

        val firstUseDay = firstUseDate.atZone(zone).toLocalDate()
        var best = maxOf(0, ChronoUnit.DAYS.between(firstUseDay, firstIncident).toInt())

        for ((previous, next) in incidentDays.zipWithNext()) {
            val gap = ChronoUnit.DAYS.between(previous, next).toInt() - 1
            best = maxOf(best, gap)
        }

        val lastIncidentInstant = incidentDays.last().atStartOfDay(zone).toInstant()
        val current = daysSince(lastIdiotDate = lastIncidentInstant, firstUseDate = firstUseDate, now = now, zone = zone)
        return maxOf(best, current)
    }
}
