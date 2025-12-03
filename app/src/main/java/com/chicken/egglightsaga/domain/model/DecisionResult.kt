package com.chicken.egglightsaga.domain.model

data class DecisionResult(
    val is_intro_completed: Boolean,
    val targetUrl: String?,
    val reason: String? = null
)
