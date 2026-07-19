package com.strive4it.greenmonkeys.ui.morning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.data.PlanWithDetails
import com.strive4it.greenmonkeys.data.SessionVideoEntity
import com.strive4it.greenmonkeys.data.VerdictEntity
import com.strive4it.greenmonkeys.logic.Brutality
import com.strive4it.greenmonkeys.logic.CharacterVoice
import com.strive4it.greenmonkeys.logic.VideoKind
import com.strive4it.greenmonkeys.notifications.NudgeScheduler
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MorningUiState(
    val plan: PlanWithDetails? = null,
    val insultWord: String = "idiot",
    val greeting: String = "",
    val paranoiaLine: String = "",
    val brokenFlags: Map<String, Boolean> = emptyMap(),
    val score: Int? = null,
    val crimeOptions: List<String> = emptyList(),
    val selectedCrimes: Set<String> = emptySet(),
    val oneChange: String = "",
    val note: String = "",
    val morningVideoFileName: String? = null,
    /** Set after "Deliver the verdict" (or when revisiting a judged plan). */
    val delivered: Boolean = false,
) {
    val isJudged: Boolean get() = plan?.verdict != null
    val canDeliver: Boolean get() = !isJudged && score != null

    /**
     * Retro sessions (the confess flow) have no promises and no reminders —
     * drunk-you COULDN'T have recorded in-app, so don't accuse.
     */
    val isUnplanned: Boolean
        get() = plan?.commitments?.isEmpty() == true &&
            plan.plan.reminderOffsetsMinutes.isEmpty()
}

/**
 * The debrief: judge each promise, confess the crimes, score yourself 0–5.
 * Only the score is mandatory — but the flow still ends pointing forward
 * (SPEC hard rule 5). Ported from iOS `MorningAfterView`.
 */
class MorningAfterViewModel(
    private val planId: String,
    private val repository: PlanRepository,
    private val settings: SettingsRepository,
    private val scheduler: NudgeScheduler,
    private val appContext: android.content.Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MorningUiState())
    val uiState: StateFlow<MorningUiState> = _uiState

    init {
        viewModelScope.launch {
            val word = settings.insultWord.first()
            val brutality: Brutality = settings.brutality.first()
            val allCrimes = settings.allCrimes.first()
            val plan = repository.getPlan(planId)

            var options = allCrimes
            val verdict = plan?.verdict
            if (verdict != null) {
                // Show this session's crimes even if later removed from the catalog.
                options = options + verdict.crimes.filter { it !in options }
            }

            _uiState.update {
                it.copy(
                    plan = plan,
                    insultWord = word,
                    greeting = CharacterVoice.monkeyMorningGreeting(brutality, word),
                    paranoiaLine = CharacterVoice.paranoiaMorningLine(brutality),
                    brokenFlags = plan?.commitments?.associate { c -> c.id to (c.wasBroken ?: false) }
                        ?: emptyMap(),
                    score = verdict?.effectiveScore,
                    crimeOptions = options,
                    selectedCrimes = verdict?.crimes?.toSet() ?: emptySet(),
                    oneChange = verdict?.oneChange ?: "",
                    note = verdict?.note ?: "",
                )
            }
        }
    }

    fun toggleBroken(commitmentId: String, broken: Boolean) =
        _uiState.update { it.copy(brokenFlags = it.brokenFlags + (commitmentId to broken)) }

    fun setScore(value: Int) = _uiState.update { it.copy(score = value) }

    fun toggleCrime(crime: String, selected: Boolean) = _uiState.update {
        it.copy(selectedCrimes = if (selected) it.selectedCrimes + crime else it.selectedCrimes - crime)
    }

    fun setOneChange(value: String) = _uiState.update { it.copy(oneChange = value) }
    fun setNote(value: String) = _uiState.update { it.copy(note = value) }
    fun attachMorningVideo(fileName: String) =
        _uiState.update { it.copy(morningVideoFileName = fileName) }

    /**
     * "Someone filmed it, didn't they." Copies a picked library video into
     * private storage as drunk evidence — imports stay on this phone like
     * everything else (SPEC §5).
     */
    fun importDrunkVideo(uri: android.net.Uri) {
        val plan = _uiState.value.plan ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val store = (appContext as GreenMonkeysApp).videoStore
            val temp = store.newTempFile()
            try {
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                } ?: return@launch
                val fileName = store.store(temp)
                repository.addVideo(
                    SessionVideoEntity(
                        planId = plan.plan.id,
                        kind = VideoKind.DRUNK.rawValue,
                        fileName = fileName,
                    )
                )
                _uiState.update { it.copy(plan = repository.getPlan(plan.plan.id)) }
            } finally {
                temp.delete()
            }
        }
    }

    /** Anything confessed is remembered for future mornings (SPEC §2a). */
    fun confessCustomCrime(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            settings.rememberCrime(trimmed)
            val options = settings.allCrimes.first()
            // The typed text may differ in case from an existing entry — select the canonical one.
            val canonical = options.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
            _uiState.update {
                it.copy(
                    crimeOptions = if (canonical in options) options else options + canonical,
                    selectedCrimes = it.selectedCrimes + canonical,
                )
            }
        }
    }

    fun deliverVerdict() {
        val state = _uiState.value
        val plan = state.plan ?: return
        val score = state.score ?: return
        if (state.isJudged) {
            _uiState.update { it.copy(delivered = true) }
            return
        }
        viewModelScope.launch {
            for (commitment in plan.commitments) {
                repository.judgeCommitment(commitment, state.brokenFlags[commitment.id] ?: false)
            }
            repository.recordVerdict(
                VerdictEntity(
                    planId = plan.plan.id,
                    score = score,
                    wasIdiot = score > 0,
                    crimes = state.selectedCrimes.sorted(),
                    oneChange = state.oneChange,
                    note = state.note,
                )
            )
            state.morningVideoFileName?.let { fileName ->
                repository.addVideo(
                    SessionVideoEntity(
                        planId = plan.plan.id,
                        kind = VideoKind.MORNING_AFTER.rawValue,
                        fileName = fileName,
                    )
                )
            }
            scheduler.cancel(plan)
            // Keep the widget honest immediately — don't wait for a Home visit.
            com.strive4it.greenmonkeys.widget.pushStreakWidget(appContext)
            _uiState.update { it.copy(delivered = true) }
        }
    }

    companion object {
        fun factory(planId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                MorningAfterViewModel(planId, app.planRepository, app.settings, app.nudgeScheduler, app)
            }
        }
    }
}
