package com.strive4it.greenmonkeys

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.strive4it.greenmonkeys.ui.AppNavHost
import com.strive4it.greenmonkeys.ui.theme.GreenMonkeysTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "planId"
        const val EXTRA_ROUTE = "route"
        const val ROUTE_RECORD = "record"
    }

    /** A notification tap waiting for the nav graph to consume it. */
    data class PendingRoute(val planId: String, val route: String)

    private var pendingRoute by mutableStateOf<PendingRoute?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* degrade silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            GreenMonkeysTheme {
                // Surface supplies background + content colors for every screen;
                // without it, bare composables draw black-on-black (shipped bug).
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        pendingRoute = pendingRoute,
                        onPendingRouteConsumed = { pendingRoute = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        val planId = intent?.getStringExtra(EXTRA_PLAN_ID) ?: return
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: return
        pendingRoute = PendingRoute(planId, route)
    }
}
