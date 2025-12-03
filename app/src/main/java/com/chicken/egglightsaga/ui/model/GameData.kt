package com.chicken.egglightsaga.ui.model

import kotlinx.serialization.Serializable

const val GRID_SIZE = 5
const val GRID_COUNT = GRID_SIZE * GRID_SIZE
const val DEFAULT_LEVEL_TIME_SECONDS = 100

@Serializable
enum class RuneColor { BLUE, GREEN, PINK , GREY, YELLOW}

@Serializable
enum class SwipeDir { UP, DOWN, LEFT, RIGHT }

