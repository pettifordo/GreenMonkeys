package com.strive4it.greenmonkeys.ui.morning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.SessionPlanEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Morning-after with no pre-existing plan (SPEC §4): creates a retrospective
 * session — no commitments, no reminders — and goes straight to the debrief.
 * Regret shouldn't need a booking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnplannedDebriefScreen(
    onStartDebrief: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var occasion by remember { mutableStateOf("") }
    var night by remember { mutableStateOf(LocalDate.now().minusDays(1)) }
    var showingPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unplanned night") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "No plan, big night, rough morning? The Monkeys will still take your confession.",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = occasion,
                onValueChange = { occasion = it },
                placeholder = { Text("What was the occasion? (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = night.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Which night?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showingPicker = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            Text(
                "No promises were made, so there's nothing to judge against — just the verdict, " +
                    "the videos, and your one change.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    val app = context.applicationContext as GreenMonkeysApp
                    val zone = ZoneId.systemDefault()
                    val plan = SessionPlanEntity(
                        occasion = occasion.ifBlank { "Unplanned night" },
                        sessionStart = night.atTime(LocalTime.of(20, 0)).atZone(zone).toInstant(),
                        plannedEnd = night.atTime(LocalTime.of(23, 59)).atZone(zone).toInstant(),
                        reminderOffsetsMinutes = emptyList(),
                    )
                    scope.launch {
                        app.planRepository.savePlan(plan, emptyList())
                        onStartDebrief(plan.id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Face the music", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (showingPicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = night.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                selectableDates = object : androidx.compose.material3.SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        utcTimeMillis <= Instant.now().toEpochMilli()
                },
            )
            DatePickerDialog(
                onDismissRequest = { showingPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let {
                            night = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showingPicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showingPicker = false }) { Text("Cancel") } },
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}
