package com.strive4it.greenmonkeys.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.capture.VideoStore
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.data.PlanWithDetails
import com.strive4it.greenmonkeys.logic.PlanStatus
import com.strive4it.greenmonkeys.logic.StreakService
import com.strive4it.greenmonkeys.notifications.NudgeScheduler
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val insultWord: String = "idiot",
    val sessionNoun: String = "Session",
    val streakDays: Int = 0,
    val longestStreak: Int = 0,
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

    /** Mirrors the iOS 1.1 record caption on the streak card. */
    val recordCaption: String
        get() = if (streakDays >= longestStreak && streakDays > 0) {
            "🏆 Personal best — beat yesterday's you again tomorrow"
        } else {
            "Longest streak: $longestStreak days"
        }
}

class HomeViewModel(
    private val repository: PlanRepository,
    settings: SettingsRepository,
    private val scheduler: NudgeScheduler,
    private val videoStore: VideoStore,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observePlans(),
        settings.insultWord,
        settings.sessionNoun,
        settings.seedLongestStreak,
    ) { plans, word, noun, seed ->
        val idiotDates = plans
            .filter { (it.verdict?.effectiveScore ?: 0) > 0 }
            .map { it.plan.sessionStart }
        val firstUse = settings.firstUseDate()
        HomeUiState(
            insultWord = word,
            sessionNoun = noun,
            streakDays = StreakService.daysSince(
                lastIdiotDate = StreakService.lastIdiotDate(idiotDates),
                firstUseDate = firstUse,
            ),
            longestStreak = maxOf(StreakService.longestStreak(idiotDates, firstUse), seed),
            hasIdiotHistory = idiotDates.isNotEmpty(),
            activePlan = plans.firstOrNull { it.status() == PlanStatus.ACTIVE },
            awaitingVerdict = plans.filter { it.status() == PlanStatus.AWAITING_VERDICT },
            upcoming = plans.filter { it.status() == PlanStatus.PLANNED },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /**
     * Swipe-away delete/cancel (iOS 1.1 parity): alarms cancelled, video FILES
     * deleted with the plan, rows cascade. Only reachable behind the
     * status-aware confirmation dialog.
     */
    fun cancelPlan(plan: PlanWithDetails) {
        viewModelScope.launch {
            for (video in plan.videos) {
                videoStore.delete(video.fileName)
            }
            scheduler.cancel(plan)
            repository.deletePlan(plan.plan)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                HomeViewModel(app.planRepository, app.settings, app.nudgeScheduler, app.videoStore)
            }
        }
    }
}
