package com.chicken.egglightsaga.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.chicken.egglightsaga.R
import com.chicken.egglightsaga.ui.screens.component.HomeButton
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.sp
import com.chicken.egglightsaga.core.Audio.AudioOrchestrator
import com.chicken.egglightsaga.core.Audio.rememberAudioController
import com.chicken.egglightsaga.ui.screens.component.OutlineActionButton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class GameOverPhase { Blur, EggIn, Crack, Text, Buttons }

private fun GameOverPhase.isAtLeast(t: GameOverPhase) = this.compareTo(t) >= 0
private fun GameOverPhase.isBefore(t: GameOverPhase) = this.compareTo(t) < 0
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameOverOverlay(
    visible: Boolean,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    if (!visible) return

    val audioController = rememberAudioController()

    LaunchedEffect(visible) {
        if (visible) {
            audioController.playLoseSound()
            audioController.vibrate(AudioOrchestrator.VibrationType.Lose)
        }
    }

    var phase by remember { mutableStateOf(GameOverPhase.Blur) }
    var eggFrame by remember { mutableStateOf(0) }

    val tBlur   = 500L
    val tEggIn  = 650L
    val tCrack  = 900L
    val tText   = 450L
    val tBtns   = 350L

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        phase = GameOverPhase.Blur
        eggFrame = 0

        delay(tBlur)
        phase = GameOverPhase.EggIn
        eggFrame = 1

        delay(tEggIn)
        phase = GameOverPhase.Crack
        eggFrame = 2

        delay(tCrack)
        eggFrame = 3

        phase = GameOverPhase.Text
        delay(tText)
        phase = GameOverPhase.Buttons
        delay(tBtns)
    }

    Box(
        Modifier
            .fillMaxSize()
            .zIndex(100f)
            .pointerInput(Unit) {
                awaitPointerEventScope { while (true) { awaitPointerEvent() } }
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0E0D16))
        ) {
            Image(
                painter = painterResource(R.drawable.game_over_background),
                contentDescription = "game over bg",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        AnimatedVisibility(
            visible = phase.isAtLeast(GameOverPhase.Blur),
            enter = fadeIn(animationSpec = tween(300))
        ) {
            Image(
                painter = painterResource(R.drawable.background_blur_top),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        val pulse by rememberInfiniteTransition("pulse").animateFloat(
            initialValue = 0.99f,
            targetValue = 1.01f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAnim"
        )

        val crackAnim = remember { Animatable(0f) }
        LaunchedEffect(eggFrame) {
            if (eggFrame == 2) {
                crackAnim.snapTo(0f)
                crackAnim.animateTo(
                    1f,
                    animationSpec = tween(durationMillis = 600, easing = LinearEasing)
                )
            }
        }
        val t = crackAnim.value
        val damp = (1f - t) * (1f - t)
        val crackDeg = if (eggFrame == 2) {
            val cycles = 2.5f
            3.5f * sin(2f * PI.toFloat() * cycles * t) * damp
        } else 0f
        val crackScale = if (eggFrame == 2) {
            1f + 0.015f * sin(PI.toFloat() * t) * (0.8f + 0.2f * cos(PI.toFloat() * t))
        } else 1f
        val crackLift = if (eggFrame == 2) 4.dp * sin(PI.toFloat() * t) * damp else 0.dp

        val eggRes = when (eggFrame) {
            1 -> R.drawable.game_over_egg1
            2 -> R.drawable.game_over_egg2
            3 -> R.drawable.game_over_egg3
            else -> 0
        }

        AnimatedVisibility(
            visible = phase.isAtLeast(GameOverPhase.EggIn) && eggRes != 0,
            enter = fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 0.92f)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(eggRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(220.dp)
                        .offset(y = crackLift)
                        .graphicsLayer {
                            val base = pulse
                            scaleX = base * crackScale
                            scaleY = base * crackScale
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            rotationZ = crackDeg
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        AnimatedVisibility(
            visible = phase.isAtLeast(GameOverPhase.Text),
            enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 4 }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 140.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "The magic fades away...",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color(0xFFFFEAC1),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 48.sp,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0x66FFAE42),
                            blurRadius = 8f
                        )
                    ),
                    modifier = Modifier
                        .width(319.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = phase.isAtLeast(GameOverPhase.Buttons),
            enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.96f)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                OutlineActionButton(
                    label = "RETRY",
                    onActivate = onRetry,
                    widthFraction = 0.6f,
                    height = 60.dp,
                    fontSize = 24.sp
                )
            }
        }
        AnimatedVisibility(
            visible = phase.isAtLeast(GameOverPhase.Buttons),
            enter = fadeIn(tween(260))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(46.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                HomeButton(onClick = onHome)
            }
        }
    }
}