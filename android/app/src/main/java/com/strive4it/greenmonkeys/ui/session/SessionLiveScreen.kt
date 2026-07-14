package com.strive4it.greenmonkeys.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strive4it.greenmonkeys.GreenMonkeysApp
import com.strive4it.greenmonkeys.data.PlanRepository
import com.strive4it.greenmonkeys.data.PlanWithDetails
import com.strive4it.greenmonkeys.data.SessionVideoEntity
import com.strive4it.greenmonkeys.logic.CharacterVoice
import com.strive4it.greenmonkeys.logic.VideoKind
import com.strive4it.greenmonkeys.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class SessionLiveViewModel(
    private val planId: String,
    private val repository: PlanRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val plan: StateFlow<PlanWithDetails?> = repository.observePlan(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _nudgeLine = MutableStateFlow("")
    val nudgeLine: StateFlow<String> = _nudgeLine

    init {
        viewModelScope.launch {
            _nudgeLine.value = CharacterVoice.monkeySessionNudge(
                brutality = settings.brutality.first(),
                word = settings.insultWord.first(),
            )
        }
    }

    /** Keep every clip: never overwrite a recording (hard rule 3). */
    fun saveDrunkVideo(fileName: String) {
        viewModelScope.launch {
            repository.addVideo(
                SessionVideoEntity(planId = planId, kind = VideoKind.DRUNK.rawValue, fileName = fileName)
            )
        }
    }

    companion object {
        fun factory(planId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as GreenMonkeysApp
                SessionLiveViewModel(planId, app.planRepository, app.settings)
            }
        }
    }
}

/** The in-session screen: the plan, sober-you's video, one big record button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionLiveScreen(
    planId: String,
    onRecordDrunkVideo: () -> Unit,
    recordedVideoFileName: String?,
    onRecordedVideoConsumed: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onRecordOutcome: () -> Unit,
    onBack: () -> Unit,
    viewModel: SessionLiveViewModel = viewModel(factory = SessionLiveViewModel.factory(planId)),
) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val nudgeLine by viewModel.nudgeLine.collectAsStateWithLifecycle()

    LaunchedEffect(recordedVideoFileName) {
        recordedVideoFileName?.let {
            viewModel.saveDrunkVideo(it)
            onRecordedVideoConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.plan?.occasion?.ifEmpty { "Session" } ?: "Session") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
            )
        },
    ) { padding ->
        val current = plan ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("🐒 The Monkeys say", style = MaterialTheme.typography.titleMedium) }
            item { Text(nudgeLine, style = MaterialTheme.typography.titleSmall) }

            item {
                Text(
                    "The plan",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(current.commitments, key = { it.id }) { commitment ->
                Text("✅  ${commitment.displayText}")
            }
            current.video(VideoKind.PLAN)?.let { video ->
                item {
                    TextButton(onClick = { onPlayVideo(video.fileName) }) {
                        Text("▶️ Watch sober you explain the plan")
                    }
                }
            }

            item {
                Button(
                    onClick = onRecordDrunkVideo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text("Record drunk-you 🎥", style = MaterialTheme.typography.titleMedium)
                }
            }
            item {
                Text(
                    "Tomorrow's you gets to watch this. Smile!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val drunkVideos = current.videos.filter { it.kind == VideoKind.DRUNK.rawValue }
            if (drunkVideos.isNotEmpty()) {
                item {
                    Text(
                        "Already in evidence",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(drunkVideos, key = { it.id }) { video ->
                    TextButton(onClick = { onPlayVideo(video.fileName) }) {
                        Text(
                            "🎞 Recorded " + video.recordedAt.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
                        )
                    }
                }
            }

            item {
                TextButton(onClick = onRecordOutcome, modifier = Modifier.padding(top = 8.dp)) {
                    Text("🌅 Record the outcome")
                }
            }
            item {
                Text(
                    "Home early, or the morning's already here? Skip straight to the debrief.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
