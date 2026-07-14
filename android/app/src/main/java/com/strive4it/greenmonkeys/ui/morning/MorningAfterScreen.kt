package com.strive4it.greenmonkeys.ui.morning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strive4it.greenmonkeys.logic.CharacterVoice
import com.strive4it.greenmonkeys.logic.VideoKind

/** The morning-after debrief. EVIDENCE LINKS AT TOP (brief §5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningAfterScreen(
    planId: String,
    onDelivered: (String) -> Unit,
    onPlayVideo: (String) -> Unit,
    onRecordMorningVideo: () -> Unit,
    recordedVideoFileName: String?,
    onRecordedVideoConsumed: () -> Unit,
    onBack: () -> Unit,
    viewModel: MorningAfterViewModel = viewModel(factory = MorningAfterViewModel.factory(planId)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var newCrime by remember { mutableStateOf("") }

    LaunchedEffect(recordedVideoFileName) {
        recordedVideoFileName?.let {
            viewModel.attachMorningVideo(it)
            onRecordedVideoConsumed()
        }
    }
    LaunchedEffect(state.delivered) {
        if (state.delivered) onDelivered(planId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The morning after") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
            )
        },
    ) { padding ->
        val plan = state.plan ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // The evidence leads: watch the tapes before you judge (owner request).
            val drunk = plan.video(VideoKind.DRUNK)
            val planVideo = plan.video(VideoKind.PLAN)
            if (drunk != null || planVideo != null) {
                item { SectionHeader("▶️ First, the evidence") }
                drunk?.let { video ->
                    item {
                        TextButton(onClick = { onPlayVideo(video.fileName) }) {
                            Text("▶️ Watch drunk you")
                        }
                    }
                }
                planVideo?.let { video ->
                    item {
                        TextButton(onClick = { onPlayVideo(video.fileName) }) {
                            Text("▶️ Rewatch sober you's plan")
                        }
                    }
                }
            }

            if (state.isJudged) {
                item {
                    TextButton(onClick = { onDelivered(planId) }) {
                        Text("🔨 The verdict has been delivered — see the roast")
                    }
                }
            } else {
                item { SectionHeader("🐒🫣 The morning committee") }
                item { Text(state.greeting, style = MaterialTheme.typography.titleMedium) }
                item {
                    Text(
                        state.paranoiaLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (drunk == null) {
                    item {
                        Text(
                            "No drunk video recorded. Suspiciously tidy, or too far gone to film?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (plan.commitments.isNotEmpty()) {
                item { SectionHeader("The promises — honestly now") }
                items(plan.commitments, key = { it.id }) { commitment ->
                    val broken = state.brokenFlags[commitment.id] ?: false
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(commitment.displayText)
                            Text(
                                if (broken) "Broken" else "Kept",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (broken) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                        }
                        Switch(
                            checked = broken,
                            onCheckedChange = { viewModel.toggleBroken(commitment.id, it) },
                            enabled = !state.isJudged,
                        )
                    }
                }
            }

            item { SectionHeader("The charge sheet") }
            items(state.crimeOptions, key = { "crime-$it" }) { crime ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(crime, modifier = Modifier.weight(1f))
                    Switch(
                        checked = crime in state.selectedCrimes,
                        onCheckedChange = { viewModel.toggleCrime(crime, it) },
                        enabled = !state.isJudged,
                    )
                }
            }
            if (!state.isJudged) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCrime,
                            onValueChange = { newCrime = it },
                            placeholder = { Text("Add your own crime…") },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                viewModel.confessCustomCrime(newCrime)
                                newCrime = ""
                            },
                            enabled = newCrime.isNotBlank(),
                        ) { Text("Confess") }
                    }
                }
            }
            item {
                Footer("Anything you add is remembered for future mornings. The Monkeys keep excellent records.")
            }

            item {
                SectionHeader(
                    "The verdict — how much of ${CharacterVoice.article(state.insultWord)} " +
                        "${state.insultWord} were you?"
                )
            }
            item {
                ScoreRow(
                    score = state.score,
                    enabled = !state.isJudged,
                    onPick = viewModel::setScore,
                )
            }
            state.score?.let { score ->
                item {
                    Text(
                        CharacterVoice.scoreGrading(score, state.insultWord),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    placeholder = { Text("Anything Captain Paranoia should let go of?") },
                    enabled = !state.isJudged,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Footer(
                    "The only mandatory question. Self-reported — the Monkeys will know if you lie. " +
                        "(They won't. But you will.)"
                )
            }

            item { SectionHeader("Say it to camera") }
            item {
                if (state.morningVideoFileName != null || plan.video(VideoKind.MORNING_AFTER) != null) {
                    Text("✅ Morning-after video recorded", color = MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = onRecordMorningVideo, enabled = !state.isJudged) {
                        Text("🎥 Record your morning-after video")
                    }
                }
            }

            item { SectionHeader("The one change (optional, but wise)") }
            item {
                OutlinedTextField(
                    value = state.oneChange,
                    onValueChange = viewModel::setOneChange,
                    placeholder = { Text("Next time I will…") },
                    enabled = !state.isJudged,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Footer("Shame fades by lunchtime; one concrete change doesn't.") }

            if (!state.isJudged) {
                item {
                    Button(
                        onClick = viewModel::deliverVerdict,
                        enabled = state.canDeliver,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text("Deliver the verdict", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// MARK: - Pieces

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Footer(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Mirrors the iOS 0–5 selector: green for 0, deepening red for 1–5. */
@Composable
private fun ScoreRow(score: Int?, enabled: Boolean, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        for (value in 0..5) {
            val selected = score == value
            val background = when {
                !selected -> MaterialTheme.colorScheme.surfaceVariant
                value == 0 -> Color(0xFF4CAF50)
                else -> Color.Red.copy(alpha = (0.35f + value * 0.13f).coerceAtMost(1f))
            }
            Text(
                "$value",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(background, RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { onPick(value) }
                    .padding(top = 9.dp),
            )
        }
    }
}
