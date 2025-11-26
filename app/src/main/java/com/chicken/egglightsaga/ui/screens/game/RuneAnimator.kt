package com.chicken.egglightsaga.ui.screens.game

import com.chicken.egglightsaga.ui.model.GRID_SIZE
import com.chicken.egglightsaga.ui.model.GameUiState
import com.chicken.egglightsaga.ui.model.RuneUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.sin

class RuneAnimator @Inject constructor() {

    suspend fun playActions(
        actions: List<GameAction>,
        uiFlow: MutableStateFlow<GameUiState>
    ) {
        for (action in actions) {
            when (action) {
                is GameAction.Swap -> {
                    playSwap(action, uiFlow)
                    commitSwapPositions(action, uiFlow)
                    resetOffsets(uiFlow, listOf(action.from, action.to))
                }
                is GameAction.BounceBack -> {
                    playBounce(action, uiFlow)
                    resetOffsets(uiFlow, listOf(action.index))
                }
                is GameAction.Match -> {
                    playMatch(action, uiFlow)
                    clearMatched(action, uiFlow)
                }
                is GameAction.Fall -> {
                    playFall(action, uiFlow)
                    commitFallPositions(action, uiFlow)
                    resetOffsets(uiFlow, action.indices)
                }
                is GameAction.Refill -> {
                    playRefill(action, uiFlow)
                }
            }
        }
    }

    private suspend fun playSwap(action: GameAction.Swap, uiFlow: MutableStateFlow<GameUiState>) {
        val duration = 200
        val (fromRow, fromCol) = idxToRowCol(action.from)
        val (toRow, toCol) = idxToRowCol(action.to)
        val dx = (toCol - fromCol).toFloat()
        val dy = (toRow - fromRow).toFloat()

        animate(duration) { progress ->
            uiFlow.update {
                it.copy(runes = it.runes.updateAtIndices(listOf(action.from, action.to)) { index, rune ->
                    rune?.copy(
                        xOffset = if (index == action.from) dx * progress else -dx * progress,
                        yOffset = if (index == action.from) dy * progress else -dy * progress
                    )
                })
            }
        }
    }

    private suspend fun playBounce(action: GameAction.BounceBack, uiFlow: MutableStateFlow<GameUiState>) {
        val duration = 100
        animate(duration) { progress ->
            val offset = -0.15f * sin(progress * PI).toFloat()
            uiFlow.update {
                it.copy(runes = it.runes.updateAtIndices(listOf(action.index)) { _, rune ->
                    rune?.copy(yOffset = offset)
                })
            }
        }
    }

    private suspend fun playMatch(action: GameAction.Match, uiFlow: MutableStateFlow<GameUiState>) {
        val duration = 160
        animate(duration) { progress ->
            val alpha = 1f - progress
            val scale = 1f + 0.3f * (1f - progress)
            uiFlow.update {
                it.copy(runes = it.runes.updateAtIndices(action.indices) { _, rune ->
                    rune?.copy(alpha = alpha, scale = scale)
                })
            }
        }
    }

    private suspend fun playFall(action: GameAction.Fall, uiFlow: MutableStateFlow<GameUiState>) {
        val duration = 250
        val distance = action.distance.toFloat()
        animate(duration) { progress ->
            val eased = easeOutBounce(progress)
            uiFlow.update {
                it.copy(runes = it.runes.updateAtIndices(action.indices) { _, rune ->
                    rune?.copy(yOffset = distance * eased)
                })
            }
        }
    }

    private suspend fun playRefill(action: GameAction.Refill, uiFlow: MutableStateFlow<GameUiState>) {
        val replacements = action.tiles.associateBy { it.index }
        uiFlow.update {
            it.copy(runes = it.runes.mapIndexed { index, rune ->
                val replacement = replacements[index]
                if (replacement != null) {
                    RuneUiModel(
                        id = replacement.tile.id,
                        color = replacement.tile.color,
                        row = index / GRID_SIZE,
                        col = index % GRID_SIZE,
                        xOffset = 0f,
                        yOffset = -1f,
                        scale = 1f,
                        alpha = 0f
                    )
                } else rune
            })
        }

        val duration = 260
        animate(duration) { progress ->
            uiFlow.update {
                it.copy(runes = it.runes.mapIndexed { index, rune ->
                    if (index in replacements) {
                        rune?.copy(
                            yOffset = -1f * (1f - progress),
                            alpha = progress
                        )
                    } else rune
                })
            }
        }
        resetOffsets(uiFlow, replacements.keys.toList())
    }

    private fun commitSwapPositions(action: GameAction.Swap, uiFlow: MutableStateFlow<GameUiState>) {
        uiFlow.update { state ->
            val runes = state.runes.toMutableList()
            val fromRune = runes[action.from]
            val toRune = runes[action.to]
            val fromPos = idxToRowCol(action.from)
            val toPos = idxToRowCol(action.to)
            runes[action.from] = toRune?.copy(row = fromPos.first, col = fromPos.second)
            runes[action.to] = fromRune?.copy(row = toPos.first, col = toPos.second)
            state.copy(runes = runes)
        }
    }

    private fun clearMatched(action: GameAction.Match, uiFlow: MutableStateFlow<GameUiState>) {
        uiFlow.update { state ->
            val runes = state.runes.toMutableList()
            action.indices.forEach { idx -> runes[idx] = null }
            state.copy(runes = runes)
        }
    }

    private fun commitFallPositions(action: GameAction.Fall, uiFlow: MutableStateFlow<GameUiState>) {
        uiFlow.update { state ->
            val runes = state.runes.toMutableList()
            action.indices.sortedDescending().forEach { index ->
                val rune = runes[index] ?: return@forEach
                val target = index + GRID_SIZE * action.distance
                val (row, col) = idxToRowCol(target)
                runes[target] = rune.copy(
                    row = row,
                    col = col,
                    yOffset = rune.yOffset - action.distance
                )
                runes[index] = null
            }
            state.copy(runes = runes)
        }
    }

    private fun resetOffsets(uiFlow: MutableStateFlow<GameUiState>, indices: Collection<Int>) {
        uiFlow.update { state ->
            val runes = state.runes.updateAtIndices(indices) { _, rune ->
                rune?.copy(xOffset = 0f, yOffset = 0f, scale = 1f)
            }
            state.copy(runes = runes)
        }
    }

    private suspend fun animate(durationMs: Int, step: (Float) -> Unit) {
        if (durationMs <= 0) {
            step(1f)
            return
        }
        val frames = 30
        val frameDelay = durationMs / frames
        repeat(frames) { frame ->
            step(frame / frames.toFloat())
            delay(frameDelay.toLong())
        }
        step(1f)
    }

    private fun easeOutBounce(x: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> {
                val t = x - 1.5f / d1
                n1 * t * t + 0.75f
            }
            x < 2.5f / d1 -> {
                val t = x - 2.25f / d1
                n1 * t * t + 0.9375f
            }
            else -> {
                val t = x - 2.625f / d1
                n1 * t * t + 0.984375f
            }
        }
    }

    private fun idxToRowCol(index: Int): Pair<Int, Int> = index / GRID_SIZE to index % GRID_SIZE
}

private fun List<RuneUiModel?>.updateAtIndices(
    indices: Collection<Int>,
    transform: (Int, RuneUiModel?) -> RuneUiModel?
): List<RuneUiModel?> {
    if (indices.isEmpty()) return this
    val target = indices.toSet()
    return mapIndexed { index, rune ->
        if (index in target) transform(index, rune) else rune
    }
}
