package com.chicken.egglightsaga.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorSchemeDark = darkColorScheme(
    primary = EggGlow,
    secondary = PurpleMystic,
    tertiary = BlueArcane,
    background = NightBg,
    surface = RuneDim,
    onPrimary = NightBg,
    onBackground = EggGlow
)

@Composable
fun ChickenWizardEgglightSagaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorSchemeDark,
        typography = GrimoireTypography,
        content = content
    )
}