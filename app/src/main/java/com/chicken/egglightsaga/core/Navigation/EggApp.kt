package com.chicken.egglightsaga.core.Navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chicken.egglightsaga.ui.screens.game.GameScreen
import com.chicken.egglightsaga.ui.screens.mainmenu.MainMenuRoute
import com.chicken.egglightsaga.ui.screens.spellbook.SpellbookScreen

@Composable
fun EggApp(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = EggRoutes.MAIN_MENU,
        modifier = Modifier
            .fillMaxSize()
    ) {
        composable(EggRoutes.MAIN_MENU) {
            MainMenuRoute(
                onPlay = { navController.navigate(EggRoutes.GAME) },
                onSpellbook = { navController.navigate(EggRoutes.SPELLBOOK) }
            )
        }
        composable(EggRoutes.GAME) {
            GameScreen(
                onBack = { navController.popBackStack() },
                onSpellbook = {
                    navController.navigate("${EggRoutes.SPELLBOOK}?${EggRoutes.SPELLBOOK_SHOW_CAST_PARAM}=true")
                },
                onGameOver = {  }
            )
        }
        composable(
            route = EggRoutes.SPELLBOOK_ROUTE,
            arguments = listOf(
                navArgument(EggRoutes.SPELLBOOK_SHOW_CAST_PARAM) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val showCastButton = backStackEntry.arguments?.getBoolean(EggRoutes.SPELLBOOK_SHOW_CAST_PARAM) ?: false
            SpellbookScreen(
                showCastButton = showCastButton,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
