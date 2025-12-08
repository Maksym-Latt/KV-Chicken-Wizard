package com.chicken.egglightsaga.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chicken.egglightsaga.domain.model.CachedDecision
import com.chicken.egglightsaga.domain.model.DecisionResult
import com.chicken.egglightsaga.domain.repository.DecisionCacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull


private val Context.decisionDataStore by preferencesDataStore(name = "decision_store")
class DecisionCacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DecisionCacheRepository {

    private val introCompletedKey = booleanPreferencesKey("intro_completed")
    private val cachedUrlKey = stringPreferencesKey("cached_url")
    private val introTimestampKey = longPreferencesKey("intro_timestamp")

    override suspend fun getCachedDecision(): CachedDecision? {
        val prefs = context.decisionDataStore.data.firstOrNull() ?: return null

        val introCompleted = prefs[introCompletedKey] ?: return null
        val url = prefs[cachedUrlKey]
        val timestamp = prefs[introTimestampKey] ?: return null

        return CachedDecision(
            is_intro_completed = introCompleted,
            cachedUrl = url,
            introTimestamp = timestamp
        )
    }


    override suspend fun saveDecision(result: DecisionResult) {
        context.decisionDataStore.edit { prefs ->
            prefs[introCompletedKey] = result.is_intro_completed

            if (!result.targetUrl.isNullOrBlank()) {
                prefs[cachedUrlKey] = result.targetUrl
            }

            prefs[introTimestampKey] = System.currentTimeMillis()
        }
    }

    override suspend fun clear() {
        context.decisionDataStore.edit { it.clear() }
    }
}
