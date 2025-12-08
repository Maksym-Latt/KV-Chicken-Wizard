package com.chicken.egglightsaga.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.egglightsaga.R
import com.chicken.egglightsaga.ui.theme.Dimens
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    message: String,
    durationMs: Long = 2000L
) {
    var started by remember { mutableStateOf(false) }
    var raw by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        started = true
        val steps = 60
        val frame = (durationMs / steps).coerceAtLeast(8)
        repeat(steps) {
            raw = (it + 1) / steps.toFloat()
            delay(frame)
        }
        raw = 1f
    }

    val progress by animateFloatAsState(
        targetValue = if (started) raw else 0f,
        animationSpec = tween(180, easing = LinearEasing), label = "splashProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(id = R.drawable.backgroung),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // логотип
            Image(
                painter = painterResource(id = R.drawable.game_logo),
                contentDescription = "Egglight Saga",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 32.dp)
            )

            // світящийся прогрес-бар
            GlowLoadingBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(24.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Loading…",
                color = Color(0xFF7AFFE6),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )
        }
    }
}

/**
 * Світящийся бар як на макеті:
 * - темний «трек»
 * - бірюзова заповнена область з округленням
 * - м’яке сяйво навколо (імітація glow)
 */
@Composable
fun GlowLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFF1E2631),
    fillTop: Color = Color(0xFF7AFFE6),
    fillBottom: Color = Color(0xFF3ED6BE),
    glowColor: Color = Color(0xFF7AFFE6)
) {
    val clamped = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.45f), Color.Transparent),
                        radius = 220f
                    ),
                    shape = RoundedCornerShape(percent = 50)
                )
        )

        Canvas(
            modifier = Modifier
                .matchParentSize()
        ) {
            val r = size.height / 2f
            val corner = CornerRadius(r, r)

            // трек
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = corner
            )

            // заповнення (градієнтом зверху вниз)
            if (clamped > 0f) {
                val w = (size.width * clamped).coerceAtLeast(size.height * 0.35f)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(fillTop, fillBottom)),
                    size = androidx.compose.ui.geometry.Size(w, size.height),
                    cornerRadius = corner
                )

                // легке світло по краях заповнення (хайлайт)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    size = androidx.compose.ui.geometry.Size(w, size.height),
                    cornerRadius = corner,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.height * 0.06f)
                )
            }
        }
    }
}