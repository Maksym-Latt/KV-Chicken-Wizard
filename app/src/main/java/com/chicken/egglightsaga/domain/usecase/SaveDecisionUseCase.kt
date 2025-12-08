package com.chicken.egglightsaga.domain.usecase

import com.chicken.egglightsaga.domain.model.DecisionResult
import com.chicken.egglightsaga.domain.repository.DecisionCacheRepository
import javax.inject.Inject

class SaveDecisionUseCase @Inject constructor(
    private val cacheRepository: DecisionCacheRepository
) {
    suspend operator fun invoke(result: DecisionResult) {
        cacheRepository.saveDecision(result)
    }
}
