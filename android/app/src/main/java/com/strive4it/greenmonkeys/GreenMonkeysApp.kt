package com.strive4it.greenmonkeys

import android.app.Application
import com.strive4it.greenmonkeys.capture.VideoStore
import com.strive4it.greenmonkeys.data.GreenMonkeysDatabase
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.notifications.NudgeScheduler
import com.strive4it.greenmonkeys.settings.SettingsRepository
import com.strive4it.greenmonkeys.widget.MidnightRefreshWorker
import com.strive4it.greenmonkeys.widget.pushStreakWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Composition root — a single-user local-only app needs no DI framework. */
class GreenMonkeysApp : Application() {

    val planRepository: PlanRepository by lazy {
        PlanRepository(GreenMonkeysDatabase.get(this).planDao())
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    val nudgeScheduler: NudgeScheduler by lazy { NudgeScheduler(this) }

    val videoStore: VideoStore by lazy { VideoStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        nudgeScheduler.ensureChannels()
        MidnightRefreshWorker.schedule(this)
        appScope.launch { pushStreakWidget(this@GreenMonkeysApp) }
    }
}
