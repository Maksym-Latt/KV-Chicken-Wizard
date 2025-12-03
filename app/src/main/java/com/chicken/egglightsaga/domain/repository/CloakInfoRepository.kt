package com.chicken.egglightsaga.domain.repository

import com.chicken.egglightsaga.domain.model.CloakInfo

interface CloakInfoRepository {
    suspend fun collectCloakInfo(): CloakInfo
}
