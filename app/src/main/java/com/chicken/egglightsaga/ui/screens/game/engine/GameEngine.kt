package com.chicken.egglightsaga.ui.screens.game.engine

import com.chicken.egglightsaga.ui.model.GRID_COUNT
import com.chicken.egglightsaga.ui.model.GRID_SIZE
import com.chicken.egglightsaga.ui.model.RuneColor
import com.chicken.egglightsaga.ui.model.RuneTile
import com.chicken.egglightsaga.ui.screens.game.GameAction
import kotlin.collections.plusAssign
import kotlin.math.abs
import kotlin.random.Random

class GameEngine(
    private val random: Random = Random.Default
) {
    fun generateStartGrid(): List<RuneTile> {
        val grid = MutableList<RuneTile?>(GRID_COUNT) { null }
        for (i in 0 until GRID_COUNT) {
            var tile: RuneTile
            var tries = 0
            do {
                tile = RuneTile(id = nextId(), color = randomColor())
                grid[i] = tile
                tries++
            } while (hasMatchAtPartial(grid, i) && tries < 20)
        }
        return grid.map { requireNotNull(it) }
    }

    /**
     * Повертає послідовність дій для відтворення анімацій (swap → match → fall → refill)
     * та фінальний стан сітки після виконання каскадів.
     */
    fun processMove(from: Int, to: Int, grid: List<RuneTile>): GameResult {
        val actions = mutableListOf<GameAction>()
        val colorStats = mutableMapOf<RuneColor, Int>()

        if (!isAdjacent(from, to)) {
            actions += GameAction.BounceBack(from)
            return GameResult(actions = actions, cascades = 0, finalGrid = grid)
        }

        val swapped = grid.toMutableList().apply { swap(from, to) }
        val matches = findAllMatches(swapped)

        if (matches.isEmpty()) {
            actions += GameAction.Swap(from, to)
            actions += GameAction.BounceBack(from)
            return GameResult(actions = actions, cascades = 0, finalGrid = grid)
        }

        actions += GameAction.Swap(from, to)
        var working = swapped.toMutableList()
        var cascades = 0

        while (true) {
            val matched = findAllMatches(working)
            if (matched.isEmpty()) break

            cascades++

            val cascadeColorStats = countMatchedColors(matched, working)
            cascadeColorStats.forEach { (color, count) ->
                colorStats[color] = colorStats.getOrDefault(color, 0) + count
            }

            val matchedIndices = matched.toList().sorted()
            actions += GameAction.Match(matchedIndices)

            val tmp: MutableList<RuneTile?> = working.mapIndexed { index, rune ->
                if (index in matched) null else rune
            }.toMutableList()

            val fallDistances = computeFallDistances(tmp)
            fallDistances
                .entries
                .groupBy({ it.value }, { it.key })
                .forEach { (distance, indices) ->
                    actions += GameAction.Fall(indices = indices.sorted(), distance = distance)
                }

            fallDistances.forEach { (fromIdx, distance) ->
                val toIdx = fromIdx + GRID_SIZE * distance
                val tile = working[fromIdx]
                tmp[toIdx] = tile
                tmp[fromIdx] = null
            }

            val refillTiles = mutableListOf<GameAction.Refill.RefillRune>()
            tmp.forEachIndexed { index, tile ->
                if (tile == null) {
                    val newTile = RuneTile(id = nextId(), color = randomColorAvoiding(tmp, index))
                    tmp[index] = newTile
                    refillTiles += GameAction.Refill.RefillRune(index = index, tile = newTile)
                }
            }
            if (refillTiles.isNotEmpty()) {
                actions += GameAction.Refill(refillTiles)
            }

            working = tmp.requireNoNulls().toMutableList()
        }

        return GameResult(actions = actions, cascades = cascades, finalGrid = working, colorStats = colorStats)
    }

    fun applySpellRemoval(grid: List<RuneTile>, indicesToRemove: Set<Int>): GameResult {
        if (indicesToRemove.isEmpty()) {
            return GameResult(actions = emptyList(), cascades = 0, finalGrid = grid, colorStats = emptyMap())
        }

        val actions = mutableListOf<GameAction>()
        val colorStats = countMatchedColors(indicesToRemove, grid).toMutableMap()

        val initialRemoval = indicesToRemove.toList().sorted()
        if (initialRemoval.isNotEmpty()) {
            actions += GameAction.Match(initialRemoval)
        }

        fun settle(current: MutableList<RuneTile?>): MutableList<RuneTile> {
            val fallDistances = computeFallDistances(current)
            fallDistances
                .entries
                .groupBy({ it.value }, { it.key })
                .forEach { (distance, indices) ->
                    actions += GameAction.Fall(indices = indices.sorted(), distance = distance)
                }

            fallDistances.forEach { (fromIdx, distance) ->
                val toIdx = fromIdx + GRID_SIZE * distance
                val tile = current[fromIdx]
                current[toIdx] = tile
                current[fromIdx] = null
            }

            val refillTiles = mutableListOf<GameAction.Refill.RefillRune>()
            current.forEachIndexed { index, tile ->
                if (tile == null) {
                    val newTile = RuneTile(id = nextId(), color = randomColorAvoiding(current, index))
                    current[index] = newTile
                    refillTiles += GameAction.Refill.RefillRune(index = index, tile = newTile)
                }
            }
            if (refillTiles.isNotEmpty()) {
                actions += GameAction.Refill(refillTiles)
            }

            return current.requireNoNulls().toMutableList()
        }

        val workingWithNulls = grid.mapIndexed { index, tile ->
            if (index in indicesToRemove) null else tile
        }.toMutableList()

        var resolved = settle(workingWithNulls)
        var cascades = 0

        while (true) {
            val matches = findAllMatches(resolved)
            if (matches.isEmpty()) break

            cascades++
            val cascadeStats = countMatchedColors(matches, resolved)
            cascadeStats.forEach { (color, count) ->
                colorStats[color] = colorStats.getOrDefault(color, 0) + count
            }

            val matchedIndices = matches.toList().sorted()
            actions += GameAction.Match(matchedIndices)

            val tmp = resolved.mapIndexed { index, rune ->
                if (index in matches) null else rune
            }.toMutableList()

            resolved = settle(tmp)
        }

        return GameResult(
            actions = actions,
            cascades = cascades,
            finalGrid = resolved,
            colorStats = colorStats
        )
    }

    private fun countMatchedColors(matchedIndices: Set<Int>, grid: List<RuneTile>): Map<RuneColor, Int> {
        val colorCounts = mutableMapOf<RuneColor, Int>()
        matchedIndices.forEach { index ->
            val color = grid[index].color
            colorCounts[color] = colorCounts.getOrDefault(color, 0) + 1
        }
        return colorCounts
    }

    // ---------------------------------------------------------
    // Utility helpers
    // ---------------------------------------------------------

    private fun computeFallDistances(grid: List<RuneTile?>): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for (col in 0 until GRID_SIZE) {
            var emptyBelow = 0
            for (row in (GRID_SIZE - 1) downTo 0) {
                val idx = row * GRID_SIZE + col
                if (grid[idx] == null) {
                    emptyBelow++
                } else if (emptyBelow > 0) {
                    result[idx] = emptyBelow
                }
            }
        }
        return result
    }

    private fun randomColor(): RuneColor {
        val colors = RuneColor.values()
        return colors[random.nextInt(colors.size)]
    }

    private fun randomColorAvoiding(grid: List<RuneTile?>, index: Int): RuneColor {
        val allColors = RuneColor.values()
        var tries = 0
        var color: RuneColor

        do {
            color = allColors[random.nextInt(allColors.size)]
            tries++
        } while (wouldCreateMatch(grid, index, color) && tries < 20)

        return color
    }

    private fun wouldCreateMatch(grid: List<RuneTile?>, index: Int, color: RuneColor): Boolean {
        val r = index / GRID_SIZE
        val c = index % GRID_SIZE

        val left1 = if (c - 1 >= 0) grid[r * GRID_SIZE + (c - 1)]?.color else null
        val left2 = if (c - 2 >= 0) grid[r * GRID_SIZE + (c - 2)]?.color else null
        if (left1 == color && left2 == color) return true

        val right1 = if (c + 1 < GRID_SIZE) grid[r * GRID_SIZE + (c + 1)]?.color else null
        val right2 = if (c + 2 < GRID_SIZE) grid[r * GRID_SIZE + (c + 2)]?.color else null
        if (right1 == color && right2 == color) return true

        val up1 = if (r - 1 >= 0) grid[(r - 1) * GRID_SIZE + c]?.color else null
        val up2 = if (r - 2 >= 0) grid[(r - 2) * GRID_SIZE + c]?.color else null
        if (up1 == color && up2 == color) return true

        val down1 = if (r + 1 < GRID_SIZE) grid[(r + 1) * GRID_SIZE + c]?.color else null
        val down2 = if (r + 2 < GRID_SIZE) grid[(r + 2) * GRID_SIZE + c]?.color else null
        if (down1 == color && down2 == color) return true

        return false
    }

    private fun hasMatchAtPartial(grid: List<RuneTile?>, index: Int): Boolean {
        val color = grid[index]?.color ?: return false
        val r = index / GRID_SIZE
        val c = index % GRID_SIZE

        var run = 1
        var cc = c - 1
        while (cc >= 0 && grid[r * GRID_SIZE + cc]?.color == color) {
            run++
            cc--
        }
        cc = c + 1
        while (cc < GRID_SIZE && grid[r * GRID_SIZE + cc]?.color == color) {
            run++
            cc++
        }
        if (run >= 3) return true

        run = 1
        var rr = r - 1
        while (rr >= 0 && grid[rr * GRID_SIZE + c]?.color == color) {
            run++
            rr--
        }
        rr = r + 1
        while (rr < GRID_SIZE && grid[rr * GRID_SIZE + c]?.color == color) {
            run++
            rr++
        }
        return run >= 3
    }

    private fun findAllMatches(grid: List<RuneTile>): Set<Int> {
        val out = mutableSetOf<Int>()
        for (r in 0 until GRID_SIZE) {
            var runStart = 0
            var runColor = grid[r * GRID_SIZE].color
            var runLen = 1
            for (c in 1 until GRID_SIZE) {
                val idx = r * GRID_SIZE + c
                if (grid[idx].color == runColor) {
                    runLen++
                } else {
                    if (runLen >= 3) {
                        repeat(runLen) { k -> out += r * GRID_SIZE + (runStart + k) }
                    }
                    runStart = c
                    runColor = grid[idx].color
                    runLen = 1
                }
            }
            if (runLen >= 3) {
                repeat(runLen) { k -> out += r * GRID_SIZE + (runStart + k) }
            }
        }

        for (c in 0 until GRID_SIZE) {
            var runStart = 0
            var runColor = grid[c].color
            var runLen = 1
            for (r in 1 until GRID_SIZE) {
                val idx = r * GRID_SIZE + c
                if (grid[idx].color == runColor) {
                    runLen++
                } else {
                    if (runLen >= 3) {
                        repeat(runLen) { k -> out += (runStart + k) * GRID_SIZE + c }
                    }
                    runStart = r
                    runColor = grid[idx].color
                    runLen = 1
                }
            }
            if (runLen >= 3) {
                repeat(runLen) { k -> out += (runStart + k) * GRID_SIZE + c }
            }
        }
        return out
    }

    private fun isAdjacent(a: Int, b: Int): Boolean {
        val ar = a / GRID_SIZE
        val ac = a % GRID_SIZE
        val br = b / GRID_SIZE
        val bc = b % GRID_SIZE
        return (ar == br && abs(ac - bc) == 1) ||
            (ac == bc && abs(ar - br) == 1)
    }

    private fun <T> MutableList<T>.swap(i: Int, j: Int) {
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }

    private fun <T> List<T?>.requireNoNulls(): List<T> = this.map { requireNotNull(it) }

    private fun nextId(): Long = idCounter++
    private var idCounter = 1L
}

// ---------------------------------------------------------
// Допоміжні типи
// ---------------------------------------------------------

data class GameResult(
    val actions: List<GameAction>,
    val cascades: Int,
    val finalGrid: List<RuneTile>,
    val colorStats: Map<RuneColor, Int> = emptyMap()
)
