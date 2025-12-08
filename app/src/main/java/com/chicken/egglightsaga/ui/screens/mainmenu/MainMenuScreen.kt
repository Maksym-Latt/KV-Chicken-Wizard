package com.chicken.egglightsaga.ui.screens.mainmenu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chicken.egglightsaga.R
import com.chicken.egglightsaga.core.Audio.rememberAudioController
import com.chicken.egglightsaga.ui.screens.component.LuminousActionButton
import com.chicken.egglightsaga.ui.screens.settings.SettingsDialog
import com.chicken.egglightsaga.ui.screens.settings.SettingsViewModel
import com.chicken.egglightsaga.ui.theme.Dimens

@Composable
fun MainMenuRoute(
    onPlay: () -> Unit,
    onSpellbook: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val audioController = rememberAudioController()

    DisposableEffect(Unit) {
        audioController.playMenuMusic()
        onDispose { audioController.stopMenuMusic() }
    }

    MainMenuScreen(
        onPlay = onPlay,
        onSpellbook = onSpellbook,
        onSettings = { showSettings = true }
    )

    if (showSettings) {
        SettingsDialog(
            state = uiState,
            onDismiss = { showSettings = false },
            onMusicToggle = viewModel::setMusicEnabled,
            onSoundToggle = viewModel::setSoundEnabled,
            onVibrationToggle = viewModel::setVibrationEnabled
        )
    }
}

@Composable
fun MainMenuScreen(
    onPlay: () -> Unit,
    onSpellbook: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // фон
        Image(
            painter = painterResource(id = R.drawable.backgroung),
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.game_logo),
                contentDescription = "Egglight Saga",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp)
            )

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
            ) {
                LuminousActionButton(
                    label = "PLAY",
                    onClick = onPlay,
                    widthFraction = 0.6f,
                    height = 60.dp,
                    fontSize = 32.sp,
                    bg = Color(0xFF35E2C5),
                    glow = Color(0x6635E2C5)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientIconButton(
                        onClick = onSpellbook,
                        iconDrawable =  R.drawable.ic_spellbook
                    )

                    GradientIconButton(
                        onClick = onSettings,
                        iconDrawable = R.drawable.ic_settings,
                        iconTint = Color(0xFFFFEAC1)
                    )
                }
            }
        }
    }
}