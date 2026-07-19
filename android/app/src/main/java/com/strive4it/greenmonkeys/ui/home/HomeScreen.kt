package com.strive4it.greenmonkeys.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strive4it.greenmonkeys.data.PlanWithDetails
import com.strive4it.greenmonkeys.logic.CharacterVoice
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The home screen — brief §5 parity checklist, copy mirrored from iOS HomeView. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPlanTapped: (PlanWithDetails) -> Unit,
    onNewPlan: () -> Unit,
    onConfess: () -> Unit,
    onPattern: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var planToCancel by remember { mutableStateOf<PlanWithDetails?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Green Monkeys") },
                actions = { TextButton(onClick = onSettings) { Text("⚙️") } },
            )
        },
        bottomBar = {
            Button(
                onClick = onNewPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("＋  Plan a ${state.sessionNoun}", style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            item {
                StreakHeadline(
                    word = state.insultWord,
                    days = state.streakDays,
                    hasIdiotHistory = state.hasIdiotHistory,
                    tagline = state.streakTagline,
                    recordCaption = state.recordCaption,
                    onTap = onPattern,
                )
            }

            state.activePlan?.let { plan ->
                item { SectionHeader("Session in progress") }
                item {
                    DismissiblePlanRow(plan, badge = "🍻 Live",
                        onTap = { onPlanTapped(plan) }, onAskCancel = { planToCancel = plan })
                }
            }

            if (state.awaitingVerdict.isNotEmpty()) {
                item { SectionHeader("Time to face the music") }
                items(state.awaitingVerdict, key = { "await-${it.plan.id}" }) { plan ->
                    DismissiblePlanRow(plan, badge = "🫣 Debrief due",
                        onTap = { onPlanTapped(plan) }, onAskCancel = { planToCancel = plan })
                }
            }

            item { SectionHeader("Coming up") }
            if (state.upcoming.isEmpty()) {
                item {
                    Text(
                        "No ${state.sessionNoun} planned. The Monkeys rest… for now. Tap to fix that.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNewPlan)
                            .padding(vertical = 8.dp),
                    )
                }
            }
            items(state.upcoming, key = { "up-${it.plan.id}" }) { plan ->
                DismissiblePlanRow(plan, badge = null,
                    onTap = { onPlanTapped(plan) }, onAskCancel = { planToCancel = plan })
            }

            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    TextButton(onClick = onConfess) {
                        Text("🌅  Rough morning, no plan? Confess")
                    }
                    Text(
                        "Last night got away from you without a plan? The debrief still counts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }

        planToCancel?.let { plan ->
            val planned = plan.status() == com.strive4it.greenmonkeys.logic.PlanStatus.PLANNED
            val name = plan.plan.occasion.ifEmpty { "This one" }.let {
                if (plan.plan.occasion.isEmpty()) it else "\"$it\""
            }
            AlertDialog(
                onDismissRequest = { planToCancel = null },
                title = {
                    Text(if (planned) "Cancel this ${state.sessionNoun}?" else "Delete this ${state.sessionNoun}?")
                },
                text = {
                    Text(
                        if (planned) {
                            "Are you sure? $name goes quietly — but you know the Monkeys are " +
                                "still watching. They saw you plan it."
                        } else {
                            "Are you sure? Was it created by mistake, or did it just go badly? " +
                                "$name and its videos vanish — but the Monkeys are still watching, " +
                                "and deleting it won't delete the memory of it."
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.cancelPlan(plan)
                        planToCancel = null
                    }) {
                        Text(if (planned) "It was created by mistake — delete it" else "Delete it — evidence and all")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { planToCancel = null }) { Text("Keep it") }
                },
            )
        }
    }
}

/** Swipe a row away to cancel/delete it — always behind the confirmation. */
@Composable
private fun DismissiblePlanRow(
    plan: PlanWithDetails,
    badge: String?,
    onTap: () -> Unit,
    onAskCancel: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onAskCancel()
            false // never auto-dismiss; the dialog decides
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("🗑", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) {
        PlanRow(plan, badge = badge, onTap = onTap)
    }
}

@Composable
private fun StreakHeadline(
    word: String,
    days: Int,
    hasIdiotHistory: Boolean,
    tagline: String,
    recordCaption: String,
    onTap: () -> Unit,
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onTap)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
        ) {
            Text(
                "Days since you were ${CharacterVoice.article(word)} $word",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$days",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = if (days == 0 && hasIdiotHistory) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Text(
                tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                recordCaption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private val rowFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.getDefault())

@Composable
fun PlanRow(plan: PlanWithDetails, badge: String?, onTap: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onTap)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    plan.plan.occasion.ifEmpty { "A session" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (badge != null) {
                    Text(badge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                plan.plan.sessionStart.atZone(ZoneId.systemDefault()).format(rowFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (plan.commitments.isNotEmpty()) {
                Text(
                    plan.commitments.joinToString(" ") { it.kind.symbol },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
