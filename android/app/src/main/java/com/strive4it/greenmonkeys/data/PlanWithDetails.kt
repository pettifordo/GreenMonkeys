package com.strive4it.greenmonkeys.data

import androidx.room.Embedded
import androidx.room.Relation
import com.strive4it.greenmonkeys.logic.PlanStatus
import com.strive4it.greenmonkeys.logic.VideoKind
import java.time.Instant

/** A plan with its commitments, videos and verdict — the unit the UI works with. */
data class PlanWithDetails(
    @Embedded val plan: SessionPlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val commitments: List<CommitmentEntity>,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val videos: List<SessionVideoEntity>,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val verdict: VerdictEntity?,
) {
    fun status(now: Instant = Instant.now()): PlanStatus =
        PlanStatus.derive(plan.sessionStart, plan.plannedEnd, hasVerdict = verdict != null, now = now)

    fun video(kind: VideoKind): SessionVideoEntity? =
        videos.firstOrNull { it.kind == kind.rawValue }

    val commitmentsSummary: String
        get() = commitments.joinToString(" · ") { it.displayText }
}
