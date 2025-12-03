package com.chicken.egglightsaga.domain.model

data class CachedDecision(
    val is_intro_completed: Boolean,
    val cachedUrl: String?,
    val introTimestamp: Long
)
