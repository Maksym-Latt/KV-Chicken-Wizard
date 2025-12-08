package com.chicken.egglightsaga.data.veilInfo

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.appsflyer.AppsFlyerLib
import com.chicken.egglightsaga.domain.model.VeilInfo
import com.chicken.egglightsaga.domain.repository.VeilInfoRepository
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.firebase.installations.FirebaseInstallations
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class VeilInfoRepositoryImpl
@Inject
constructor(@ApplicationContext private val context: Context) : VeilInfoRepository {

    override suspend fun collectVeilInfo(): VeilInfo {
        val referrerDetails = getInstallReferrer()
        val referrer = parseReferrerUrl(referrerDetails?.installReferrer)

        val utmSource = referrer["utm_source"]
        val utmMedium = referrer["utm_medium"]
        val campaign = referrer["utm_campaign"]

        return VeilInfo(
                isUsbDebuggingEnabled = isDeviceDebuggable(),
                advertisingId = getAdvertisingId(),
                appsflyerDeviceId = AppsFlyerLib.getInstance().getAppsFlyerUID(context),
                campaign = campaign,
                utmSource = utmSource,
                utmMedium = utmMedium,
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

    private suspend fun getInstallReferrer(): ReferrerDetails? = withContext(Dispatchers.IO) {
        try {
            val referrerClient = InstallReferrerClient
                .newBuilder(context)
                .build()

            val setupResult = CompletableDeferred<ReferrerDetails?>()

            referrerClient.startConnection(object : InstallReferrerStateListener {

                override fun onInstallReferrerSetupFinished(responseCode: Int) {

                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            val details = referrerClient.installReferrer
                            setupResult.complete(details)
                        }

                        else -> {
                            setupResult.complete(null)
                        }
                    }

                    try {
                        referrerClient.endConnection()
                    } catch (_: Exception) { }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    setupResult.complete(null)
                }
            })

            return@withContext setupResult.await()

        } catch (_: Exception) {
            return@withContext null
        }
    }

    private fun parseReferrerUrl(url: String?): Map<String, String> {
        if (url.isNullOrBlank()) return emptyMap()

        return url.split("&")
            .mapNotNull {
                val p = it.split("=")
                if (p.size == 2) p[0] to p[1] else null
            }
            .toMap()
    }
}
