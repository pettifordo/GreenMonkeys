package com.strive4it.greenmonkeys.ui.morning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.logic.CharacterVoice
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class VerdictUiState(
    val roast: CharacterVoice.Roast? = null,
    val oneChange: String = "",
)

class VerdictViewModel(
    planId: String,
    repository: PlanRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerdictUiState())
    val uiState: StateFlow<VerdictUiState> = _uiState

    init {
        viewModelScope.launch {
            val plan = repository.getPlan(planId) ?: return@launch
            val verdict = plan.verdict ?: return@launch
            _uiState.value = VerdictUiState(
                roast = CharacterVoice.roast(
                    score = verdict.effectiveScore,
                    crimes = verdict.crimes,
                    brokenPromises = plan.commitments.count { it.wasBroken == true },
                    brutality = settings.brutality.first(),
                    word = settings.insultWord.first(),
                ),
                oneChange = verdict.oneChange,
            )
        }
    }

    companion object {
        fun factory(planId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                VerdictViewModel(planId, app.planRepository, app.settings)
            }
        }
    }
}

/**
 * The dedicated verdict screen: the roast, the monkey-free closer, and the
 * exit. "Finish" unwinds every entry point to Home (lesson §6.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerdictScreen(
    planId: String,
    onFinish: () -> Unit,
    viewModel: VerdictViewModel = viewModel(factory = VerdictViewModel.factory(planId)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("The verdict") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.roast?.let { roast ->
                item {
                    Text("🐒 The roast", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Text(roast.opener, style = MaterialTheme.typography.titleLarge)
                }
                items(roast.charges, key = { it }) { charge ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🚨")
                        Text(charge, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                roast.promisesLine?.let { line ->
                    item {
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    Text(
                        "And finally, monkey-free",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                item { Text(roast.closer) }

                if (state.oneChange.isNotBlank()) {
                    item {
                        Text(
                            "Your one change",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    item { Text("↪️ ${state.oneChange}") }
                }
            }

            item {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text("Finish — go face the day", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
