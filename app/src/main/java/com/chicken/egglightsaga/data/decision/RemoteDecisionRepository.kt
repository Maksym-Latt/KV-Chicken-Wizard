package com.chicken.egglightsaga.data.decision

import android.content.Context
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

class RemoteDecisionRepository @Inject constructor(
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context
) : DecisionRepository {

    companion object ApiConfig {
        /* ---------- Production API ---------- */
        const val PROD_SCHEME = "https"
        const val PROD_HOST = "wizard-eggsaga.fun"
        const val PROD_PATH = "api/v11c"

        /* ---------- Query Params ---------- */
        const val PARAM_BUNDLE_ID = "bundle_id"
        const val PARAM_ADVERTISING_ID = "advertising_id"
        const val PARAM_APPFLYER_ID = "appsflyer_device_id"

        /* ---------- App Values ---------- */
        const val BUNDLE_ID = "com.chicken.egglightsaga"
    }

    override suspend fun getDecision(input: DecisionInput): DecisionResult {
        return getProdDecision(input)
    }

    private suspend fun getProdDecision(input: DecisionInput): DecisionResult =
        withContext(Dispatchers.IO) {

            try {
                val url = buildProdUrl(input)

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val bodyString = response.body?.string().orEmpty()

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
                val urlFromServer = json.optString("url", "")

                if (urlFromServer.isBlank()) {
                    return@withContext DecisionResult(
                        is_intro_completed = true,
                        targetUrl = null,
                        reason = "No URL in server response"
                    )
                }

                return@withContext DecisionResult(
                    is_intro_completed = false,
                    targetUrl = urlFromServer,
                    reason = "Server URL"
                )

            } catch (e: Exception) {

                return@withContext DecisionResult(
                    is_intro_completed = true,
                    targetUrl = null,
                    reason = "Exception: ${e.message}"
                )
            }
        }


    private fun buildProdUrl(input: DecisionInput): String {
        val info = input.veilInfo

        return HttpUrl.Builder()
            .scheme(PROD_SCHEME)
            .host(PROD_HOST)
            .addPathSegments(PROD_PATH)
            .addQueryParameter(PARAM_BUNDLE_ID, BUNDLE_ID)
            .addQueryParameter(PARAM_ADVERTISING_ID, info.advertisingId.orEmpty())
            .addQueryParameter(PARAM_APPFLYER_ID, info.appsflyerDeviceId.orEmpty())
            .build()
            .toString()
            .also {  }
    }
}


