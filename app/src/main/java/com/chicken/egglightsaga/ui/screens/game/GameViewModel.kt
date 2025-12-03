package com.chicken.egglightsaga.ui.screens.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chicken.egglightsaga.core.Audio.AudioController
import com.chicken.egglightsaga.ui.model.ColorStats
import com.chicken.egglightsaga.ui.model.DEFAULT_LEVEL_TIME_SECONDS
import com.chicken.egglightsaga.ui.model.GRID_SIZE
import com.chicken.egglightsaga.ui.model.GameUiState
import com.chicken.egglightsaga.ui.model.PendingSpellTarget
import com.chicken.egglightsaga.ui.model.RuneColor
import com.chicken.egglightsaga.ui.model.RuneTile
import com.chicken.egglightsaga.ui.model.RuneUiModel
import com.chicken.egglightsaga.ui.model.SpellNotification
import com.chicken.egglightsaga.ui.screens.game.engine.GameEngine
import com.chicken.egglightsaga.ui.screens.game.engine.GameResult
import com.chicken.egglightsaga.ui.screens.spellbook.SpellId
import com.chicken.egglightsaga.ui.screens.spellbook.SpellbookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.toMutableMap
import kotlin.math.max

private const val ENERGY_PER_CASCADE = 0.08f
private const val LEVEL_TIME_SECONDS = DEFAULT_LEVEL_TIME_SECONDS
private const val BASE_EXPERIENCE = 120
private const val EXPERIENCE_PER_LEVEL = 30
private const val BONUS_ENERGY_PER_CASCADE = 3
private const val FREEZE_DURATION_SECONDS = 15

@HiltViewModel
class GameViewModel @Inject constructor(
    private val engine: GameEngine,
    private val animator: RuneAnimator,
    private val spellbookRepository: SpellbookRepository,
    private val sessionRepository: GameSessionRepository,
    private val audioController: AudioController
) : ViewModel() {

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui

    /* ---------------- internal state ---------------- */
    private var board: MutableList<RuneTile> = mutableListOf()
    private var timerJob: Job? = null

    private var isTimerPaused: Boolean = false

    private var currentLevel: Int = 1
    private var hasResetSpellbookForRun = false
    private var teleportFirstSelection: Int? = null

    private var freezeJob: Job? = null
    private var isTimeFrozen: Boolean = false
    private var resumeAfterFreeze: Boolean = false

    private var spellMessageJob: Job? = null

    private val TAG = "ColorStats"

    /* ---------------- pause reasons core ---------------- */
    private enum class PauseReason { OVERLAY, FREEZE, BACKGROUND }
    private val pauseReasons = mutableSetOf<PauseReason>()

    private fun isActuallyPaused(): Boolean = pauseReasons.isNotEmpty()

    private fun ensureTimerState() {
        isTimerPaused = isActuallyPaused()

        if (isActuallyPaused() || _ui.value.isTimeUp || _ui.value.isLevelComplete) {
            timerJob?.cancel()
            timerJob = null
            return
        }

        if (timerJob?.isActive != true) {
            timerJob = null
        }

        if (timerJob == null) {
            startTimer(_ui.value.timeRemainingSeconds)
        }
    }

    private fun pause(reason: PauseReason) {
        if (pauseReasons.add(reason)) {
            ensureTimerState()
            persistSession()
        }
    }

    private fun resume(reason: PauseReason) {
        if (pauseReasons.remove(reason)) {
            ensureTimerState()
            persistSession()
        }
    }

    fun onBackground() = pause(PauseReason.BACKGROUND)
    fun onForeground() = resume(PauseReason.BACKGROUND)

    fun pauseTimer() = pause(PauseReason.OVERLAY)
    fun resumeTimer() = resume(PauseReason.OVERLAY)

    /* ---------------- init/restore ---------------- */
    init {
        viewModelScope.launch {
            spellbookRepository.spellCasts.collect { spellId ->
                handleSpellCast(spellId)
            }
        }

        val savedSession = sessionRepository.get()
        if (savedSession != null) {
            restoreSession(savedSession)
        } else {
            startNewGame()
        }
    }

    private fun restoreSession(session: GameSessionState) {
        board = session.board.toMutableList()
        currentLevel = session.currentLevel
        hasResetSpellbookForRun = session.hasResetSpellbookForRun
        teleportFirstSelection = session.teleportFirstSelection

        pauseReasons.clear()
        if (session.isTimerPaused) pauseReasons.add(PauseReason.BACKGROUND)
        isTimerPaused = session.isTimerPaused

        isTimeFrozen = false
        resumeAfterFreeze = false

        _ui.value = session.uiState.copy(
            runes = board.mapIndexed { index, tile -> tile.toUiModel(index) },

            spellNotification = null,
            pendingSpellTarget = null,

            isTimeFrozenUi = false
        )

        val st = _ui.value
        if (!st.isTimeUp && !st.isLevelComplete) {
            ensureTimerState()
        }
        persistSession()
    }

    /* ---------------- game lifecycle ---------------- */
    fun startNewGame() {
        timerJob?.cancel()
        timerJob = null
        if (!hasResetSpellbookForRun && currentLevel == 1) {
            spellbookRepository.resetProgress()
            hasResetSpellbookForRun = true
        }

        board = engine.generateStartGrid().toMutableList()

        // чистый старт
        pauseReasons.clear()
        isTimerPaused = false
        isTimeFrozen = false
        resumeAfterFreeze = false
        freezeJob?.cancel(); freezeJob = null
        spellMessageJob?.cancel(); spellMessageJob = null

        _ui.value = GameUiState(
            runes = board.mapIndexed { index, tile -> tile.toUiModel(index) },
            isBusy = false,
            energyProgress = 0f,
            timeRemainingSeconds = LEVEL_TIME_SECONDS,
            isTimeUp = false,
            isLevelComplete = false,
            currentLevel = currentLevel,
            experienceEarned = 0,
            bonusEnergy = 0,
            // без банера
            spellNotification = null,
            pendingSpellTarget = null,
            // без заморозки
            isTimeFrozenUi = false
        )
        ensureTimerState()
        persistSession()
    }

    /* ---------------- spells ---------------- */
    private fun handleSpellCast(spellId: SpellId) {
        if (_ui.value.pendingSpellTarget != null) return
        when (spellId) {
            SpellId.FREEZE_TIME -> castFreezeTime()
            SpellId.FIRE_EGG    -> beginRowDestructionTargeting()
            SpellId.TELEPORT    -> beginColorDestructionTargeting()
        }
    }

    private fun castFreezeTime() {
        viewModelScope.launch {
            if (_ui.value.isTimeUp || _ui.value.isLevelComplete) return@launch

            spellMessageJob?.cancel()
            freezeJob?.cancel()

            val shouldResumeAfter = !isActuallyPaused()

            pause(PauseReason.FREEZE)
            isTimeFrozen = true
            _ui.update { it.copy(isTimeFrozenUi = true) }

            val total = FREEZE_DURATION_SECONDS
            var secondsLeft = total

            freezeJob = launch {
                while (secondsLeft > 0) {
                    delay(1_000)
                    secondsLeft--

                }

                isTimeFrozen = false
                _ui.update { it.copy(isTimeFrozenUi = false) }

                resume(PauseReason.FREEZE)

                if (shouldResumeAfter) ensureTimerState()
            }
        }
    }

    private fun beginRowDestructionTargeting() {
        viewModelScope.launch {
            val state = _ui.value
            if (state.isTimeUp || state.isLevelComplete) return@launch
            if (state.isBusy) {
                showTemporarySpellNotification("Дождитесь завершения текущего хода")
                return@launch
            }

            promptSpellTargeting(
                target = PendingSpellTarget.Row,
                message = "Нажмите на ряд на игровом поле"
            )
        }
    }

    private fun beginColorDestructionTargeting() {
        viewModelScope.launch {
            val state = _ui.value
            if (state.isTimeUp || state.isLevelComplete) return@launch
            if (state.isBusy) {
                showTemporarySpellNotification("Дождитесь завершения текущего хода")
                return@launch
            }

            val availableColors = board.map { it.color }.distinct()
            if (availableColors.isEmpty()) return@launch

            promptSpellTargeting(
                target = PendingSpellTarget.Color,
                message = "Нажмите на руну нужного цвета на игровом поле"
            )
        }
    }

    fun onSpellTarget(index: Int) {
        viewModelScope.launch {
            val state = _ui.value
            val target = state.pendingSpellTarget ?: return@launch
            if (state.isBusy || state.isTimeUp || state.isLevelComplete) return@launch

            when (target) {
                PendingSpellTarget.Row -> {
                    val row = index / GRID_SIZE
                    resolveRowSelection(row)
                }
                PendingSpellTarget.Color -> {
                    val tile = board.getOrNull(index) ?: return@launch
                    resolveColorSelection(tile.color)
                }
            }
        }
    }

    private fun promptSpellTargeting(target: PendingSpellTarget, message: String) {
        spellMessageJob?.cancel(); spellMessageJob = null
        _ui.update {
            it.copy(
                pendingSpellTarget = target,
                spellNotification = SpellNotification(message = message)
            )
        }
    }

    private suspend fun resolveRowSelection(row: Int) {
        spellMessageJob?.cancel(); spellMessageJob = null

        val indices = (0 until GRID_SIZE).map { row * GRID_SIZE + it }.toSet()

        _ui.update {
            it.copy(
                isBusy = true,
                pendingSpellTarget = null,
                spellNotification = null
            )
        }

        audioController.playFireEggSound()

        val result = engine.applySpellRemoval(board, indices)
        board = result.finalGrid.toMutableList()

        applySpellResult(result, cascadesBonus = 1)

        showTemporarySpellNotification("Ряд №${row + 1} уничтожен!")
    }

    private suspend fun resolveColorSelection(color: RuneColor) {
        spellMessageJob?.cancel(); spellMessageJob = null

        val indices = board.mapIndexedNotNull { index, tile ->
            if (tile.color == color) index else null
        }.toSet()
        if (indices.isEmpty()) {
            _ui.update {
                it.copy(
                    pendingSpellTarget = null,
                    spellNotification = null
                )
            }
            return
        }

        _ui.update {
            it.copy(
                isBusy = true,
                pendingSpellTarget = null,
                spellNotification = null
            )
        }

        audioController.playLightningSound()

        val result = engine.applySpellRemoval(board, indices)
        board = result.finalGrid.toMutableList()

        applySpellResult(result, cascadesBonus = 1)

        val message = "${color.toDisplayName().replaceFirstChar { it.uppercase() }} руны исчезли!"
        showTemporarySpellNotification(message)
    }

    /* ---------------- apply results ---------------- */
    private suspend fun applySpellResult(result: GameResult, cascadesBonus: Int) {
        if (result.actions.isNotEmpty()) {
            animator.playActions(result.actions, _ui)
        }
        val cascadesForEnergy = (result.cascades + cascadesBonus).coerceAtLeast(1)
        val currentStats = _ui.value.colorStats
        val (updatedColorStats, energyGains) = updateColorStats(
            currentStats = currentStats,
            newMatches = result.colorStats,
            cascades = cascadesForEnergy
        )

        if (energyGains.isNotEmpty()) {
            spellbookRepository.addEnergy(energyGains)
        }

        _ui.update { state ->
            val newEnergy = (state.energyProgress + cascadesForEnergy * ENERGY_PER_CASCADE).coerceAtMost(1f)
            val justCompleted = !state.isLevelComplete && newEnergy >= 1f
            if (justCompleted) {
                timerJob?.cancel(); timerJob = null
                pauseReasons.clear()
                isTimerPaused = false
            }
            val bonusEnergy = if (justCompleted) {
                max(10, cascadesForEnergy * BONUS_ENERGY_PER_CASCADE + 5)
            } else state.bonusEnergy

            val experience = if (justCompleted) {
                calculateExperience(state.currentLevel, cascadesForEnergy)
            } else state.experienceEarned

            state.copy(
                runes = board.mapIndexed { index, tile -> tile.toUiModel(index) },
                isBusy = false,
                energyProgress = newEnergy,
                isLevelComplete = state.isLevelComplete || justCompleted,
                experienceEarned = experience,
                bonusEnergy = bonusEnergy,
                colorStats = updatedColorStats
            )
        }
        persistSession()
    }

    /* ---------------- notifications (для НЕ-freeze спеллов) ---------------- */
    private fun showTemporarySpellNotification(message: String, durationMillis: Long = 3_000L) {
        spellMessageJob?.cancel()
        _ui.update { it.copy(spellNotification = SpellNotification(message = message)) }
        spellMessageJob = viewModelScope.launch {
            delay(durationMillis)
            _ui.update { state ->
                if (state.spellNotification?.message == message) {
                    state.copy(spellNotification = null)
                } else state
            }
        }
    }

    /* ---------------- moves ---------------- */
    private fun RuneColor.toDisplayName(): String = when (this) {
        RuneColor.BLUE   -> "синие"
        RuneColor.GREEN  -> "зеленые"
        RuneColor.PINK   -> "розовые"
        RuneColor.GREY   -> "серые"
        RuneColor.YELLOW -> "желтые"
    }

    fun onSwap(from: Int, to: Int) {
        viewModelScope.launch {
            val state = _ui.value
            if (state.isBusy || state.isTimeUp || state.isLevelComplete || state.pendingSpellTarget != null) return@launch
            _ui.update { it.copy(isBusy = true) }

            val result = engine.processMove(from, to, board)
            animator.playActions(result.actions, _ui)

            board = result.finalGrid.toMutableList()

            val (updatedColorStats, energyGains) = updateColorStats(
                currentStats = _ui.value.colorStats,
                newMatches = result.colorStats,
                cascades = result.cascades
            )

            if (energyGains.isNotEmpty()) {
                spellbookRepository.addEnergy(energyGains)
            }

            _ui.update { st ->
                val newEnergy = (st.energyProgress + result.cascades * ENERGY_PER_CASCADE).coerceAtMost(1f)
                val justCompleted = !st.isLevelComplete && newEnergy >= 1f
                if (justCompleted) {
                    timerJob?.cancel(); timerJob = null
                    pauseReasons.clear()
                    isTimerPaused = false
                }
                val bonusEnergy = if (justCompleted) {
                    max(10, result.cascades * BONUS_ENERGY_PER_CASCADE + 5)
                } else st.bonusEnergy
                val experience = if (justCompleted) calculateExperience(st.currentLevel, result.cascades) else st.experienceEarned

                Log.d(TAG, "=== Color Statistics Update ===")
                Log.d(TAG, "Cascades: ${result.cascades}")
                Log.d(TAG, "Total energy from move: ${result.cascades * ENERGY_PER_CASCADE}")
                Log.d(TAG, "New matches by color:")
                result.colorStats.forEach { (color, count) -> Log.d(TAG, "  $color: $count tiles") }
                Log.d(TAG, "Updated color stats:")
                updatedColorStats.forEach { (color, stats) ->
                    Log.d(TAG, "  $color: ${stats.count} total tiles, ${stats.energy} energy")
                }
                Log.d(TAG, "==============================")

                st.copy(
                    runes = board.mapIndexed { index, tile -> tile.toUiModel(index) },
                    isBusy = false,
                    energyProgress = newEnergy,
                    isLevelComplete = st.isLevelComplete || justCompleted,
                    experienceEarned = experience,
                    bonusEnergy = bonusEnergy,
                    colorStats = updatedColorStats
                )
            }
            persistSession()
        }
    }

    /* ---------------- stats/energy ---------------- */
    private fun updateColorStats(
        currentStats: Map<RuneColor, ColorStats>,
        newMatches: Map<RuneColor, Int>,
        cascades: Int
    ): Pair<Map<RuneColor, ColorStats>, Map<RuneColor, Float>> {
        val totalEnergyFromMove = cascades * ENERGY_PER_CASCADE
        val totalMatchedTiles = newMatches.values.sum()

        val updatedStats = currentStats.toMutableMap()
        val energyGains = mutableMapOf<RuneColor, Float>()

        newMatches.forEach { (color, count) ->
            val currentStat = updatedStats[color] ?: ColorStats()
            val energyForThisColor = if (totalMatchedTiles > 0) {
                (count.toFloat() / totalMatchedTiles) * totalEnergyFromMove
            } else 0f

            updatedStats[color] = ColorStats(
                count = currentStat.count + count,
                energy = currentStat.energy + energyForThisColor
            )
            if (energyForThisColor > 0f) energyGains[color] = energyForThisColor
        }

        return updatedStats to energyGains
    }

    /* ---------------- level flow ---------------- */
    fun advanceToNextLevel() {
        currentLevel += 1
        spellbookRepository.incrementAwakenings()
        startNewGame()
    }

    fun restartLevel() {
        startNewGame()
    }

    /* ---------------- timer ---------------- */
    private fun startTimer(fromSeconds: Int) {
        if (fromSeconds <= 0) {
            _ui.update { it.copy(timeRemainingSeconds = 0, isTimeUp = true) }
            timerJob = null
            return
        }
        if (isActuallyPaused()) {
            timerJob?.cancel()
            timerJob = null
            return
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = fromSeconds
            while (remaining > 0) {
                delay(1_000)
                if (isActuallyPaused()) {
                    timerJob = null
                    return@launch
                }
                remaining -= 1
                _ui.update { it.copy(timeRemainingSeconds = remaining) }
            }
            timerJob = null
            _ui.update { it.copy(isTimeUp = true) }
        }
    }

    /* ---------------- mapping ---------------- */
    private fun RuneTile.toUiModel(index: Int): RuneUiModel = RuneUiModel(
        id = id,
        color = color,
        row = index / GRID_SIZE,
        col = index % GRID_SIZE,
        xOffset = 0f,
        yOffset = 0f,
        scale = 1f,
        alpha = 1f
    )

    private fun calculateExperience(level: Int, cascades: Int): Int {
        val cascadeBonus = cascades * 10
        return BASE_EXPERIENCE + (level - 1) * EXPERIENCE_PER_LEVEL + cascadeBonus
    }

    /* ---------------- teardown ---------------- */
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel(); timerJob = null
        freezeJob?.cancel(); freezeJob = null
        spellMessageJob?.cancel(); spellMessageJob = null
        persistSession()
    }

    private fun persistSession() {
        sessionRepository.save(
            GameSessionState(
                board = board.toList(),
                uiState = _ui.value,
                currentLevel = currentLevel,
                hasResetSpellbookForRun = hasResetSpellbookForRun,
                teleportFirstSelection = teleportFirstSelection,
                isTimerPaused = isActuallyPaused(),
            )
        )
    }
}