package com.chicken.egglightsaga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.chicken.egglightsaga.core.Navigation.EggApp
import com.chicken.egglightsaga.ui.screens.splash.SplashScreen
import com.chicken.egglightsaga.ui.theme.ChickenWizardEgglightSagaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import com.chicken.egglightsaga.core.Audio.AudioOrchestrator
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var audioOrchestrator: AudioOrchestrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChickenWizardEgglightSagaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    val splashMessage by remember { mutableStateOf(SPLASH_MESSAGES.random()) }

                    LaunchedEffect(Unit) {
                        delay(SPLASH_DURATION_MS)
                        showSplash = false
                    }

                    if (showSplash) {
                        SplashScreen(message = splashMessage, SPLASH_DURATION_MS)
                    } else {
                        EggApp()
                    }
                }
            }
        }

        hideSystemUI()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        audioOrchestrator.onAppForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioOrchestrator.onAppBackground()
    }

    override fun onPause() {
        super.onPause()
        audioOrchestrator.onAppBackground()
    }
}

private const val SPLASH_DURATION_MS = 3_000L

private val SPLASH_MESSAGES = listOf(
    "Sharpening beak…",
    "Heating up the coop…",
    "Casting egglight…"
)