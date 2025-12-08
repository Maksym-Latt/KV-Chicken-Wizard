package com.chicken.egglightsaga.ui.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RuneUiModel(
    val id: Long,
    val color: RuneColor,
    val row: Int,
    val col: Int,
    val xOffset: Float = 0f,
    val yOffset: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f
)