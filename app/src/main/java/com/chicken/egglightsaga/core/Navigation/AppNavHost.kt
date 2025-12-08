package com.chicken.egglightsaga.core.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chicken.egglightsaga.presentation.splash.SplashScreen
import com.chicken.egglightsaga.presentation.contentscreen.ContentScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
            navController = navController,
            startDestination = NavRoutes.SPLASH,
            modifier = modifier
    ) {
        composable(NavRoutes.SPLASH) { SplashScreen(navController) }
        composable(
                route = "${NavRoutes.CONTENT}?url={url}",
                arguments =
                        listOf(
                                navArgument("url") {
                                    type = NavType.StringType
                                    nullable = true
                                }
                        )
        ) { ContentScreen() }
        composable(NavRoutes.GAME) { EggApp() }
    }
}

object NavRoutes {
    const val SPLASH = "splash"
    const val CONTENT = "content"
    const val GAME = "game"
}
