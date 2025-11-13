package com.korinvail.egglightsaga.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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