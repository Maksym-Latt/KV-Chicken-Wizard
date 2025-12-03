package com.chicken.egglightsaga

import android.app.Application
import com.appsflyer.AppsFlyerLib
import dagger.hilt.android.HiltAndroidApp
import com.chicken.egglightsaga.BuildConfig

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initAppsFlyer()
    }

    private fun initAppsFlyer() {
        val devKey = BuildConfig.AF_DEV_KEY

        if (devKey.isBlank()) {
            return
        }


        AppsFlyerLib.getInstance().init(
            devKey,
            null,
            this
        )

        AppsFlyerLib.getInstance().start(this)
    }
}
