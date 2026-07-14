package com.strive4it.greenmonkeys.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Mirrors iOS `SessionPlan`. Lifecycle status is DERIVED from dates + verdict
 * (see PlanWithDetails.status) and never stored — brief §4.
 */
@Entity(tableName = "plans")
data class SessionPlanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** e.g. "Dave's birthday", "Friday pub" */
    val occasion: String = "",
    val sessionStart: Instant,
    val plannedEnd: Instant,
    /** Reminder times as minute-offsets from sessionStart, chosen at plan time. */
    val reminderOffsetsMinutes: List<Int> = emptyList(),
    val createdAt: Instant = Instant.now(),
)
