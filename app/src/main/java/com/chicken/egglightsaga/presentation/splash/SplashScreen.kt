package com.chicken.egglightsaga.presentation.splash

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chicken.egglightsaga.core.Navigation.NavRoutes
import com.chicken.egglightsaga.ui.screens.splash.GameSplashScreen

private val SPLASH_MESSAGES =
        listOf("Sharpening beak…", "Heating up the coop…", "Casting egglight…")

@Composable
fun SplashScreen(navController: NavController, viewModel: SplashViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val splashMessage by remember { mutableStateOf(SPLASH_MESSAGES.random()) }

    LaunchedEffect(state) {
        when (val current = state) {
            SplashUiState.Loading -> Unit
            SplashUiState.NavigateToGame -> {
                navController.navigate(NavRoutes.GAME) {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            }
            is SplashUiState.NavigateToContent -> {
                val encodedUrl = Uri.encode(current.url)
                navController.navigate("${NavRoutes.CONTENT}?url=$encodedUrl") {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            }
        }
    }

    GameSplashScreen(message = splashMessage, durationMs = 3000L)
}
