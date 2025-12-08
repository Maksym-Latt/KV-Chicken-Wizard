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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.chicken.egglightsaga.core.Audio.AudioController
import com.chicken.egglightsaga.core.Audio.rememberAudioController
import com.chicken.egglightsaga.ui.screens.component.OutlineActionButton

enum class WinPhase { Blur, EggIn, Text, Buttons }

private fun WinPhase.isAtLeast(t: WinPhase) = this.compareTo(t) >= 0

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WinOverlay(
    visible: Boolean,
    onNextLevel: () -> Unit,
    onHome: () -> Unit
) {
    if (!visible) return

    val audioController = rememberAudioController()

    LaunchedEffect(visible) {
        if (visible) {
            audioController.playWinSound()
            audioController.vibrate(AudioController.VibrationType.Win)
        }
    }

    var phase by remember { mutableStateOf(WinPhase.Blur) }

    val tBlur  = 500L
    val tEggIn = 650L
    val tText  = 450L
    val tBtns  = 350L

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        phase = WinPhase.Blur

        delay(tBlur)
        phase = WinPhase.EggIn

        delay(tEggIn)
        phase = WinPhase.Text

        delay(tText)
        phase = WinPhase.Buttons

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
                contentDescription = "win bg",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        AnimatedVisibility(
            visible = phase.isAtLeast(WinPhase.Blur),
            enter = fadeIn(animationSpec = tween(300))
        ) {
            Image(
                painter = painterResource(R.drawable.background_blur_top),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        val pulse by rememberInfiniteTransition(label = "pulseWin").animateFloat(
            initialValue = 0.99f,
            targetValue = 1.01f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAnim"
        )

        AnimatedVisibility(
            visible = phase.isAtLeast(WinPhase.EggIn),
            enter = fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 0.92f)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.game_over_egg1),
                    contentDescription = null,
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                ,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ТЕКСТ YOU WIN!
                AnimatedVisibility(
                    visible = phase.isAtLeast(WinPhase.Text),
                    enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 4 }
                ) {
                    Text(
                        text = "YOU WIN!",
                        textAlign = TextAlign.Center,
                        color = Color(0xFFFFF7D6),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            lineHeight = 48.sp,
                            shadow = Shadow(
                                color = Color(0x66FFD36E),
                                blurRadius = 10f
                            )
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = phase.isAtLeast(WinPhase.Buttons),
                    enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.96f)
                ) {
                    OutlineActionButton(
                        label = "NEXT LEVEL",
                        onActivate = onNextLevel,
                        widthFraction = 0.6f,
                        height = 60.dp,
                        fontSize = 24.sp
                    )
                }
            }
        }

        // ======== КНОПКА ДОМУ ВВЕРХУ ========
        AnimatedVisibility(
            visible = phase.isAtLeast(WinPhase.Buttons),
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
