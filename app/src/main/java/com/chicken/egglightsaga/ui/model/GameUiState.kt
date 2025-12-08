package com.chicken.egglightsaga.ui.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class GameUiState(
    val runes: List<RuneUiModel?> = List(GRID_COUNT) { null },
    val isBusy: Boolean = false,
    val energyProgress: Float = 0f,
    val timeRemainingSeconds: Int = DEFAULT_LEVEL_TIME_SECONDS,
    val isTimeUp: Boolean = false,
    val isLevelComplete: Boolean = false,
    val currentLevel: Int = 1,
    val experienceEarned: Int = 0,
    val bonusEnergy: Int = 0,
    val colorStats: Map<RuneColor, ColorStats> = emptyMap(),
    val spellNotification: SpellNotification? = null,
    val pendingSpellTarget: PendingSpellTarget? = null,
    val isTimeFrozenUi: Boolean = false
)

@Stable
@Serializable
data class SpellNotification(
    val message: String,
    val countdownSeconds: Int? = null
)

@Stable
@Serializable
sealed interface PendingSpellTarget {
    @Serializable
    @SerialName("row")
    data object Row : PendingSpellTarget

    @Serializable
    @SerialName("color")
    data object Color : PendingSpellTarget
}