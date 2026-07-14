package com.strive4it.greenmonkeys.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.strive4it.greenmonkeys.MainActivity
import com.strive4it.greenmonkeys.R

/** Fires when an alarm lands: posts the pre-baked nudge or summons. */
class NudgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val planId = intent.getStringExtra(NudgeScheduler.EXTRA_PLAN_ID) ?: return
        val kind = intent.getStringExtra(NudgeScheduler.EXTRA_KIND) ?: return
        val title = intent.getStringExtra(NudgeScheduler.EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(NudgeScheduler.EXTRA_BODY) ?: return

        val channel = if (kind == NudgeScheduler.KIND_SESSION) {
            NudgeScheduler.SESSION_CHANNEL
        } else {
            NudgeScheduler.MORNING_CHANNEL
        }

        val builder = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_monkey_notification)
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull() ?: body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(openIntent(context, planId, kind))

        // One tap from the nudge straight into recording — drunk users don't navigate (brief §4).
        if (kind == NudgeScheduler.KIND_SESSION) {
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    "Record drunk-you 🎥",
                    openIntent(context, planId, MainActivity.ROUTE_RECORD),
                ).build()
            )
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify("$planId-$kind".hashCode(), builder.build())
    }

    private fun openIntent(context: Context, planId: String, route: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            "$planId-$route-open".hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_PLAN_ID, planId)
                putExtra(MainActivity.EXTRA_ROUTE, route)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
