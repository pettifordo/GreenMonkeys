package com.strive4it.greenmonkeys.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.logic.CharacterVoice
import com.strive4it.greenmonkeys.logic.StreakService
import com.strive4it.greenmonkeys.settings.SettingsRepository
import com.strive4it.greenmonkeys.data.PlanRepository
import kotlinx.coroutines.flow.first

/** Home-screen streak counter (SPEC §7). Computes days from the anchor at render time. */
class StreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = StreakSnapshotStore.load(context)
        provideContent {
            GlanceTheme {
                val days = snapshot.days()
                val red = days == 0 && snapshot.hasIdiotHistory
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Days since you were " +
                            "${CharacterVoice.article(snapshot.insultWord)} ${snapshot.insultWord}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = GlanceTheme.colors.onSurface,
                        ),
                    )
                    Text(
                        "$days",
                        style = TextStyle(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (red) ColorProvider(Color(0xFFE53935))
                            else ColorProvider(Color(0xFF4CAF50)),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        /** Recompute the snapshot from the database and redraw every widget. */
        suspend fun push(context: Context, repository: PlanRepository, settings: SettingsRepository) {
            val plans = repository.getAllPlans()
            val idiotDates = plans
                .filter { (it.verdict?.effectiveScore ?: 0) > 0 }
                .map { it.plan.sessionStart }
            StreakSnapshotStore.save(
                context,
                StreakSnapshot(
                    anchorDate = StreakService.lastIdiotDate(idiotDates),
                    hasIdiotHistory = idiotDates.isNotEmpty(),
                    firstUseDate = settings.firstUseDate(),
                    insultWord = settings.insultWord.first(),
                ),
            )
            StreakWidget().updateAll(context)
        }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

/** Convenience for app code that can't easily reach a coroutine scope. */
suspend fun pushStreakWidget(context: Context) {
    val app = context.applicationContext as? GreenMonkeysApp ?: return
    StreakWidget.push(context, app.planRepository, app.settings)
}
