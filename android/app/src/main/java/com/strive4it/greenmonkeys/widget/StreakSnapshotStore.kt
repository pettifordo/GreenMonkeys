package com.strive4it.greenmonkeys.widget

import android.content.Context
import com.strive4it.greenmonkeys.logic.StreakService
import java.time.Instant

/**
 * The widget's only data: the streak ANCHOR DATE, first-use date, and the
 * word — never any video or session content (SPEC §7). The widget computes
 * the day count itself so it ticks over without the app opening.
 */
data class StreakSnapshot(
    val anchorDate: Instant?,
    val hasIdiotHistory: Boolean,
    val firstUseDate: Instant,
    val insultWord: String,
) {
    fun days(now: Instant = Instant.now()): Int =
        StreakService.daysSince(lastIdiotDate = anchorDate, firstUseDate = firstUseDate, now = now)
}

object StreakSnapshotStore {
    private const val PREFS = "streak_snapshot"

    fun save(context: Context, snapshot: StreakSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("anchor", snapshot.anchorDate?.toEpochMilli() ?: -1L)
            .putBoolean("hasHistory", snapshot.hasIdiotHistory)
            .putLong("firstUse", snapshot.firstUseDate.toEpochMilli())
            .putString("word", snapshot.insultWord)
            .apply()
    }

    fun load(context: Context): StreakSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val anchor = prefs.getLong("anchor", -1L)
        return StreakSnapshot(
            anchorDate = if (anchor >= 0) Instant.ofEpochMilli(anchor) else null,
            hasIdiotHistory = prefs.getBoolean("hasHistory", false),
            firstUseDate = Instant.ofEpochMilli(
                prefs.getLong("firstUse", Instant.now().toEpochMilli())
            ),
            insultWord = prefs.getString("word", "idiot") ?: "idiot",
        )
    }
}
