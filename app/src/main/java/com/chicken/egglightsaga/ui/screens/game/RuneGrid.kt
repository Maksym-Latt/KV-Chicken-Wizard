package com.chicken.egglightsaga.ui.screens.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chicken.egglightsaga.R
import com.chicken.egglightsaga.ui.model.GRID_SIZE
import com.chicken.egglightsaga.ui.model.GameUiState
import com.chicken.egglightsaga.ui.model.RuneColor
import com.chicken.egglightsaga.ui.model.RuneUiModel
import com.chicken.egglightsaga.ui.model.SwipeDir
import kotlin.math.abs

@Composable
fun RuneGridAnimated(
    ui: GameUiState,
    onSwipe: (Int, Int) -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellSize = 64.dp
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSize.toPx() }

    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until GRID_SIZE) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until GRID_SIZE) {
                        val idx = r * GRID_SIZE + c
                        val rune = ui.runes.getOrNull(idx)
                        if (rune != null) {
                            val pendingTarget = ui.pendingSpellTarget
                            RuneCellAnimated(
                                rune = rune,
                                size = cellSize,
                                cellSizePx = cellSizePx,
                                enabled = !ui.isLevelComplete && !ui.isTimeUp && (pendingTarget != null || !ui.isBusy),
                                enableSwipe = pendingTarget == null,
                                onSwipe = { dir ->
                                    val to = neighborOf(idx, dir) ?: return@RuneCellAnimated
                                    onSwipe(idx, to)
                                },
                                onTap = if (pendingTarget != null) {
                                    { onSelect(idx) }
                                } else null
                            )
                        } else {
                            EmptyCell(size = cellSize)
                        }
                    }
                }
            }
        }
    }
}
@DrawableRes
fun RuneColor.iconRes(): Int = when (this) {
    RuneColor.BLUE   -> R.drawable.stone_blue
    RuneColor.GREEN  -> R.drawable.stone_green
    RuneColor.PINK   -> R.drawable.stone_pink
    RuneColor.GREY   -> R.drawable.stone_grey
    RuneColor.YELLOW -> R.drawable.stone_yellow
}

@Composable
private fun RuneCellAnimated(
    rune: RuneUiModel,
    size: Dp,
    cellSizePx: Float,
    enabled: Boolean,
    enableSwipe: Boolean,
    onSwipe: (SwipeDir) -> Unit,
    onTap: (() -> Unit)?
) {
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var dragEnd by remember { mutableStateOf(Offset.Zero) }
    val resId = rune.color.iconRes()
    val painter = painterResource(id = resId)

    val gestureModifier = when {
        !enabled -> Modifier
        enableSwipe -> Modifier.pointerInput(rune.id) {
            detectDragGestures(
                onDragStart = {
                    dragStart = it
                    dragEnd = it
                },
                onDragEnd = {
                    val dx = dragEnd.x - dragStart.x
                    val dy = dragEnd.y - dragStart.y
                    dominantDirection(dx, dy, thresholdPx = cellSizePx * 0.35f)?.let(onSwipe)
                },
                onDrag = { change, drag ->
                    change.consume()
                    dragEnd += drag
                }
            )
        }
        onTap != null -> Modifier.pointerInput(rune.id) {
            detectTapGestures(onTap = { onTap() })
        }
        else -> Modifier
    }

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(size)
            .padding(10.dp)
            .graphicsLayer {
                translationX = rune.xOffset * cellSizePx
                translationY = rune.yOffset * cellSizePx
                scaleX = rune.scale
                scaleY = rune.scale
                alpha = rune.alpha
            }
            .then(gestureModifier)
    )
}

@Composable
private fun EmptyCell(size: Dp) {
    Box(
        Modifier
            .size(size)
            .background(Color.Transparent, shape = RoundedCornerShape(12.dp))
    )
}

private fun neighborOf(index: Int, dir: SwipeDir): Int? {
    val r = index / GRID_SIZE
    val c = index % GRID_SIZE
    return when (dir) {
        SwipeDir.LEFT -> if (c > 0) r * GRID_SIZE + (c - 1) else null
        SwipeDir.RIGHT -> if (c < GRID_SIZE - 1) r * GRID_SIZE + (c + 1) else null
        SwipeDir.UP -> if (r > 0) (r - 1) * GRID_SIZE + c else null
        SwipeDir.DOWN -> if (r < GRID_SIZE - 1) (r + 1) * GRID_SIZE + c else null
    }
}

private fun dominantDirection(dx: Float, dy: Float, thresholdPx: Float): SwipeDir? {
    if (abs(dx) < thresholdPx && abs(dy) < thresholdPx) return null
    return if (abs(dx) > abs(dy)) {
        if (dx > 0) SwipeDir.RIGHT else SwipeDir.LEFT
    } else {
        if (dy > 0) SwipeDir.DOWN else SwipeDir.UP
    }
}
