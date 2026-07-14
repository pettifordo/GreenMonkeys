package com.strive4it.greenmonkeys.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Redraws the widget just after midnight so the day count ticks over without
 * the app opening (SPEC §7), then re-arms itself for the next midnight.
 */
class MidnightRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        runBlocking { StreakWidget().updateAll(applicationContext) }
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "streak-midnight-refresh"

        fun schedule(context: Context) {
            val zone = ZoneId.systemDefault()
            val nextMidnight = ZonedDateTime.of(
                LocalDate.now(zone).plusDays(1),
                LocalTime.of(0, 1),
                zone,
            )
            val delay = Duration.between(ZonedDateTime.now(zone), nextMidnight)
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<MidnightRefreshWorker>()
                    .setInitialDelay(delay)
                    .build(),
            )
        }
    }
}
