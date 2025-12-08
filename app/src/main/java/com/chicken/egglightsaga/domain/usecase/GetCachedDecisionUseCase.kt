package com.chicken.egglightsaga.domain.usecase

import com.chicken.egglightsaga.domain.model.CachedDecision
import com.chicken.egglightsaga.domain.repository.DecisionCacheRepository
import javax.inject.Inject

class GetCachedDecisionUseCase @Inject constructor(
    private val cacheRepository: DecisionCacheRepository
) {
    suspend operator fun invoke(): CachedDecision? = cacheRepository.getCachedDecision()
}
