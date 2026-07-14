package com.strive4it.greenmonkeys.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.CommitmentEntity
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.data.SessionPlanEntity
import com.strive4it.greenmonkeys.logic.Brutality
import com.strive4it.greenmonkeys.logic.CharacterVoice
import com.strive4it.greenmonkeys.logic.CommitmentKind
import com.strive4it.greenmonkeys.logic.CommitmentRecord
import com.strive4it.greenmonkeys.logic.PatternService
import com.strive4it.greenmonkeys.logic.patternKey
import com.strive4it.greenmonkeys.notifications.NudgeScheduler
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class DraftCommitment(
    val id: String = UUID.randomUUID().toString(),
    val kind: CommitmentKind,
    val detail: String,
)

data class EditorUiState(
    val occasion: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(19, 0),
    val endTime: LocalTime = LocalTime.of(23, 0),
    val drafts: List<DraftCommitment> = listOf(
        DraftCommitment(kind = CommitmentKind.MAX_DRINKS, detail = "4"),
        DraftCommitment(kind = CommitmentKind.NO_DRIVING, detail = ""),
    ),
    val reminderOffsets: Set<Int> = setOf(60, 120, 180),
    val savedCustomPromises: List<String> = emptyList(),
    val sessionNoun: String = "Session",
    val patternCallback: String? = null,
    val planVideoFileName: String? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = drafts.isNotEmpty()

    val sessionStart: Instant
        get() = date.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()

    /** Leaving time on the same evening; past midnight rolls to the next day. */
    val plannedEnd: Instant
        get() {
            val endDate = if (endTime > startTime) date else date.plusDays(1)
            return endDate.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant()
        }

    /** Built-in kinds not yet drafted (customs can repeat with different text). */
    val availableKinds: List<CommitmentKind>
        get() = CommitmentKind.entries.filter { kind ->
            kind != CommitmentKind.CUSTOM && drafts.none { it.kind == kind }
        }

    /** Previously used custom promises, minus ones already drafted. */
    val availableSavedCustoms: List<String>
        get() = savedCustomPromises.filter { text ->
            drafts.none { it.kind == CommitmentKind.CUSTOM && it.detail.equals(text, ignoreCase = true) }
        }
}

class PlanEditorViewModel(
    private val repository: PlanRepository,
    private val settings: SettingsRepository,
    private val scheduler: NudgeScheduler,
) : ViewModel() {

    companion object {
        val reminderChoices = listOf(30, 60, 90, 120, 180, 240)

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                PlanEditorViewModel(app.planRepository, app.settings, app.nudgeScheduler)
            }
        }
    }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

    private var judgedRecords: List<CommitmentRecord> = emptyList()
    private var brutality: Brutality = Brutality.STANDARD
    private var insultWord: String = "idiot"

    init {
        viewModelScope.launch {
            judgedRecords = repository.judgedRecords()
            brutality = settings.brutality.first()
            insultWord = settings.insultWord.first()
            _uiState.update {
                it.copy(
                    savedCustomPromises = settings.customPromises.first(),
                    sessionNoun = settings.sessionNoun.first(),
                )
            }
            refreshCallback()
        }
    }

    fun setOccasion(value: String) = _uiState.update { it.copy(occasion = value) }
    fun setDate(value: LocalDate) = _uiState.update { it.copy(date = value) }
    fun setStartTime(value: LocalTime) = _uiState.update { it.copy(startTime = value) }
    fun setEndTime(value: LocalTime) = _uiState.update { it.copy(endTime = value) }

    fun addKind(kind: CommitmentKind) {
        val defaultDetail = when (kind) {
            CommitmentKind.MAX_DRINKS -> "4"
            CommitmentKind.LEAVE_BY -> _uiState.value.endTime.toString()
            else -> ""
        }
        _uiState.update { it.copy(drafts = it.drafts + DraftCommitment(kind = kind, detail = defaultDetail)) }
        refreshCallback()
    }

    fun addCustom(text: String) {
        _uiState.update { it.copy(drafts = it.drafts + DraftCommitment(kind = CommitmentKind.CUSTOM, detail = text)) }
        refreshCallback()
    }

    fun updateDetail(id: String, detail: String) {
        _uiState.update { state ->
            state.copy(drafts = state.drafts.map { if (it.id == id) it.copy(detail = detail) else it })
        }
        refreshCallback()
    }

    fun removeDraft(id: String) {
        _uiState.update { state -> state.copy(drafts = state.drafts.filterNot { it.id == id }) }
        refreshCallback()
    }

    fun attachPlanVideo(fileName: String) =
        _uiState.update { it.copy(planVideoFileName = fileName) }

    fun toggleReminder(minutes: Int) {
        _uiState.update { state ->
            val offsets = if (minutes in state.reminderOffsets) {
                state.reminderOffsets - minutes
            } else {
                state.reminderOffsets + minutes
            }
            state.copy(reminderOffsets = offsets)
        }
    }

    /** "You've promised this before" — the Monkeys remember (SPEC §1.4). */
    private fun refreshCallback() {
        val state = _uiState.value
        val callback = state.drafts.firstNotNullOfOrNull { draft ->
            val key = patternKey(draft.kind, draft.detail)
            if (key.isEmpty()) return@firstNotNullOfOrNull null
            val history = PatternService.history(key, judgedRecords)
            CharacterVoice.patternCallback(
                label = if (draft.kind == CommitmentKind.CUSTOM) draft.detail else draft.kind.label,
                timesPromised = history.timesPromised,
                timesBroken = history.timesBroken,
                brutality = brutality,
                word = insultWord,
            )
        }
        _uiState.update { it.copy(patternCallback = callback) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val plan = SessionPlanEntity(
                occasion = state.occasion,
                sessionStart = state.sessionStart,
                plannedEnd = state.plannedEnd,
                reminderOffsetsMinutes = state.reminderOffsets.sorted(),
            )
            val commitments = state.drafts.map {
                CommitmentEntity(planId = plan.id, kindRaw = it.kind.rawValue, detail = it.detail)
            }
            repository.savePlan(plan, commitments)

            // Custom promises join the catalog for future plans.
            for (draft in state.drafts.filter { it.kind == CommitmentKind.CUSTOM }) {
                settings.rememberPromise(draft.detail)
            }

            state.planVideoFileName?.let { fileName ->
                repository.addVideo(
                    com.strive4it.greenmonkeys.data.SessionVideoEntity(
                        planId = plan.id,
                        kind = com.strive4it.greenmonkeys.logic.VideoKind.PLAN.rawValue,
                        fileName = fileName,
                    )
                )
            }

            repository.getPlan(plan.id)?.let { scheduler.schedule(it, settings) }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
