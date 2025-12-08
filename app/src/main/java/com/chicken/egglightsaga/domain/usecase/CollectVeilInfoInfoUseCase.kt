package com.chicken.egglightsaga.domain.usecase

import com.chicken.egglightsaga.domain.model.VeilInfo
import com.chicken.egglightsaga.domain.repository.VeilInfoRepository
import javax.inject.Inject

class CollectVeilInfoInfoUseCase @Inject constructor(
    private val repository: VeilInfoRepository
) {
    suspend operator fun invoke(): VeilInfo = repository.collectVeilInfo()
}
