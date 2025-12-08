package com.chicken.egglightsaga.ui.screens.spellbook

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.chicken.egglightsaga.R
import com.chicken.egglightsaga.ui.model.RuneColor
import com.chicken.egglightsaga.ui.screens.component.OutlineActionButton
import com.chicken.egglightsaga.ui.theme.ArcaneBorderBottom
import com.chicken.egglightsaga.ui.theme.ArcaneBorderTop
import com.chicken.egglightsaga.ui.theme.ArcaneFillBottom
import com.chicken.egglightsaga.ui.theme.ArcaneFillTop
import com.chicken.egglightsaga.ui.theme.ArcaneHandleInner
import com.chicken.egglightsaga.ui.theme.ArcaneHandleOuter
import com.chicken.egglightsaga.ui.theme.ArcaneTrackBottom
import com.chicken.egglightsaga.ui.theme.ArcaneTrackTop
import com.chicken.egglightsaga.ui.theme.BlueArcane
import com.chicken.egglightsaga.ui.theme.Dimens
import com.chicken.egglightsaga.ui.theme.EggGlow
import com.chicken.egglightsaga.ui.theme.EmberBorderBottom
import com.chicken.egglightsaga.ui.theme.EmberBorderTop
import com.chicken.egglightsaga.ui.theme.EmberFillBottom
import com.chicken.egglightsaga.ui.theme.EmberFillTop
import com.chicken.egglightsaga.ui.theme.EmberHandleInner
import com.chicken.egglightsaga.ui.theme.EmberHandleOuter
import com.chicken.egglightsaga.ui.theme.EmberTrackBottom
import com.chicken.egglightsaga.ui.theme.EmberTrackTop
import com.chicken.egglightsaga.ui.theme.FrostBorderBottom
import com.chicken.egglightsaga.ui.theme.FrostBorderTop
import com.chicken.egglightsaga.ui.theme.FrostFillBottom
import com.chicken.egglightsaga.ui.theme.FrostFillTop
import com.chicken.egglightsaga.ui.theme.FrostHandleInner
import com.chicken.egglightsaga.ui.theme.FrostHandleOuter
import com.chicken.egglightsaga.ui.theme.FrostTrackBottom
import com.chicken.egglightsaga.ui.theme.FrostTrackTop
import com.chicken.egglightsaga.ui.theme.NightBg
import com.chicken.egglightsaga.ui.theme.PurpleMystic
import com.chicken.egglightsaga.core.Audio.rememberAudioController
import androidx.compose.runtime.DisposableEffect

@Composable
fun SpellbookScreen(
    showCastButton: Boolean,
    onBack: () -> Unit,
    viewModel: SpellbookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.actionMessages.collectAsState()

    val audioController = rememberAudioController()
    DisposableEffect(Unit) {
        audioController.playMenuMusic()
        onDispose { audioController.stopMenuMusic() }
    }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpellbookBackground)
    ) {

        Image(
            painter = painterResource(id = R.drawable.backgroung),
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )


        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SpellbookHeader(
                    awakeningsCompleted = uiState.awakeningsCompleted,
                    awakeningsGoal = uiState.awakeningsGoal
                )

                uiState.spells.forEach { spell ->
                    val visuals = spellVisuals(spell.definition.color)
                    SpellCard(
                        spell = spell,
                        visuals = visuals,
                        showCastButton = showCastButton,
                        onCast = {
                            val wasCast = viewModel.onCastSpell(spell.definition.id)
                            if (wasCast) onBack()
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(40.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 80.dp)
                ) {
                    OutlineActionButton(
                        label = "BACK",
                        onActivate = onBack,
                        widthFraction = 0.6f,   // 60% ширини екрана
                        height = 60.dp,         // вище
                        fontSize = 24.sp        // більший текст
                    )
                }
            }
        }
    }
}
@Composable
private fun SpellbookHeader(
    awakeningsCompleted: Int,
    awakeningsGoal: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // центр по горизонталі
        modifier = Modifier.fillMaxWidth()
    ) {
        // === Назва ===
        Text(
            text = "SPELLBOOK",
            color = EggGlow,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                shadow = Shadow(
                    color = EggGlow.copy(alpha = 0.4f),
                    blurRadius = 18f
                )
            )
        )

        // === Прогрес енергії ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_diamond),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "MAGIC ENERGY",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // === Текст із градієнтом ===
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF20E3B2), // верх
                    Color(0xFF17866B)  // низ
                )
            )

            Text(
                text = "$awakeningsCompleted/$awakeningsGoal",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    brush = gradient   // ← основний ефект
                )
            )
        }
    }
}
@Composable
private fun SpellCard(
    spell: SpellState,
    visuals: SpellVisuals,
    showCastButton: Boolean,
    onCast: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // === Верхній ряд: Назва + Прогрес ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Назва зліва
                Text(
                    text = spell.definition.title.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                // === Контейнер із накладенням іконки поверх бара ===
                // стало:
                SpellProgressWithIcon(
                    progress = if (spell.isUnlocked) spell.energyProgress else spell.unlockProgress,
                    visuals = visuals,
                    colors = if (spell.isUnlocked) visuals.progressColors else visuals.progressColors.dimmed(0.6f),
                    modifier = Modifier.width(180.dp), // або wrapContentWidth(), або зроби параметром
                )
            }

            // === Текст розблокування або енергії ===
            if (!spell.isUnlocked) {
                Text(
                    text = "Unlock after ${spell.definition.awakeningsToUnlock} awakenings",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Energy ${formatEnergy(spell.currentEnergy)}/${formatEnergy(spell.definition.energyCost)}",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // === Кнопка Cast, тільки якщо можна активувати ===
            if (showCastButton && spell.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SpellCastButton(
                        isReady = spell.isActive,
                        onCast = onCast
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellProgressWithIcon(
    progress: Float,
    visuals: SpellVisuals,
    colors: SpellProgressColors,
    modifier: Modifier = Modifier,
    barHeight: Dp = 40.dp,
    iconSize: Dp = 60.dp,
    iconOverlapStart: Dp = 16.dp
) {
    // Контейнер по висоті рівний більшому з barHeight та iconSize
    val containerHeight = max(barHeight, iconSize)
    val yOffset = (barHeight - iconSize) / 3      // якщо іконка більша -> від’ємний

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
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = -iconOverlapStart, y = yOffset)
                .size(iconSize)
                .zIndex(2f)
        )
    }
}

@Composable
private fun SpellCastButton(
    isReady: Boolean,
    onCast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (isReady) "Cast" else "Charging"
    Button(
        onClick = onCast,
        enabled = isReady,
        colors = ButtonDefaults.buttonColors(
            containerColor = PurpleMystic,
            disabledContainerColor = PurpleMystic.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        modifier = modifier.height(44.dp)
    ) {
        Text(label)
    }
}

@Composable
internal fun SpellEnergyBar(
    progress: Float,
    colors: SpellProgressColors,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier.height(40.dp)) {
        val borderWidth = 6.dp.toPx()
        val outerRadius = 24.dp.toPx()
        val inset = 7.dp.toPx()

        drawRoundRect(
            brush = Brush.verticalGradient(colors.border),
            size = size,
            cornerRadius = CornerRadius(outerRadius, outerRadius),
            style = Stroke(width = borderWidth)
        )

        val trackRect = Rect(
            offset = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2)
        )
        val trackRadius = CornerRadius(outerRadius - inset, outerRadius - inset)

        drawRoundRect(
            brush = Brush.verticalGradient(colors.track),
            topLeft = trackRect.topLeft,
            size = trackRect.size,
            cornerRadius = trackRadius
        )

        if (clamped > 0f) {
            val progressWidth = trackRect.width * clamped
            val progressRect = Rect(
                offset = trackRect.topLeft,
                size = Size(progressWidth.coerceAtLeast(trackRect.height / 3f), trackRect.height)
            )

            drawRoundRect(
                brush = Brush.verticalGradient(colors.fill),
                topLeft = progressRect.topLeft,
                size = progressRect.size,
                cornerRadius = trackRadius
            )

            val bubbleRadius = trackRect.height / 2f
            val bubbleCenterX = (progressRect.left + progressWidth).coerceIn(
                trackRect.left + bubbleRadius,
                trackRect.right - bubbleRadius
            )
            val bubbleCenter = Offset(bubbleCenterX, trackRect.center.y)

            drawCircle(
                brush = Brush.radialGradient(colors.handle, center = bubbleCenter, radius = bubbleRadius * 1.25f),
                center = bubbleCenter,
                radius = bubbleRadius
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                center = bubbleCenter,
                radius = bubbleRadius * 0.45f
            )
        }
    }
}

internal data class SpellVisuals(
    @DrawableRes val iconRes: Int,
    val iconBackground: List<Color>,
    val progressColors: SpellProgressColors
)

internal data class SpellProgressColors(
    val border: List<Color>,
    val track: List<Color>,
    val fill: List<Color>,
    val handle: List<Color>
) {
    fun dimmed(alpha: Float) = SpellProgressColors(
        border = border.map { it.copy(alpha = it.alpha * alpha) },
        track = track.map { it.copy(alpha = it.alpha * alpha) },
        fill = fill.map { it.copy(alpha = it.alpha * alpha) },
        handle = handle.map { it.copy(alpha = it.alpha * alpha) }
    )
}
private fun spellVisuals(color: RuneColor): SpellVisuals = when (color) {
    RuneColor.BLUE -> SpellVisuals(
        iconRes = R.drawable.ic_ice, // ❄️ твоя иконка льда
        iconBackground = listOf(FrostFillTop, BlueArcane),
        progressColors = SpellProgressColors(
            border = listOf(FrostBorderTop, FrostBorderBottom),
            track = listOf(FrostTrackTop, FrostTrackBottom),
            fill = listOf(FrostFillTop, FrostFillBottom),
            handle = listOf(FrostHandleOuter, FrostHandleInner)
        )
    )

    RuneColor.YELLOW -> SpellVisuals(
        iconRes = R.drawable.ic_egg, // 🔥 твоя иконка огня
        iconBackground = listOf(EmberFillTop, EmberFillBottom),
        progressColors = SpellProgressColors(
            border = listOf(EmberBorderTop, EmberBorderBottom),
            track = listOf(EmberTrackTop, EmberTrackBottom),
            fill = listOf(EmberFillTop, EmberFillBottom),
            handle = listOf(EmberHandleOuter, EmberHandleInner)
        )
    )

    RuneColor.PINK -> SpellVisuals(
        iconRes = R.drawable.ic_thunder, // ⚡ твоя иконка телепорта
        iconBackground = listOf(ArcaneFillTop, ArcaneFillBottom),
        progressColors = SpellProgressColors(
            border = listOf(ArcaneBorderTop, ArcaneBorderBottom),
            track = listOf(ArcaneTrackTop, ArcaneTrackBottom),
            fill = listOf(ArcaneFillTop, ArcaneFillBottom),
            handle = listOf(ArcaneHandleOuter, ArcaneHandleInner)
        )
    )

    else -> SpellVisuals(
        iconRes = R.drawable.ic_diamond,
        iconBackground = listOf(BlueArcane, FrostFillBottom),
        progressColors = SpellProgressColors(
            border = listOf(FrostBorderTop, FrostBorderBottom),
            track = listOf(FrostTrackTop, FrostTrackBottom),
            fill = listOf(FrostFillTop, FrostFillBottom),
            handle = listOf(FrostHandleOuter, FrostHandleInner)
        )
    )
}

private val SpellbookBackground = Brush.verticalGradient(
    colors = listOf(Color(0xFF141F38), NightBg)
)

private fun formatEnergy(value: Float): String = String.format("%.2f", value)

