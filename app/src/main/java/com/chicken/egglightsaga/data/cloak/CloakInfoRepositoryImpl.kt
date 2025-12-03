package com.chicken.egglightsaga.data.cloak

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.appsflyer.AppsFlyerLib
import com.chicken.egglightsaga.domain.model.CloakInfo
import com.chicken.egglightsaga.domain.repository.CloakInfoRepository
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.firebase.installations.FirebaseInstallations
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CloakInfoRepositoryImpl
@Inject
constructor(@ApplicationContext private val context: Context) : CloakInfoRepository {

    override suspend fun collectCloakInfo(): CloakInfo {
        return CloakInfo(
                isUsbDebuggingEnabled = isDeviceDebuggable(),
                advertisingId = getAdvertisingId(),
                appsflyerDeviceId = AppsFlyerLib.getInstance().getAppsFlyerUID(context),
                campaign = null,
                utmSource = null,
                utmMedium = null,
                installTime = System.currentTimeMillis().toString(),
                deepLink = null,
                deviceModel = Build.MODEL,
                deviceBrand = Build.BRAND,
                osVersion = Build.VERSION.RELEASE,
                appVersion = getVersionName(),
                isPlayStoreInstall = isInstalledFromPlayStore()
        )
    }

    private fun isDeviceDebuggable(): Boolean {
        return Settings.Secure.getInt(context.contentResolver, Settings.Secure.ADB_ENABLED, 0) == 1
    }

    private suspend fun getAdvertisingId(): String = withContext(Dispatchers.IO) {
        val googleAdId = try {
            AdvertisingIdClient.getAdvertisingIdInfo(context)?.id
        } catch (e: Exception) {
            null
        }

        if (!googleAdId.isNullOrBlank() && !googleAdId.startsWith("000000")) {
            return@withContext googleAdId
        }

        val firebaseId = try {
            FirebaseInstallations.getInstance().id.await()
        } catch (e: Exception) {
            null
        }

        if (!firebaseId.isNullOrBlank()) {
            return@withContext firebaseId
        }

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        return@withContext androidId
    }

    private fun getVersionName(): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    }

    private fun isInstalledFromPlayStore(): Boolean {
        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        return installer == "com.android.vending"
    }
}
