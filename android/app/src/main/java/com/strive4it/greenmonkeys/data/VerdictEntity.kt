package com.strive4it.greenmonkeys.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Mirrors iOS `Verdict`. Self-reported 0–5 — the app never sets this on its
 * own (SPEC §2, hard rule 4). Never deleted/overwritten except by explicit
 * user action (hard rule 3).
 */
@Entity(
    tableName = "verdicts",
    foreignKeys = [
        ForeignKey(
            entity = SessionPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("planId", unique = true)],
)
data class VerdictEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val planId: String,
    /** 0 = behaved like a normal person, 5 = can't show my face again. */
    val score: Int = 0,
    /** Legacy flag kept in sync with `score > 0` (iOS pre-scale verdicts stored only this). */
    val wasIdiot: Boolean = false,
    /** Booze crimes confessed to — labels from the crime catalog plus customs. */
    val crimes: List<String> = emptyList(),
    /** The forward-looking commitment. Encouraged, not required. */
    val oneChange: String = "",
    val note: String = "",
    val recordedAt: Instant = Instant.now(),
) {
    /** Bridges pre-scale verdicts: a legacy `wasIdiot` with no score counts as 1. */
    val effectiveScore: Int get() = maxOf(score, if (wasIdiot) 1 else 0)
}
