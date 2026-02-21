package com.lifepad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifepad.app.navigation.BottomNavBar
import com.lifepad.app.navigation.LifepadNavHost
import com.lifepad.app.navigation.Screen
import com.lifepad.app.security.PinLockScreen
import com.lifepad.app.security.PinSetupScreen
import com.lifepad.app.security.SecurityManager
import com.lifepad.app.ui.theme.LifepadTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private enum class AppState {
    NEEDS_PIN_SETUP,
    LOCKED,
    UNLOCKED
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    private var appState by mutableStateOf(AppState.UNLOCKED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appState = determineAppState()
        enableEdgeToEdge()
        setContent {
            LifepadTheme {
                // Re-check lock state when activity resumes (e.g., returning from background)
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                securityManager.setLastBackgroundTime(System.currentTimeMillis())
                                securityManager.setNeedsLock()
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                val newState = determineAppState()
                                if (newState != AppState.UNLOCKED) {
                                    appState = newState
                                }
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                when (appState) {
                    AppState.NEEDS_PIN_SETUP -> {
                        PinSetupScreen(
                            onPinSet = {
                                securityManager.clearNeedsLock()
                                appState = AppState.UNLOCKED
                            }
                        )
                    }
                    AppState.LOCKED -> {
                        PinLockScreen(
                            onUnlocked = {
                                securityManager.clearNeedsLock()
                                appState = AppState.UNLOCKED
                            }
                        )
                    }
                    AppState.UNLOCKED -> {
                        MainContent()
                    }
                }
            }
        }
    }

    private fun determineAppState(): AppState {
        return when {
            !securityManager.isPinSet() -> AppState.NEEDS_PIN_SETUP
            securityManager.shouldLockAfterBackground() -> AppState.LOCKED
            else -> AppState.UNLOCKED
        }
    }

    @Composable
    private fun MainContent() {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        val showBottomBar = currentRoute in listOf(
            Screen.Dashboard.route,
            Screen.Notepad.route,
            Screen.Journal.route,
            Screen.Finance.route,
            Screen.Search.route
        )

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(navController = navController)
                }
            }
        ) { padding ->
            LifepadNavHost(
                navController = navController,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
