package com.chicken.egglightsaga.domain.model

data class VeilInfo(
    val isUsbDebuggingEnabled: Boolean = false,

    val advertisingId: String? = null,
    val appsflyerDeviceId: String? = null,

    val campaign: String? = null,
    val utmSource: String? = null,
    val utmMedium: String? = null,

    val installTime: String? = null,
    val deepLink: String? = null,

    val deviceModel: String = "",
    val deviceBrand: String = "",
    val osVersion: String = "",
    val appVersion: String = "",
    val isPlayStoreInstall: Boolean = false
)
