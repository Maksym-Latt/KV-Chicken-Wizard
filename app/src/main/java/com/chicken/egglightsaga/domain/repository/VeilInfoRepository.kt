package com.chicken.egglightsaga.domain.repository

import com.chicken.egglightsaga.domain.model.VeilInfo

interface VeilInfoRepository {
    suspend fun collectVeilInfo(): VeilInfo
}
