package com.chicken.egglightsaga.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit


private const val PREFS_NAME = "egg_settings"
private const val KEY_MUSIC = "music_enabled"
private const val KEY_SOUND = "sound_enabled"
private const val KEY_VIBRATION = "vibration_enabled"

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<SettingsPreferences> = _settings.asStateFlow()

    fun setMusicEnabled(enabled: Boolean) {
        val current = _settings.value
        if (current.musicEnabled == enabled) return
        update { it.copy(musicEnabled = enabled) }
        preferences.edit { putBoolean(KEY_MUSIC, enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        val current = _settings.value
        if (current.soundEnabled == enabled) return
        update { it.copy(soundEnabled = enabled) }
        preferences.edit { putBoolean(KEY_SOUND, enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        val current = _settings.value
        if (current.vibrationEnabled == enabled) return
        update { it.copy(vibrationEnabled = enabled) }
        preferences.edit { putBoolean(KEY_VIBRATION, enabled) }
    }

    private fun update(transform: (SettingsPreferences) -> SettingsPreferences) {
        _settings.update(transform)
    }

    private fun load(): SettingsPreferences = SettingsPreferences(
        musicEnabled = preferences.getBoolean(KEY_MUSIC, true),
        soundEnabled = preferences.getBoolean(KEY_SOUND, true),
        vibrationEnabled = preferences.getBoolean(KEY_VIBRATION, true)
    )
}

data class SettingsPreferences(
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
