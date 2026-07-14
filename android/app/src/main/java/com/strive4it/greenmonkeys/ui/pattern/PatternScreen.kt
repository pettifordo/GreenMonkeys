package com.strive4it.greenmonkeys.ui.pattern

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.data.PlanWithDetails
import com.strive4it.greenmonkeys.logic.CommitmentRecord
import com.strive4it.greenmonkeys.logic.CrimeCount
import com.strive4it.greenmonkeys.logic.PatternService
import com.strive4it.greenmonkeys.logic.PromiseHistory
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One judged night, flattened for charting. */
data class NightPoint(
    val date: Instant,
    val score: Int,
    val crimes: Int,
    val brokenPromises: Int,
)

data class PatternUiState(
    val insultWord: String = "idiot",
    val judged: List<PlanWithDetails> = emptyList(),
    val scores: List<Int> = emptyList(),
    val nightPoints: List<NightPoint> = emptyList(),
    val repeatOffenders: List<PromiseHistory> = emptyList(),
    val crimeCounts: List<CrimeCount> = emptyList(),
) {
    val idiotRate: Double? get() = PatternService.idiotRate(scores)
    val averageScore: Double? get() = PatternService.averageScore(scores)
}

class PatternViewModel(
    repository: PlanRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<PatternUiState> = combine(
        repository.observePlans(),
        settings.insultWord,
    ) { plans, word ->
        val judged = plans.filter { it.verdict != null }
        val records = judged.flatMap { it.commitments }.mapNotNull { c ->
            c.wasBroken?.let { CommitmentRecord(c.patternKey, c.patternLabel, it) }
        }
        PatternUiState(
            insultWord = word,
            judged = judged.sortedByDescending { it.plan.sessionStart },
            scores = judged.mapNotNull { it.verdict?.effectiveScore },
            nightPoints = judged
                .sortedBy { it.plan.sessionStart }
                .map { plan ->
                    NightPoint(
                        date = plan.plan.sessionStart,
                        score = plan.verdict?.effectiveScore ?: 0,
                        crimes = plan.verdict?.crimes?.size ?: 0,
                        brokenPromises = plan.commitments.count { it.wasBroken == true },
                    )
                },
            repeatOffenders = PatternService.repeatOffenders(records),
            crimeCounts = PatternService.crimeCounts(judged.mapNotNull { it.verdict?.crimes }),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatternUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                PatternViewModel(app.planRepository, app.settings)
            }
        }
    }
}

/** The pattern: score stats, two charts, repeat offenders, lifetime charge sheet, the record. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternScreen(
    onPlanTapped: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PatternViewModel = viewModel(factory = PatternViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The pattern") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            item { SectionHeader("The score") }
            if (state.scores.isEmpty()) {
                item {
                    Text(
                        "No judged sessions yet. The Monkeys await their first case.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item { StatRow("Sessions judged", "${state.judged.size}") }
                state.idiotRate?.let { rate ->
                    item {
                        StatRow(
                            "${state.insultWord.replaceFirstChar { it.uppercase() }} rate",
                            "${(rate * 100).toInt()}%",
                        )
                    }
                }
                state.averageScore?.let { average ->
                    item { StatRow("Average ${state.insultWord} score", "%.1f / 5".format(average)) }
                }
                state.scores.maxOrNull()?.let { worst ->
                    item { StatRow("Personal best (worst)", "$worst / 5") }
                }
            }

            if (state.nightPoints.size >= 2) {
                item {
                    SectionHeader(
                        "${state.insultWord.replaceFirstChar { it.uppercase() }} score over time"
                    )
                }
                item {
                    ScoreChart(
                        points = state.nightPoints,
                        average = state.averageScore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .padding(vertical = 8.dp),
                    )
                }
                item {
                    Footer("Green nights keep the streak. The dashed line is who you are on average — aim under it.")
                }

                item { SectionHeader("Misdeeds per night") }
                item {
                    MisdeedsChart(
                        points = state.nightPoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .padding(vertical = 8.dp),
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendSwatch(Color(0xFFE53935)); Text(" Booze crimes   ", style = MaterialTheme.typography.bodySmall)
                        LegendSwatch(Color(0xFFFB8C00)); Text(" Broken promises", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    Footer("Crimes confessed plus promises broken, stacked. Flat is the goal; the Monkeys prefer drama.")
                }
            }

            if (state.repeatOffenders.isNotEmpty()) {
                item { SectionHeader("Promises you keep breaking") }
                items(state.repeatOffenders, key = { "offender-${it.key}" }) { history ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(history.label)
                        Text(
                            "Promised ${history.timesPromised}× · broken ${history.timesBroken}× · kept ${history.timesKept}×",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.crimeCounts.isNotEmpty()) {
                item { SectionHeader("The charge sheet, lifetime edition") }
                items(state.crimeCounts, key = { "count-${it.crime}" }) { entry ->
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Text(entry.crime, modifier = Modifier.weight(1f))
                        Text(
                            "${entry.count}×",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item { SectionHeader("The record") }
            items(state.judged, key = { "rec-${it.plan.id}" }) { plan ->
                val score = plan.verdict?.effectiveScore ?: 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlanTapped(plan.plan.id) }
                        .padding(vertical = 6.dp),
                ) {
                    Text(if (score == 0) "🎉" else "🙈")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(plan.plan.occasion.ifEmpty { "A session" })
                        Text(
                            plan.plan.sessionStart.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (score > 0) {
                        Text(
                            "$score/5",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Charts (Compose Canvas — no chart dependency, brief §4)

private const val MAX_SCORE = 5f

/**
 * Score-over-time: date-positioned bars (gaps meaningful), y locked 0–5,
 * clean nights drawn as green dots on the axis, dashed lifetime average.
 */
@Composable
private fun ScoreChart(points: List<NightPoint>, average: Double?, modifier: Modifier = Modifier) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val minT = points.first().date.epochSecond.toFloat()
        val maxT = points.last().date.epochSecond.toFloat()
        val span = (maxT - minT).coerceAtLeast(1f)
        val barWidth = (size.width / (points.size * 2.5f)).coerceAtMost(28.dp.toPx())
        fun x(point: NightPoint): Float =
            barWidth / 2 + (point.date.epochSecond - minT) / span * (size.width - barWidth)
        fun y(value: Float): Float = size.height - value / MAX_SCORE * size.height

        // Axis
        drawLine(axisColor, Offset(0f, size.height), Offset(size.width, size.height), 2f)

        for (point in points) {
            if (point.score == 0) {
                // A zero-height bar is invisible — clean nights get a green dot on the axis.
                drawCircle(Color(0xFF4CAF50), radius = 6.dp.toPx(), center = Offset(x(point), size.height))
            } else {
                val alpha = (0.35f + point.score * 0.13f).coerceAtMost(1f)
                drawRoundRect(
                    color = Color.Red.copy(alpha = alpha),
                    topLeft = Offset(x(point) - barWidth / 2, y(point.score.toFloat())),
                    size = Size(barWidth, size.height - y(point.score.toFloat())),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }

        average?.let {
            val avgY = y(it.toFloat())
            drawLine(
                axisColor,
                Offset(0f, avgY),
                Offset(size.width, avgY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
        }
    }
}

/** Misdeeds per night: crimes + broken promises, stacked. */
@Composable
private fun MisdeedsChart(points: List<NightPoint>, modifier: Modifier = Modifier) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val maxCount = points.maxOf { it.crimes + it.brokenPromises }.coerceAtLeast(1).toFloat()
        val minT = points.first().date.epochSecond.toFloat()
        val maxT = points.last().date.epochSecond.toFloat()
        val span = (maxT - minT).coerceAtLeast(1f)
        val barWidth = (size.width / (points.size * 2.5f)).coerceAtMost(28.dp.toPx())
        fun x(point: NightPoint): Float =
            barWidth / 2 + (point.date.epochSecond - minT) / span * (size.width - barWidth)

        drawLine(axisColor, Offset(0f, size.height), Offset(size.width, size.height), 2f)

        for (point in points) {
            val crimeHeight = point.crimes / maxCount * size.height
            val brokenHeight = point.brokenPromises / maxCount * size.height
            if (crimeHeight > 0) {
                drawRoundRect(
                    color = Color(0xFFE53935),
                    topLeft = Offset(x(point) - barWidth / 2, size.height - crimeHeight),
                    size = Size(barWidth, crimeHeight),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            if (brokenHeight > 0) {
                drawRoundRect(
                    color = Color(0xFFFB8C00),
                    topLeft = Offset(x(point) - barWidth / 2, size.height - crimeHeight - brokenHeight),
                    size = Size(barWidth, brokenHeight),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}

// MARK: - Pieces

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Footer(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LegendSwatch(color: Color) {
    Canvas(modifier = Modifier
        .width(12.dp)
        .height(12.dp)) {
        drawRoundRect(color, cornerRadius = CornerRadius(2.dp.toPx()))
    }
}
