package com.strive4it.greenmonkeys.logic

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure timing math for the notification chain, mirroring iOS
 * `NotificationService.schedule`: nudges are minute-offsets from session
 * start (only future ones fire); the morning-after summons lands the morning
 * after plannedEnd at the configured hour.
 */
object ReminderTimes {

    /** Fire instants for the session nudges — offsets already in the past are dropped. */
    fun nudgeInstants(
        sessionStart: Instant,
        reminderOffsetsMinutes: List<Int>,
        now: Instant = Instant.now(),
    ): List<Instant> =
        reminderOffsetsMinutes
            .map { sessionStart.plusSeconds(it * 60L) }
            .filter { it > now }

    /**
     * The morning-after summons: next morning after plannedEnd at
     * [morningAfterHour], or null if that moment has already passed.
     */
    fun morningAfterInstant(
        plannedEnd: Instant,
        morningAfterHour: Int,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant? {
        val morning = plannedEnd.plusSeconds(24 * 3600L)
            .atZone(zone)
            .toLocalDate()
            .atTime(LocalTime.of(morningAfterHour, 0))
            .atZone(zone)
            .toInstant()
        return if (morning > now) morning else null
    }
}
