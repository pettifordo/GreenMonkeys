package com.strive4it.greenmonkeys.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.strive4it.greenmonkeys.MainActivity
import com.strive4it.greenmonkeys.logic.PlanStatus
import com.strive4it.greenmonkeys.ui.detail.PlanDetailScreen
import com.strive4it.greenmonkeys.ui.editor.PlanEditorScreen
import com.strive4it.greenmonkeys.ui.home.HomeScreen

/**
 * The single nav graph. ALL debrief routes will live here so popUpTo(HOME)
 * always unwinds fully (lesson §6.4) — no second graph, ever.
 */
object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val PLAN = "plan/{planId}"
    const val COMING_SOON = "comingSoon/{what}"

    fun plan(planId: String) = "plan/$planId"
    fun comingSoon(what: String) = "comingSoon/$what"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    pendingRoute: MainActivity.PendingRoute?,
    onPendingRouteConsumed: () -> Unit,
) {
    // Notification taps land here: route to the plan once the graph is up.
    LaunchedEffect(pendingRoute) {
        val pending = pendingRoute ?: return@LaunchedEffect
        navController.navigate(Routes.plan(pending.planId)) {
            popUpTo(Routes.HOME)
        }
        onPendingRouteConsumed()
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPlanTapped = { plan ->
                    when (plan.status()) {
                        PlanStatus.ACTIVE -> navController.navigate(Routes.comingSoon("The session screen"))
                        PlanStatus.AWAITING_VERDICT -> navController.navigate(Routes.comingSoon("The morning-after debrief"))
                        else -> navController.navigate(Routes.plan(plan.plan.id))
                    }
                },
                onNewPlan = { navController.navigate(Routes.EDITOR) },
                onConfess = { navController.navigate(Routes.comingSoon("The unplanned-night confession")) },
                onPattern = { navController.navigate(Routes.comingSoon("The pattern screen")) },
                onSettings = { navController.navigate(Routes.comingSoon("Settings")) },
            )
        }
        composable(Routes.EDITOR) {
            PlanEditorScreen(onDone = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }
        composable(Routes.PLAN) { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId") ?: return@composable
            PlanDetailScreen(
                planId = planId,
                onDeleted = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.COMING_SOON) { backStackEntry ->
            val what = backStackEntry.arguments?.getString("what") ?: "This screen"
            ComingSoonScreen(what, onBack = { navController.popBackStack() })
        }
    }
}

/** Honest placeholder for flows that land in the next build phase (brief §8). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ComingSoonScreen(what: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$what arrives in the next build.\nThe Monkeys are still packing. 🐒",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
