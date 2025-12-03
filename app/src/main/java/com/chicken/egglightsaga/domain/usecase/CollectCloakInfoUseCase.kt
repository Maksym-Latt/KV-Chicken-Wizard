package com.chicken.egglightsaga.domain.usecase

import com.chicken.egglightsaga.domain.model.CloakInfo
import com.chicken.egglightsaga.domain.repository.CloakInfoRepository
import javax.inject.Inject

class CollectCloakInfoUseCase @Inject constructor(
    private val repository: CloakInfoRepository
) {
    suspend operator fun invoke(): CloakInfo = repository.collectCloakInfo()
}
