package com.strive4it.greenmonkeys.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.strive4it.greenmonkeys.MainActivity
import com.strive4it.greenmonkeys.capture.CameraRecorderScreen
import com.strive4it.greenmonkeys.capture.VideoPlayerScreen
import com.strive4it.greenmonkeys.logic.PlanStatus
import com.strive4it.greenmonkeys.ui.detail.PlanDetailScreen
import com.strive4it.greenmonkeys.ui.editor.PlanEditorScreen
import com.strive4it.greenmonkeys.ui.home.HomeScreen
import com.strive4it.greenmonkeys.ui.morning.MorningAfterScreen
import com.strive4it.greenmonkeys.ui.morning.UnplannedDebriefScreen
import com.strive4it.greenmonkeys.ui.morning.VerdictScreen
import com.strive4it.greenmonkeys.ui.pattern.PatternScreen
import com.strive4it.greenmonkeys.ui.session.SessionLiveScreen
import com.strive4it.greenmonkeys.ui.settings.SettingsScreen

/**
 * The single nav graph. ALL debrief routes live here so popUpTo(HOME) always
 * unwinds fully (lesson §6.4) — no second graph, ever.
 */
object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val PLAN = "plan/{planId}"
    const val SESSION = "session/{planId}"
    const val MORNING = "morning/{planId}"
    const val VERDICT = "verdict/{planId}"
    const val UNPLANNED = "unplanned"
    const val PATTERN = "pattern"
    const val SETTINGS = "settings"
    const val CAMERA = "camera"
    const val PLAYER = "player/{fileName}"

    fun plan(planId: String) = "plan/$planId"
    fun session(planId: String) = "session/$planId"
    fun morning(planId: String) = "morning/$planId"
    fun verdict(planId: String) = "verdict/$planId"
    fun player(fileName: String) = "player/$fileName"
}

/** Key used to hand a recorded video's file name back to the previous screen. */
private const val RECORDED_VIDEO_KEY = "recordedVideo"

@Composable
fun AppNavHost(
    navController: NavHostController,
    pendingRoute: MainActivity.PendingRoute?,
    onPendingRouteConsumed: () -> Unit,
) {
    // Notification taps and debug-rig launches land here: route once the graph is up.
    LaunchedEffect(pendingRoute) {
        val pending = pendingRoute ?: return@LaunchedEffect
        val planId = pending.planId
        when (pending.route) {
            NudgeRoutes.SESSION -> planId?.let {
                navController.navigate(Routes.session(it)) { popUpTo(Routes.HOME) }
            }
            NudgeRoutes.MORNING -> planId?.let {
                navController.navigate(Routes.morning(it)) { popUpTo(Routes.HOME) }
            }
            NudgeRoutes.RECORD -> planId?.let {
                // Straight into the camera — drunk users don't navigate (brief §4).
                navController.navigate(Routes.session(it)) { popUpTo(Routes.HOME) }
                navController.navigate(Routes.CAMERA)
            }
            "verdict" -> planId?.let { navController.navigate(Routes.verdict(it)) }
            "editor" -> navController.navigate(Routes.EDITOR)
            "pattern" -> navController.navigate(Routes.PATTERN)
            "settings" -> navController.navigate(Routes.SETTINGS)
        }
        onPendingRouteConsumed()
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPlanTapped = { plan ->
                    when (plan.status()) {
                        PlanStatus.ACTIVE -> navController.navigate(Routes.session(plan.plan.id))
                        PlanStatus.AWAITING_VERDICT -> navController.navigate(Routes.morning(plan.plan.id))
                        else -> navController.navigate(Routes.plan(plan.plan.id))
                    }
                },
                onNewPlan = { navController.navigate(Routes.EDITOR) },
                onConfess = { navController.navigate(Routes.UNPLANNED) },
                onPattern = { navController.navigate(Routes.PATTERN) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.EDITOR) { entry ->
            PlanEditorScreen(
                onDone = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onRecordPlanVideo = { navController.navigate(Routes.CAMERA) },
                recordedVideoFileName = entry.recordedVideo(),
                onRecordedVideoConsumed = { entry.clearRecordedVideo() },
            )
        }

        composable(Routes.PLAN) { entry ->
            val planId = entry.arguments?.getString("planId") ?: return@composable
            PlanDetailScreen(
                planId = planId,
                onDeleted = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { navController.popBackStack() },
                onPlayVideo = { navController.navigate(Routes.player(it)) },
                onReliveRoast = { navController.navigate(Routes.verdict(planId)) },
                onOpenSession = { navController.navigate(Routes.session(planId)) },
                onRecordOutcome = { navController.navigate(Routes.morning(planId)) },
            )
        }

        composable(Routes.SESSION) { entry ->
            val planId = entry.arguments?.getString("planId") ?: return@composable
            SessionLiveScreen(
                planId = planId,
                onRecordDrunkVideo = { navController.navigate(Routes.CAMERA) },
                recordedVideoFileName = entry.recordedVideo(),
                onRecordedVideoConsumed = { entry.clearRecordedVideo() },
                onPlayVideo = { navController.navigate(Routes.player(it)) },
                onRecordOutcome = { navController.navigate(Routes.morning(planId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.MORNING) { entry ->
            val planId = entry.arguments?.getString("planId") ?: return@composable
            MorningAfterScreen(
                planId = planId,
                onDelivered = { navController.navigate(Routes.verdict(it)) },
                onPlayVideo = { navController.navigate(Routes.player(it)) },
                onRecordMorningVideo = { navController.navigate(Routes.CAMERA) },
                recordedVideoFileName = entry.recordedVideo(),
                onRecordedVideoConsumed = { entry.clearRecordedVideo() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.VERDICT) { entry ->
            val planId = entry.arguments?.getString("planId") ?: return@composable
            VerdictScreen(
                planId = planId,
                // Finish unwinds EVERY entry point back to Home (lesson §6.4).
                onFinish = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }

        composable(Routes.UNPLANNED) {
            UnplannedDebriefScreen(
                onStartDebrief = { planId -> navController.navigate(Routes.morning(planId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PATTERN) {
            PatternScreen(
                onPlanTapped = { navController.navigate(Routes.plan(it)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CAMERA) {
            CameraRecorderScreen(
                onRecorded = { fileName ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(RECORDED_VIDEO_KEY, fileName)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(Routes.PLAYER) { entry ->
            val fileName = entry.arguments?.getString("fileName") ?: return@composable
            VideoPlayerScreen(fileName = fileName, onClose = { navController.popBackStack() })
        }
    }
}

/** Route names a notification tap can carry (MainActivity extras). */
object NudgeRoutes {
    const val SESSION = "session"
    const val MORNING = "morning"
    const val RECORD = "record"
}

@Composable
private fun NavBackStackEntry.recordedVideo(): String? {
    val value by savedStateHandle.getStateFlow<String?>(RECORDED_VIDEO_KEY, null).collectAsState()
    return value
}

private fun NavBackStackEntry.clearRecordedVideo() {
    savedStateHandle[RECORDED_VIDEO_KEY] = null
}
