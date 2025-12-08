package com.chicken.egglightsaga.ui.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RuneTile(
    val id: Long,
    val color: RuneColor
)
