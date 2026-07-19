package com.strive4it.greenmonkeys.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strive4it.greenmonkeys.logic.Brutality

/** Settings: word, noun, brutality, catalogs, debrief hour, lock, links (brief §5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var customWord by remember { mutableStateOf("") }
    var customNoun by remember { mutableStateOf("") }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionHeader("Your word") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (preset in state.insultPresets) {
                        FilterChip(
                            selected = state.insultWord == preset,
                            onClick = { viewModel.setInsultWord(preset) },
                            label = { Text(preset) },
                        )
                    }
                }
            }
            if (state.insultWord !in state.insultPresets) {
                item {
                    Text(
                        "Current: ${state.insultWord}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customWord,
                        onValueChange = { customWord = it },
                        placeholder = { Text("Or your own word…") },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            viewModel.setInsultWord(customWord.trim().lowercase())
                            customWord = ""
                        },
                        enabled = customWord.isNotBlank(),
                    ) { Text("Use") }
                }
            }

            item { SectionHeader("What do you call one?") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (preset in state.sessionNounPresets) {
                        FilterChip(
                            selected = state.sessionNoun == preset,
                            onClick = { viewModel.setSessionNoun(preset) },
                            label = { Text(preset) },
                        )
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customNoun,
                        onValueChange = { customNoun = it },
                        placeholder = { Text("Or your own name for it…") },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            viewModel.setSessionNoun(customNoun.trim())
                            customNoun = ""
                        },
                        enabled = customNoun.isNotBlank(),
                    ) { Text("Use") }
                }
            }
            item {
                Footer("Drives the plan button and empty states — \"Plan a ${state.sessionNoun}\", \"No ${state.sessionNoun} planned\".")
            }

            item { SectionHeader("How hard should the Monkeys go?") }
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Brutality.entries.forEachIndexed { index, level ->
                        SegmentedButton(
                            selected = state.brutality == level,
                            onClick = { viewModel.setBrutality(level) },
                            shape = SegmentedButtonDefaults.itemShape(index, Brutality.entries.size),
                        ) { Text(level.label) }
                    }
                }
            }
            item { Footer("Scales the jokes, not the debrief.") }

            item { SectionHeader("Your promise list") }
            if (state.customPromises.isEmpty()) {
                item {
                    Text(
                        "Custom promises you add while planning appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.customPromises, key = { "promise-$it" }) { promise ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("🤙 $promise", modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.removePromise(promise) }) { Text("Remove") }
                }
            }
            item { Footer("Past sessions keep their record either way.") }

            item { SectionHeader("Your booze-crime list") }
            if (state.customCrimes.isEmpty()) {
                item {
                    Text(
                        "Crimes you confess to that aren't on the standard charge sheet appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.customCrimes, key = { "crime-$it" }) { crime ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("🚨 $crime", modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.removeCrime(crime) }) { Text("Remove") }
                }
            }
            item { Footer("The built-in charges can't be removed. The law is the law.") }

            item { SectionHeader("Your record before the app") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Longest streak to beat", modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = if (state.seedLongestStreak == 0) "" else "${state.seedLongestStreak}",
                        onValueChange = { text ->
                            viewModel.setSeedLongestStreak(text.filter { it.isDigit() }.toIntOrNull() ?: 0)
                        },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                    )
                    Text("days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Footer(
                    "Already know your longest clean run? Enter it and the app won't call a new " +
                        "personal best until you've beaten it. A challenge from past-you to future-you."
                )
            }

            item { SectionHeader("The morning after") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Debrief at ${state.morningAfterHour}:00", modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.setMorningAfterHour(state.morningAfterHour - 1) },
                        enabled = state.morningAfterHour > 5,
                    ) { Text("−") }
                    TextButton(
                        onClick = { viewModel.setMorningAfterHour(state.morningAfterHour + 1) },
                        enabled = state.morningAfterHour < 14,
                    ) { Text("＋") }
                }
            }

            // Exact-alarm rationale (brief §7): nudges are user-scheduled reminders.
            if (!state.canScheduleExactAlarms) {
                item { SectionHeader("Check-in punctuality") }
                item {
                    Column {
                        Text(
                            "Android is currently allowed to delay the Monkey check-ins by a few " +
                                "minutes. For on-the-dot nudges, allow exact alarms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 31) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}"),
                                    )
                                )
                            }
                        }) { Text("Allow exact alarms") }
                    }
                }
            }

            item { SectionHeader("Privacy") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Lock app with biometrics", modifier = Modifier.weight(1f))
                    Switch(checked = state.appLockEnabled, onCheckedChange = viewModel::setAppLockEnabled)
                }
            }
            item {
                Footer(
                    "Your videos never leave this phone: not the cloud, not backups, not anywhere. " +
                        "The lock keeps over-the-shoulder eyes out too."
                )
            }

            item { SectionHeader("Help") }
            item {
                TextButton(onClick = { openUrl("https://pettifordo.github.io/GreenMonkeys/support.html") }) {
                    Text("❓ Help & support")
                }
            }
            item {
                TextButton(onClick = { openUrl("https://pettifordo.github.io/GreenMonkeys/privacy.html") }) {
                    Text("✋ Privacy policy")
                }
            }

            item {
                TextButton(onClick = { openUrl("https://www.nhs.uk/live-well/alcohol-advice/") }) {
                    Text("If this feels bigger than morning-after regret…")
                }
            }
            item {
                Footer(
                    "Green Monkeys is a self-awareness tool, not treatment. If drinking is worrying " +
                        "you, the NHS page above is a good, judgement-free start."
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
}

@Composable
private fun Footer(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Exposed for the rationale row. */
val android.content.Context.canScheduleExact: Boolean
    get() {
        val manager = getSystemService(AlarmManager::class.java) ?: return false
        return Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
    }
