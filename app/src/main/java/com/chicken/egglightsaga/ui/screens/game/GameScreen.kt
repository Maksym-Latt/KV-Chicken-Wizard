package com.chicken.egglightsaga.ui.screens.game

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.drawWithContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chicken.egglightsaga.R
import com.chicken.egglightsaga.core.Audio.rememberAudioController
import com.chicken.egglightsaga.ui.screens.mainmenu.GradientIconButton
import com.chicken.egglightsaga.ui.screens.component.PauseButton
import com.chicken.egglightsaga.ui.theme.BlueArcane
import com.chicken.egglightsaga.ui.theme.EggGlow
import com.chicken.egglightsaga.ui.theme.PurpleMystic
import com.chicken.egglightsaga.ui.screens.spellbook.SpellEnergyBar
import com.chicken.egglightsaga.ui.screens.spellbook.SpellProgressColors
import com.chicken.egglightsaga.ui.screens.spellbook.SpellVisuals
import com.chicken.egglightsaga.ui.theme.FrostFillBottom
import com.chicken.egglightsaga.ui.theme.FrostFillTop
import com.chicken.egglightsaga.ui.theme.FrostHandleInner
import com.chicken.egglightsaga.ui.theme.FrostHandleOuter
import com.chicken.egglightsaga.ui.theme.FrostTrackBottom
import com.chicken.egglightsaga.ui.theme.FrostTrackTop
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy

@Composable
fun GameScreen(
    onBack: () -> Unit,
    onSpellbook: () -> Unit,
    onGameOver: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var suppressPauseOverlay by rememberSaveable { mutableStateOf(false) }
    val isSpellTargeting = ui.pendingSpellTarget != null
    var runeGridBounds by remember { mutableStateOf<Rect?>(null) }

    val activity = LocalContext.current.findActivity()
    val audioController = rememberAudioController()

    /* ---------- Музика рівня ---------- */
    DisposableEffect(Unit) {
        audioController.playGameMusic()
        onDispose { audioController.stopGameMusic() }
    }

    /* ---------- Пауза ↔ таймер ---------- */
    LaunchedEffect(isSettingsOpen) {
        if (isSettingsOpen) {
            viewModel.pauseTimer()
        } else {
            viewModel.resumeTimer()
        }
    }

    /* ---------- Пауза при згортанні/втраті фокуса (з урахуванням навігації) ---------- */
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, suppressPauseOverlay) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onForeground()
                    if (!isSettingsOpen) {
                        viewModel.resumeTimer()
                    }
                    suppressPauseOverlay = false
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    viewModel.onBackground()
                    if (!suppressPauseOverlay) {
                        isSettingsOpen = true
                    }
                    // Дополнительно вызывать pauseTimer()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    /* ---------- Game Over → зовнішній колбек ---------- */
    LaunchedEffect(ui.isTimeUp) {
        if (ui.isTimeUp) onGameOver()
    }

    /* ---------- Системна "Назад": в грі відкриває паузу; у паузі — згортає апу ---------- */
    BackHandler {
        if (!isSettingsOpen) {
            isSettingsOpen = true
            viewModel.pauseTimer()
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    /* ---------- UI ---------- */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1) Фони
        Image(
            painter = painterResource(id = R.drawable.backgroung),
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Image(
            painter = painterResource(id = R.drawable.background_blur_top),
            contentDescription = "background_blur",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // 2) Основний контент
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout).padding(bottom = 20.dp)
        ) {
            TopBarEgg(
                level = ui.currentLevel,
                progress = ui.energyProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isFrozen = ui.isTimeFrozenUi
                val timeColor = if (isFrozen) EggGlow else Color.White
                val timeText = if (isFrozen)
                    "${ui.timeRemainingSeconds.formatAsTime()}  (frozen)"
                else
                    ui.timeRemainingSeconds.formatAsTime()

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleMedium,
                    color = timeColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )

                PauseButton(
                    onClick = { isSettingsOpen = true },
                    enabled = !isSpellTargeting,
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                RuneGridAnimated(
                    ui = ui,
                    onSwipe = { from, to -> viewModel.onSwap(from, to) },
                    onSelect = { index -> viewModel.onSpellTarget(index) },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        runeGridBounds = coordinates.boundsInRoot()
                    }
                )
            }

            AwakeningEggIndicator(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .align(Alignment.CenterHorizontally),
                isAwakened = ui.isLevelComplete,
                progress = ui.energyProgress
            )

            BottomBar(
                enabled = !isSpellTargeting,
                onSpellbook = {
                    suppressPauseOverlay = true
                    viewModel.pauseTimer()
                    onSpellbook()
                },
                onRestart = { viewModel.restartLevel() }
            )
        }

        // 3) Повноекранний BLUR з 80%
        if (ui.energyProgress >= 0.8f) {
            Image(
                painter = painterResource(R.drawable.blur),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        if (isSpellTargeting) {
            SpellTargetScrim(bounds = runeGridBounds)
        }

        // 4) FULLSCREEN GAME OVER
        GameOverOverlay(
            visible = ui.isTimeUp,
            onRetry = { viewModel.restartLevel() },
            onHome  = { viewModel.pauseTimer(); onBack() }
        )

        // 4.1) FULLSCREEN WIN
        WinOverlay(
            visible = ui.isLevelComplete,
            onNextLevel = { viewModel.advanceToNextLevel() },
            onHome = { viewModel.pauseTimer(); onBack() }
        )

        // 5) PAUSE overlay (твій із попереднього повідомлення)
        PauseOverlay(
            visible = isSettingsOpen,
            onResume = { isSettingsOpen = false },
            onMenu = {
                viewModel.pauseTimer()
                isSettingsOpen = false
                onBack()
            },
            onDismiss = { isSettingsOpen = false }
        )
    }
}



@Composable
private fun BoxScope.SpellTargetScrim(
    bounds: Rect?,
    cornerRadius: Dp = 28.dp,
) {
    val density = LocalDensity.current
    val radiusPx = remember(cornerRadius, density) { with(density) { cornerRadius.toPx() } }
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                alpha = 0.99f
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawRect(Color.Black.copy(alpha = 0.65f))
                val rect = bounds
                if (rect != null) {
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        blendMode = BlendMode.Clear
                    )
                }
            }
    )
}


@Composable
fun TopBarEgg(
    level: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPanel(
            modifier = Modifier
                .weight(1f)
                .height(88.dp),
            corner = 24.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(20.dp))
                Text(
                    text = "LEVEL $level",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.width(50.dp))
                // прогрес-полоса праворуч від тексту
                Box(Modifier.weight(1f)) {

                    val visuals = SpellVisuals(
                        iconRes = R.drawable.ic_diamond,
                        iconBackground = listOf(FrostFillTop, BlueArcane),
                        progressColors = SpellProgressColors(
                            border = listOf(
                                FrostTrackBottom,
                                FrostTrackBottom
                            ),
                            track = listOf(FrostTrackTop, FrostTrackBottom),
                            fill = listOf(FrostFillTop, FrostFillBottom),
                            handle = listOf(FrostHandleOuter, FrostHandleInner)
                        )
                    )
                    // використовуємо вже існуючий бар
                    SpellProgressWithIcon(
                        progress = progress,
                        visuals = visuals,
                        colors = visuals.progressColors,
                    )
                }
            }
        }
    }
}


@Composable
internal fun SpellProgressWithIcon(
    progress: Float,
    visuals: SpellVisuals,
    colors: SpellProgressColors,
    modifier: Modifier = Modifier,
    barHeight: Dp = 25.dp,
    iconSize: Dp = 100.dp,
    iconOverlapStart: Dp = 45.dp
) {
    // Контейнер по висоті рівний більшому з barHeight та iconSize
    val containerHeight = max(barHeight, iconSize)


    Box(
        modifier = modifier
            .height(containerHeight)
            .fillMaxWidth()
    ) {
        // Сам прогрес-бар з фіксованою висотою barHeight
        SpellEnergyBar(
            progress = progress.coerceIn(0f, 1f),
            colors = colors,
            modifier = Modifier
                .align(Alignment.Center)
                .height(barHeight)
                .fillMaxWidth()
        )

        // Іконка на старті бару, адаптивно по вертикалі
        Image(
            painter = painterResource(id = visuals.iconRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = -iconOverlapStart)
                .size(iconSize)
                .zIndex(2f)
        )
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x99382478),
                spotColor = Color(0x99382478)
            )

            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x55382478),
                spotColor = Color(0x55382478)
            )

            .background(
                color = Color(0xFF0E1535),
                shape = shape
            )

            .border(
                width = 2.dp,
                color = Color(0xFF7AFEE6),
                shape = shape
            )
            .clip(shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) { content() }
}

@Composable
fun AwakeningEggIndicator(
    modifier: Modifier = Modifier,
    isAwakened: Boolean,
    progress: Float
) {
    val clamped = progress.coerceIn(0f, 1f)

    val infinite = rememberInfiniteTransition(label = "eggPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val rockPhase by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rockPhase"
    )
    val rockActive = clamped >= 0.8f

    val rockTargetDeg = if (rockActive) rockPhase * 6f else 0f

    val rockDeg by animateFloatAsState(
        targetValue = rockTargetDeg,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
        label = "rockDeg"
    )

    val eggAlpha by animateFloatAsState(
        targetValue = when {
            clamped >= 1f -> 1f
            clamped >= 0.9f -> 0.95f
            else -> 0.7f
        },
        label = "eggAlpha"
    )

    val glowColor = when {
        clamped >= 1f && isAwakened -> Color(0xFFFFE45A)
        clamped >= 1f && !isAwakened -> Color(0xFFFF7A7A)
        clamped >= 0.9f -> EggGlow
        clamped >= 0.6f -> EggGlow.copy(alpha = 0.9f)
        else -> PurpleMystic
    }

    val auraAlpha by animateFloatAsState(
        targetValue = when {
            clamped >= 1f -> 1f
            clamped >= 0.9f -> 0.85f
            clamped >= 0.6f -> 0.7f
            else -> 0.45f
        },
        label = "auraAlpha"
    )
    val auraOffset by animateDpAsState(
        targetValue = if (clamped >= 0.9f) (-24).dp else 0.dp,
        label = "auraOffset"
    )

    val eggPainter = when {
        clamped >= 1f && isAwakened -> painterResource(R.drawable.ic_egg_gold)
        clamped >= 1f && !isAwakened -> painterResource(R.drawable.ic_egg_gold_broken)
        clamped >= 0.6f -> painterResource(R.drawable.ic_egg_gold)
        else -> painterResource(R.drawable.ic_egg_blue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .offset(y = auraOffset)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = auraAlpha * 0.4f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = auraAlpha * 0.7f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = auraAlpha
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.7f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }

        Image(
            painter = eggPainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer {
                    alpha = eggAlpha
                    scaleX = pulse
                    scaleY = pulse
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    rotationZ = rockDeg
                }
        )
    }
}


@Composable
private fun BottomBar(
    enabled: Boolean,
    onSpellbook: () -> Unit,
    onRestart: () -> Unit,
) {

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GradientIconButton(
            onClick = onSpellbook,
            iconDrawable =  R.drawable.ic_spellbook,
            enabled = enabled
        )
        Spacer(modifier = Modifier.weight(1f),)
        GradientIconButton(
            onClick = onRestart,
            iconDrawable = R.drawable.ic_restart,
            iconTint = Color(0xFFFFEAC1),
            enabled = enabled
        )
    }
}

private fun Int.formatAsTime(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}

/* ---------- Helpers ---------- */

private fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}