package com.chicken.egglightsaga.domain.repository

import com.chicken.egglightsaga.domain.model.CachedDecision
import com.chicken.egglightsaga.domain.model.DecisionResult

interface DecisionCacheRepository {
    suspend fun getCachedDecision(): CachedDecision?
    suspend fun saveDecision(result: DecisionResult)
    suspend fun clear()
}
