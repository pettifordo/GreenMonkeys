package com.strive4it.greenmonkeys.debug

import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.CommitmentEntity
import com.strive4it.greenmonkeys.data.SessionPlanEntity
import com.strive4it.greenmonkeys.data.VerdictEntity
import com.strive4it.greenmonkeys.logic.CommitmentKind
import com.strive4it.greenmonkeys.logic.PlanStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Screenshot/demo rig — the same dataset as iOS `ScreenshotRig.swift`,
 * verbatim (brief §4): 5 judged nights (scores 4/2/0/3/0), one awaiting its
 * verdict, one upcoming. Debug builds only; trigger with:
 *
 *   adb shell am start -n com.strive4it.greenmonkeys/.MainActivity \
 *       --ez demoData true --es screen pattern
 *
 * Screens: home, editor, session, morning, roast, pattern, settings.
 */
object DemoSeeder {

    private data class Promise(val kind: CommitmentKind, val detail: String, val broken: Boolean)

    suspend fun seedIfEmpty(app: GreenMonkeysApp) {
        if (app.planRepository.getAllPlans().isNotEmpty()) return

        app.settings.rememberPromise("No karaoke")
        app.settings.rememberCrime("Sang Wonderwall. Twice.")

        suspend fun makePlan(
            occasion: String,
            daysAgo: Long,
            promises: List<Promise>,
            score: Int?,
            crimes: List<String>,
            oneChange: String,
        ) {
            val zone = ZoneId.systemDefault()
            fun night(hour: Int): Instant =
                LocalDate.now(zone).minusDays(daysAgo).atTime(LocalTime.of(hour, 0))
                    .atZone(zone).toInstant()

            val plan = SessionPlanEntity(
                occasion = occasion,
                sessionStart = night(19),
                plannedEnd = night(23),
                reminderOffsetsMinutes = listOf(60, 120, 180),
            )
            val commitments = promises.map {
                CommitmentEntity(
                    planId = plan.id,
                    kindRaw = it.kind.rawValue,
                    detail = it.detail,
                    wasBroken = if (score != null) it.broken else null,
                )
            }
            app.planRepository.savePlan(plan, commitments)
            if (score != null) {
                app.planRepository.recordVerdict(
                    VerdictEntity(
                        planId = plan.id,
                        score = score,
                        wasIdiot = score > 0,
                        crimes = crimes,
                        oneChange = oneChange,
                    )
                )
            }
        }

        makePlan(
            "Dave's 40th", daysAgo = 26,
            promises = listOf(
                Promise(CommitmentKind.MAX_DRINKS, "4", broken = true),
                Promise(CommitmentKind.LEAVE_BY, "23:00", broken = true),
                Promise(CommitmentKind.NO_DRIVING, "", broken = false),
            ),
            score = 4, crimes = listOf("Blacked out", "Threw up", "Lost phone / wallet / keys"),
            oneChange = "No tequila. Ever.",
        )
        makePlan(
            "Friday pub", daysAgo = 19,
            promises = listOf(
                Promise(CommitmentKind.MAX_DRINKS, "5", broken = true),
                Promise(CommitmentKind.WATER_BETWEEN, "", broken = false),
            ),
            score = 2, crimes = listOf("Drunk texting"),
            oneChange = "Leave the phone in my coat.",
        )
        makePlan(
            "Work leaving drinks", daysAgo = 14,
            promises = listOf(
                Promise(CommitmentKind.MAX_DRINKS, "3", broken = false),
                Promise(CommitmentKind.EAT_FIRST, "", broken = false),
            ),
            score = 0, crimes = emptyList(), oneChange = "",
        )
        makePlan(
            "Quiet one with Sam", daysAgo = 12,
            promises = listOf(
                Promise(CommitmentKind.MAX_DRINKS, "4", broken = true),
                Promise(CommitmentKind.CUSTOM, "No karaoke", broken = true),
                Promise(CommitmentKind.LEAVE_BY, "22:30", broken = true),
            ),
            score = 3, crimes = listOf("Insulted someone", "Sang Wonderwall. Twice."),
            oneChange = "\"Quiet one\" means quiet one.",
        )
        makePlan(
            "Sunday roast", daysAgo = 5,
            promises = listOf(
                Promise(CommitmentKind.MAX_DRINKS, "2", broken = false),
                Promise(CommitmentKind.NO_DRIVING, "", broken = false),
            ),
            score = 0, crimes = emptyList(), oneChange = "",
        )
        // Last night — awaiting its verdict (the morning-after shot).
        makePlan(
            "Pub quiz", daysAgo = 1,
            promises = listOf(
                Promise(CommitmentKind.MAX_DRINKS, "4", broken = false),
                Promise(CommitmentKind.LEAVE_BY, "23:00", broken = false),
            ),
            score = null, crimes = emptyList(), oneChange = "",
        )
        // Tomorrow — upcoming (home + session shots).
        makePlan(
            "Marco's birthday", daysAgo = -1,
            promises = listOf(
                Promise(CommitmentKind.MAX_DRINKS, "4", broken = false),
                Promise(CommitmentKind.WATER_BETWEEN, "", broken = false),
                Promise(CommitmentKind.LEAVE_BY, "23:00", broken = false),
                Promise(CommitmentKind.NO_DRIVING, "", broken = false),
                Promise(CommitmentKind.CUSTOM, "No karaoke", broken = false),
            ),
            score = null, crimes = emptyList(), oneChange = "",
        )
    }

    /** Mirrors iOS `routeForScreenshot`: which destination a rig launch opens. */
    suspend fun routeFor(screen: String, app: GreenMonkeysApp): Pair<String?, String>? {
        val all = app.planRepository.getAllPlans().sortedBy { it.plan.sessionStart }
        return when (screen) {
            "editor" -> null to "editor"
            "session" -> all.lastOrNull { it.verdict == null && it.commitments.isNotEmpty() }
                ?.let { it.plan.id to "session" }
            "morning" -> all.firstOrNull { it.status() == PlanStatus.AWAITING_VERDICT }
                ?.let { it.plan.id to "morning" }
            "roast" -> all.firstOrNull { (it.verdict?.crimes?.size ?: 0) >= 2 }
                ?.let { it.plan.id to "verdict" }
            "pattern" -> null to "pattern"
            "settings" -> null to "settings"
            else -> null
        }
    }
}
