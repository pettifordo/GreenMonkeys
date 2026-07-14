package com.strive4it.greenmonkeys.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.strive4it.greenmonkeys.GreenMonkeysApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms don't survive a reboot — reschedule every plan with future events. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? GreenMonkeysApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (plan in app.planRepository.getAllPlans()) {
                    if (plan.verdict == null) {
                        app.nudgeScheduler.schedule(plan, app.settings)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
