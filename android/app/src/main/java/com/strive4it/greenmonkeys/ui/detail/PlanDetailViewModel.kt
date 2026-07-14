package com.strive4it.greenmonkeys.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.data.PlanWithDetails
import com.strive4it.greenmonkeys.notifications.NudgeScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanDetailViewModel(
    private val planId: String,
    private val repository: PlanRepository,
    private val scheduler: NudgeScheduler,
) : ViewModel() {

    val plan: StateFlow<PlanWithDetails?> = repository.observePlan(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Only reachable behind the two-stage sarcastic confirmation (hard rule 3). */
    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.getPlan(planId)?.let { current ->
                scheduler.cancel(current)
                repository.deletePlan(current.plan)
            }
            onDone()
        }
    }

    companion object {
        fun factory(planId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                PlanDetailViewModel(planId, app.planRepository, app.nudgeScheduler)
            }
        }
    }
}
