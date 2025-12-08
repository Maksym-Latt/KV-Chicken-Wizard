package com.chicken.egglightsaga.ui.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ColorStats(
    val count: Int = 0,
    val energy: Float = 0f
)