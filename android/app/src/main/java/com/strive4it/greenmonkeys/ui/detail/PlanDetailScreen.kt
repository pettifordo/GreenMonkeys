package com.strive4it.greenmonkeys.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strive4it.greenmonkeys.logic.CharacterVoice
import com.strive4it.greenmonkeys.logic.PlanStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.getDefault())

/** Read-only plan view for planned/completed sessions, plus the guarded delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlanDetailViewModel = viewModel(factory = PlanDetailViewModel.factory(planId)),
) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    // 0 = no dialog; 1 / 2 = the two-stage sarcastic confirmation (hard rule 3).
    var deleteStage by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.plan?.occasion?.ifEmpty { "A session" } ?: "") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
                actions = {
                    if (plan != null) {
                        TextButton(onClick = { deleteStage = 1 }) { Text("Delete") }
                    }
                },
            )
        },
    ) { padding ->
        val current = plan ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    when (current.status()) {
                        PlanStatus.PLANNED -> "Planned"
                        PlanStatus.ACTIVE -> "🍻 Live now"
                        PlanStatus.AWAITING_VERDICT -> "🫣 Debrief due"
                        PlanStatus.COMPLETED -> "Judged"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                Column {
                    Text(
                        "Starts " + current.plan.sessionStart.atZone(ZoneId.systemDefault()).format(formatter),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Leaving " + current.plan.plannedEnd.atZone(ZoneId.systemDefault()).format(formatter),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            item {
                Text("Sober-you's promises", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp))
            }
            items(current.commitments, key = { it.id }) { commitment ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("${commitment.kind.symbol}  ${commitment.displayText}", modifier = Modifier.weight(1f))
                    when (commitment.wasBroken) {
                        true -> Text("Broken", color = MaterialTheme.colorScheme.error)
                        false -> Text("Kept", color = MaterialTheme.colorScheme.primary)
                        null -> {}
                    }
                }
            }

            current.verdict?.let { verdict ->
                item {
                    Text("The verdict", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp))
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${verdict.effectiveScore} out of 5", style = MaterialTheme.typography.titleLarge)
                        for (crime in verdict.crimes) {
                            Text("• $crime", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (verdict.oneChange.isNotEmpty()) {
                            Text("One change: ${verdict.oneChange}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (current.plan.reminderOffsetsMinutes.isNotEmpty()) {
                item {
                    Text(
                        "Check-ins: " + current.plan.reminderOffsetsMinutes.joinToString(", ") { "${it}m" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        if (deleteStage > 0) {
            AlertDialog(
                onDismissRequest = { deleteStage = 0 },
                title = { Text(if (deleteStage == 1) "Delete this session?" else "Really?") },
                text = {
                    Text(
                        if (deleteStage == 1) CharacterVoice.deleteWarnings.first
                        else CharacterVoice.deleteWarnings.second
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (deleteStage == 1) {
                            deleteStage = 2
                        } else {
                            deleteStage = 0
                            viewModel.delete(onDeleted)
                        }
                    }) { Text(if (deleteStage == 1) "Continue" else "Delete it all") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteStage = 0 }) { Text("Keep it") }
                },
            )
        }
    }
}
