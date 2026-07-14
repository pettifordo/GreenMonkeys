package com.strive4it.greenmonkeys.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.strive4it.greenmonkeys.data.PlanWithDetails
import com.strive4it.greenmonkeys.logic.CharacterVoice
import com.strive4it.greenmonkeys.logic.ReminderTimes
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Schedules session-night nudges and the morning-after summons via
 * AlarmManager, mirroring iOS `NotificationService`: everything is scheduled
 * up-front when the plan is saved (SPEC §4) and cancelled/rescheduled if the
 * plan changes. Content is baked at schedule time, like the iOS requests.
 *
 * Exact alarms use SCHEDULE_EXACT_ALARM (user-scheduled reminders — brief §7)
 * and degrade gracefully to inexact when not permitted.
 */
class NudgeScheduler(private val context: Context) {

    companion object {
        const val SESSION_CHANNEL = "session_nudges"
        const val MORNING_CHANNEL = "morning_after"

        const val EXTRA_PLAN_ID = "planId"
        const val EXTRA_KIND = "kind"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val KIND_SESSION = "session"
        const val KIND_MORNING = "morning"
    }

    fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                SESSION_CHANNEL,
                "Session check-ins",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "The Green Monkeys resurfacing the plan mid-session" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                MORNING_CHANNEL,
                "Morning-after debrief",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Captain Paranoia's summons the morning after" }
        )
    }

    /** Schedules every reminder for a plan plus the morning-after summons. */
    suspend fun schedule(plan: PlanWithDetails, settings: SettingsRepository) {
        cancel(plan)

        val brutality = settings.brutality.first()
        val word = settings.insultWord.first()
        val morningHour = settings.morningAfterHour.first()
        val summary = plan.commitmentsSummary
        val now = Instant.now()

        for (offset in plan.plan.reminderOffsetsMinutes) {
            val fireAt = plan.plan.sessionStart.plusSeconds(offset * 60L)
            if (fireAt <= now) continue
            val body = CharacterVoice.monkeySessionNudge(brutality = brutality, word = word) +
                "\n\nThe plan: $summary"
            setAlarm(
                requestCode = requestCode(plan.plan.id, "session-$offset"),
                fireAt = fireAt,
                kind = KIND_SESSION,
                planId = plan.plan.id,
                title = "🐒 The Green Monkeys",
                body = body,
            )
        }

        val morning = ReminderTimes.morningAfterInstant(plan.plan.plannedEnd, morningHour, now)
        if (morning != null) {
            setAlarm(
                requestCode = requestCode(plan.plan.id, KIND_MORNING),
                fireAt = morning,
                kind = KIND_MORNING,
                planId = plan.plan.id,
                title = "🫣 Captain Paranoia",
                body = "Morning. Time for the debrief on ${plan.plan.occasion}. " +
                    "The Monkeys have the tape ready.",
            )
        }
    }

    fun cancel(plan: PlanWithDetails) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val codes = plan.plan.reminderOffsetsMinutes.map { requestCode(plan.plan.id, "session-$it") } +
            requestCode(plan.plan.id, KIND_MORNING)
        for (code in codes) {
            alarmManager.cancel(pendingIntent(code, Intent(context, NudgeReceiver::class.java)))
        }
    }

    // MARK: - Private

    private fun setAlarm(
        requestCode: Int,
        fireAt: Instant,
        kind: String,
        planId: String,
        title: String,
        body: String,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, NudgeReceiver::class.java).apply {
            putExtra(EXTRA_PLAN_ID, planId)
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }
        val pending = pendingIntent(requestCode, intent)
        val canExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt.toEpochMilli(), pending)
        } else {
            // Exact-alarm permission revoked: fire approximately rather than not at all.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt.toEpochMilli(), pending)
        }
    }

    private fun pendingIntent(requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun requestCode(planId: String, tag: String): Int = "$planId-$tag".hashCode()
}
