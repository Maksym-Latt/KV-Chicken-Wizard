package com.chicken.egglightsaga.ui.screens.game

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.chicken.egglightsaga.core.di.GameSessionStore
import com.chicken.egglightsaga.ui.model.GameUiState
import com.chicken.egglightsaga.ui.model.RuneTile
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SESSION_KEY = stringPreferencesKey("game_session_state")

@Singleton
class GameSessionRepository @Inject constructor(
    @GameSessionStore private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var state: GameSessionState? = loadInitialState()

    private fun loadInitialState(): GameSessionState? = runBlocking {
        val preferences = dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        val stored = preferences[SESSION_KEY]
        stored?.let { decodeState(it) }
    }

    private fun decodeState(jsonString: String): GameSessionState? =
        runCatching { json.decodeFromString<GameSessionState>(jsonString) }.getOrNull()

    fun get(): GameSessionState? = state

    fun save(newState: GameSessionState?) {
        state = newState
        scope.launch {
            dataStore.edit { prefs ->
                if (newState == null) {
                    prefs.remove(SESSION_KEY)
                } else {
                    prefs[SESSION_KEY] = json.encodeToString(newState)
                }
            }
        }
    }

    fun clear() {
        save(null)
    }
}

@Serializable
data class GameSessionState(
    val board: List<RuneTile>,
    val uiState: GameUiState,
    val currentLevel: Int,
    val hasResetSpellbookForRun: Boolean,
    val teleportFirstSelection: Int?,
    val isTimerPaused: Boolean,
)