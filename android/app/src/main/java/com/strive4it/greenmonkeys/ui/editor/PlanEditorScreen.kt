package com.strive4it.greenmonkeys.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strive4it.greenmonkeys.logic.CommitmentKind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Where sober-you makes the promises (SPEC §1.1, §4). Plan video lands with the camera work. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    onDone: () -> Unit,
    onRecordPlanVideo: () -> Unit = {},
    recordedVideoFileName: String? = null,
    onRecordedVideoConsumed: () -> Unit = {},
    viewModel: PlanEditorViewModel = viewModel(factory = PlanEditorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    LaunchedEffect(recordedVideoFileName) {
        recordedVideoFileName?.let {
            viewModel.attachPlanVideo(it)
            onRecordedVideoConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New ${state.sessionNoun}") },
                navigationIcon = { TextButton(onClick = onDone) { Text("Cancel") } },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = state.canSave) { Text("Save") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionHeader("The occasion") }
            item {
                OutlinedTextField(
                    value = state.occasion,
                    onValueChange = viewModel::setOccasion,
                    placeholder = { Text("e.g. Friday pub, Dave's 40th") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                DateField(date = state.date, onPicked = viewModel::setDate)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField("Starts", state.startTime, viewModel::setStartTime, Modifier.weight(1f))
                    TimeField("Leaving at", state.endTime, viewModel::setEndTime, Modifier.weight(1f))
                }
            }

            item { SectionHeader("Sober-you's promises") }
            items(state.drafts, key = { it.id }) { draft ->
                DraftRow(
                    draft = draft,
                    onDetailChange = { viewModel.updateDetail(draft.id, it) },
                    onRemove = { viewModel.removeDraft(draft.id) },
                )
            }
            item {
                AddPromiseMenu(
                    availableKinds = state.availableKinds,
                    savedCustoms = state.availableSavedCustoms,
                    onKind = viewModel::addKind,
                    onSavedCustom = viewModel::addCustom,
                    onNewCustom = { viewModel.addCustom("") },
                )
            }
            state.patternCallback?.let { callback ->
                item {
                    Text(
                        callback,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            item { SectionHeader("Monkey check-ins during the session") }
            items(PlanEditorViewModel.reminderChoices, key = { "offset-$it" }) { minutes ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(reminderLabel(minutes, state), modifier = Modifier.weight(1f))
                    Switch(
                        checked = minutes in state.reminderOffsets,
                        onCheckedChange = { viewModel.toggleReminder(minutes) },
                    )
                }
            }
            item {
                Text(
                    "Each check-in resurfaces the plan with one tap to record drunk-you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item { SectionHeader("Message from sober you") }
            item {
                if (state.planVideoFileName != null) {
                    Text("✅ Plan video recorded", color = MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = onRecordPlanVideo) {
                        Text("🎥 Record your plan video")
                    }
                }
            }
            item {
                Text(
                    "30 seconds of sober you saying exactly what the plan is. " +
                        "Tomorrow-you will thank you. Or wince.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// MARK: - Pieces

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: LocalDate, onPicked: (LocalDate) -> Unit) {
    var showing by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date.format(dateFormatter),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text("Date") },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showing = true },
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
    if (showing) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showing = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showing = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showing = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, time: LocalTime, onPicked: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var showing by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = "%02d:%02d".format(time.hour, time.minute),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        modifier = modifier.clickable { showing = true },
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
    if (showing) {
        val pickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showing = false },
            confirmButton = {
                TextButton(onClick = {
                    onPicked(LocalTime.of(pickerState.hour, pickerState.minute))
                    showing = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showing = false }) { Text("Cancel") } },
            text = { TimePicker(state = pickerState) },
        )
    }
}

@Composable
private fun DraftRow(
    draft: DraftCommitment,
    onDetailChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(draft.kind.symbol)
        when (draft.kind) {
            CommitmentKind.MAX_DRINKS -> {
                OutlinedTextField(
                    value = draft.detail,
                    onValueChange = onDetailChange,
                    label = { Text("Max drinks") },
                    modifier = Modifier.weight(1f),
                )
            }
            CommitmentKind.LEAVE_BY -> {
                OutlinedTextField(
                    value = draft.detail,
                    onValueChange = onDetailChange,
                    label = { Text("Leave by") },
                    placeholder = { Text("23:00") },
                    modifier = Modifier.weight(1f),
                )
            }
            CommitmentKind.CUSTOM -> {
                OutlinedTextField(
                    value = draft.detail,
                    onValueChange = onDetailChange,
                    placeholder = { Text("Your promise…") },
                    modifier = Modifier.weight(1f),
                )
            }
            else -> Text(draft.kind.label, modifier = Modifier.weight(1f))
        }
        TextButton(onClick = onRemove) { Text("✕") }
    }
}

@Composable
private fun AddPromiseMenu(
    availableKinds: List<CommitmentKind>,
    savedCustoms: List<String>,
    onKind: (CommitmentKind) -> Unit,
    onSavedCustom: (String) -> Unit,
    onNewCustom: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = true }) { Text("＋ Add a promise") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (kind in availableKinds) {
                DropdownMenuItem(
                    text = { Text("${kind.symbol} ${kind.label}") },
                    onClick = {
                        onKind(kind)
                        expanded = false
                    },
                )
            }
            if (savedCustoms.isNotEmpty()) {
                HorizontalDivider()
                for (text in savedCustoms) {
                    DropdownMenuItem(
                        text = { Text("🤙 $text") },
                        onClick = {
                            onSavedCustom(text)
                            expanded = false
                        },
                    )
                }
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("✍️ New promise…") },
                onClick = {
                    onNewCustom()
                    expanded = false
                },
            )
        }
    }
}

private fun reminderLabel(minutes: Int, state: EditorUiState): String {
    val fire = state.sessionStart.plusSeconds(minutes * 60L)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    val hours = if (minutes % 60 == 0) "${minutes / 60}h" else "${minutes}m"
    return "$hours in — around %02d:%02d".format(fire.hour, fire.minute)
}
