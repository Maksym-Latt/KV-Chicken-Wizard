package com.chicken.egglightsaga.domain.repository

import com.chicken.egglightsaga.domain.model.DecisionInput
import com.chicken.egglightsaga.domain.model.DecisionResult

interface DecisionRepository {
    suspend fun getDecision(input: DecisionInput): DecisionResult
}
