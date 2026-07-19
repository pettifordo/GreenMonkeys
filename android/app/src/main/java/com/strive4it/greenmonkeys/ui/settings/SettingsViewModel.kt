package com.strive4it.greenmonkeys.ui.settings

import android.app.AlarmManager
import android.app.Application
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.logic.Brutality
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val insultWord: String = "idiot",
    val sessionNoun: String = "Session",
    val brutality: Brutality = Brutality.STANDARD,
    val morningAfterHour: Int = 9,
    val appLockEnabled: Boolean = true,
    val seedLongestStreak: Int = 0,
    val customPromises: List<String> = emptyList(),
    val customCrimes: List<String> = emptyList(),
    val canScheduleExactAlarms: Boolean = true,
) {
    val insultPresets: List<String> get() = SettingsRepository.insultPresets
    val sessionNounPresets: List<String> get() = SettingsRepository.sessionNounPresets
}

class SettingsViewModel(
    private val application: Application,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val canExact: Boolean = run {
        val manager = application.getSystemService(AlarmManager::class.java)
        Build.VERSION.SDK_INT < 31 || manager?.canScheduleExactAlarms() == true
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.insultWord,
        settings.sessionNoun,
        settings.brutality,
        settings.morningAfterHour,
        settings.appLockEnabled,
        settings.seedLongestStreak,
        settings.customPromises,
        settings.customCrimes,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            insultWord = values[0] as String,
            sessionNoun = values[1] as String,
            brutality = values[2] as Brutality,
            morningAfterHour = values[3] as Int,
            appLockEnabled = values[4] as Boolean,
            seedLongestStreak = values[5] as Int,
            customPromises = values[6] as List<String>,
            customCrimes = values[7] as List<String>,
            canScheduleExactAlarms = canExact,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(canScheduleExactAlarms = canExact))

    fun setInsultWord(value: String) {
        if (value.isBlank()) return
        viewModelScope.launch {
            settings.setInsultWord(value)
            // Keep the widget's wording in step without waiting for a Home visit.
            com.strive4it.greenmonkeys.widget.pushStreakWidget(application)
        }
    }

    fun setSessionNoun(value: String) {
        if (value.isBlank()) return
        viewModelScope.launch { settings.setSessionNoun(value) }
    }

    fun setBrutality(value: Brutality) = viewModelScope.launch { settings.setBrutality(value) }

    fun setMorningAfterHour(value: Int) =
        viewModelScope.launch { settings.setMorningAfterHour(value.coerceIn(5, 14)) }

    fun setAppLockEnabled(value: Boolean) =
        viewModelScope.launch { settings.setAppLockEnabled(value) }

    fun setSeedLongestStreak(value: Int) = viewModelScope.launch {
        settings.setSeedLongestStreak(value)
        // The widget's "longest" caption depends on the seed.
        com.strive4it.greenmonkeys.widget.pushStreakWidget(application)
    }

    fun removePromise(text: String) = viewModelScope.launch { settings.removeCustomPromise(text) }
    fun removeCrime(text: String) = viewModelScope.launch { settings.removeCustomCrime(text) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                SettingsViewModel(app, app.settings)
            }
        }
    }
}
