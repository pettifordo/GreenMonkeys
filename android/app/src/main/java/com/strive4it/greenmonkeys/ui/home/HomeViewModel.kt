package com.strive4it.greenmonkeys.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.data.PlanWithDetails
import com.strive4it.greenmonkeys.logic.PlanStatus
import com.strive4it.greenmonkeys.logic.StreakService
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val insultWord: String = "idiot",
    val sessionNoun: String = "Session",
    val streakDays: Int = 0,
    val hasIdiotHistory: Boolean = false,
    val activePlan: PlanWithDetails? = null,
    val awaitingVerdict: List<PlanWithDetails> = emptyList(),
    val upcoming: List<PlanWithDetails> = emptyList(),
) {
    /** Mirrors iOS HomeView.streakTagline. */
    val streakTagline: String
        get() = when {
            streakDays == 0 && hasIdiotHistory -> "Rock bottom has a floor. Build on it."
            !hasIdiotHistory -> "Clean record so far. The Monkeys are watching. 🐒"
            else -> "Keep it going. 🐒"
        }
}

class HomeViewModel(
    repository: PlanRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observePlans(),
        settings.insultWord,
        settings.sessionNoun,
    ) { plans, word, noun ->
        val idiotDates = plans
            .filter { (it.verdict?.effectiveScore ?: 0) > 0 }
            .map { it.plan.sessionStart }
        HomeUiState(
            insultWord = word,
            sessionNoun = noun,
            streakDays = StreakService.daysSince(
                lastIdiotDate = StreakService.lastIdiotDate(idiotDates),
                firstUseDate = settings.firstUseDate(),
            ),
            hasIdiotHistory = idiotDates.isNotEmpty(),
            activePlan = plans.firstOrNull { it.status() == PlanStatus.ACTIVE },
            awaitingVerdict = plans.filter { it.status() == PlanStatus.AWAITING_VERDICT },
            upcoming = plans.filter { it.status() == PlanStatus.PLANNED },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                HomeViewModel(app.planRepository, app.settings)
            }
        }
    }
}
