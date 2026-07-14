package com.strive4it.greenmonkeys.logic

import java.time.Duration
import java.time.Instant

/**
 * Derived lifecycle state of a plan. Never stored — always computed from
 * dates + verdict (SPEC §4). Ported from iOS `Models/SessionPlan.swift`.
 */
enum class PlanStatus {
    PLANNED,
    ACTIVE,
    AWAITING_VERDICT,
    COMPLETED;

    companion object {
        /** Grace period after planned end during which the session still counts as live. */
        val GRACE: Duration = Duration.ofHours(6)

        fun derive(
            sessionStart: Instant,
            plannedEnd: Instant,
            hasVerdict: Boolean,
            now: Instant = Instant.now(),
        ): PlanStatus {
            if (hasVerdict) return COMPLETED
            if (now < sessionStart) return PLANNED
            if (now <= plannedEnd.plus(GRACE)) return ACTIVE
            return AWAITING_VERDICT
        }
    }
}
