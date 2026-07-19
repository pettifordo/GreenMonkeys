package com.strive4it.greenmonkeys

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.strive4it.greenmonkeys.debug.DemoSeeder
import com.strive4it.greenmonkeys.ui.AppNavHost
import com.strive4it.greenmonkeys.ui.lock.LockScreen
import com.strive4it.greenmonkeys.ui.theme.GreenMonkeysTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// FragmentActivity, not ComponentActivity: BiometricPrompt requires it.
class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "planId"
        const val EXTRA_ROUTE = "route"
        const val ROUTE_RECORD = "record"
    }

    /** A notification tap (or debug-rig launch) waiting for the nav graph to consume it. */
    data class PendingRoute(val planId: String?, val route: String)

    private var pendingRoute by mutableStateOf<PendingRoute?>(null)

    /** null = still reading the setting; true = locked (SPEC §5, default ON). */
    private var locked by mutableStateOf<Boolean?>(null)
    private var lockEnabled = true

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
                    LaunchedEffect(Unit) {
                        if (locked == null) {
                            val app = application as GreenMonkeysApp
                            lockEnabled = app.settings.appLockEnabled.first()
                            locked = lockEnabled
                            if (lockEnabled) authenticate()
                        }
                    }
                    when (locked) {
                        true -> LockScreen(onUnlock = ::authenticate)
                        false -> {
                            val navController = rememberNavController()
                            AppNavHost(
                                navController = navController,
                                pendingRoute = pendingRoute,
                                onPendingRouteConsumed = { pendingRoute = null },
                            )
                        }
                        null -> {} // reading the lock setting
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-lock whenever the app leaves the foreground.
        if (lockEnabled) locked = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return

        // Debug-only demo/screenshot rig, mirroring iOS ScreenshotRig.
        if (BuildConfig.DEBUG && intent.getBooleanExtra("demoData", false)) {
            val screen = intent.getStringExtra("screen")
            lifecycleScope.launch {
                val app = application as GreenMonkeysApp
                DemoSeeder.seedIfEmpty(app)
                if (screen != null) {
                    DemoSeeder.routeFor(screen, app)?.let { (planId, route) ->
                        pendingRoute = PendingRoute(planId, route)
                    }
                }
            }
            return
        }

        val planId = intent.getStringExtra(EXTRA_PLAN_ID) ?: return
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: return
        pendingRoute = PendingRoute(planId, route)
    }

    private fun authenticate() {
        // Nothing enrolled (no PIN, no biometrics): the lock cannot protect
        // anything and must not brick the app.
        val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            locked = false
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    locked = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Credential setup vanished mid-flight: fail open rather than brick.
                    // User cancellation keeps the lock screen (Unlock retries).
                    if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                        errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT
                    ) {
                        locked = false
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Green Monkeys is locked")
            .setSubtitle("Your videos stay between you and the Monkeys.")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}
