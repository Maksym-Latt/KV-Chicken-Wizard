package com.chicken.egglightsaga.ui.screens.spellbook

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chicken.egglightsaga.core.di.SpellbookStore
import com.chicken.egglightsaga.ui.model.RuneColor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

private const val MAX_ENERGY = 1f
private val PROGRESS_KEY = stringPreferencesKey("spellbook_progress")

@Singleton
class SpellbookRepository @Inject constructor(
    @SpellbookStore private val dataStore: DataStore<Preferences>,
    private val json: Json
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progress = MutableStateFlow(loadInitialProgress())
    val progress: StateFlow<SpellbookProgress> = _progress
    private val _spellCasts = MutableSharedFlow<SpellId>(extraBufferCapacity = 1)
    val spellCasts: SharedFlow<SpellId> = _spellCasts

    init {
        scope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is java.io.IOException) emit(emptyPreferences()) else throw exception
                }
                .map { prefs -> prefs[PROGRESS_KEY] }
                .collect { stored ->
                    val restored = stored?.let { decodeProgress(it) } ?: SpellbookProgress()
                    if (restored != _progress.value) {
                        _progress.value = restored
                    }
                }
        }
    }

    private fun loadInitialProgress(): SpellbookProgress = runBlocking {
        val preferences = dataStore.data
            .catch { exception ->
                if (exception is java.io.IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        val stored = preferences[PROGRESS_KEY]
        stored?.let { decodeProgress(it) } ?: SpellbookProgress()
    }

    private fun decodeProgress(jsonString: String): SpellbookProgress =
        runCatching { json.decodeFromString<SpellbookProgress>(jsonString) }
            .getOrElse { SpellbookProgress() }

    private fun persistProgress(progress: SpellbookProgress) {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[PROGRESS_KEY] = json.encodeToString(progress)
            }
        }
    }

    fun addEnergy(energyGains: Map<RuneColor, Float>) {
        if (energyGains.isEmpty()) return
        var updated: SpellbookProgress? = null
        _progress.update { state ->
            val updatedEnergy = state.energyByColor.toMutableMap()
            var changed = false
            energyGains.forEach { (color, gain) ->
                if (gain <= 0f) return@forEach
                val current = updatedEnergy[color] ?: 0f
                val newValue = (current + gain).coerceAtMost(MAX_ENERGY)
                if (newValue != current) {
                    updatedEnergy[color] = newValue
                    changed = true
                }
            }
            if (!changed) {
                state
            } else {
                state.copy(energyByColor = updatedEnergy).also { updated = it }
            }
        }
        updated?.let { persistProgress(it) }
    }

    fun consumeEnergy(color: RuneColor) {
        var updated: SpellbookProgress? = null
        _progress.update { state ->
            if (!state.energyByColor.containsKey(color)) return@update state
            val updatedEnergy = state.energyByColor.toMutableMap()
            updatedEnergy[color] = 0f
            state.copy(energyByColor = updatedEnergy).also { updated = it }
        }
        updated?.let { persistProgress(it) }
    }

    fun notifySpellCast(spellId: SpellId) {
        _spellCasts.tryEmit(spellId)
    }

    fun incrementAwakenings() {
        var updated: SpellbookProgress? = null
        _progress.update { state ->
            state.copy(awakeningsCompleted = state.awakeningsCompleted + 1).also { updated = it }
        }
        updated?.let { persistProgress(it) }
    }

    fun resetProgress() {
        val reset = SpellbookProgress()
        _progress.value = reset
        persistProgress(reset)
    }
}

@Serializable
data class SpellbookProgress(
    val awakeningsCompleted: Int = 0,
    val energyByColor: Map<RuneColor, Float> = emptyMap()
)
