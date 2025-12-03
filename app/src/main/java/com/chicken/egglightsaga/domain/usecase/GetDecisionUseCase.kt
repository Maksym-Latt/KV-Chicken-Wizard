package com.chicken.egglightsaga.domain.usecase

import com.chicken.egglightsaga.domain.model.DecisionInput
import com.chicken.egglightsaga.domain.model.DecisionResult
import com.chicken.egglightsaga.domain.repository.DecisionRepository
import javax.inject.Inject

class GetDecisionUseCase @Inject constructor(
    private val repository: DecisionRepository
) {
    suspend operator fun invoke(input: DecisionInput): DecisionResult = repository.getDecision(input)
}
