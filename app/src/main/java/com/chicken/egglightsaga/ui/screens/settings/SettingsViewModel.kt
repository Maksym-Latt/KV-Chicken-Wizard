package com.chicken.egglightsaga.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.settings
        .map { preferences ->
            SettingsUiState(
                musicEnabled = preferences.musicEnabled,
                soundEnabled = preferences.soundEnabled,
                vibrationEnabled = preferences.vibrationEnabled
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun setMusicEnabled(enabled: Boolean) {
        repository.setMusicEnabled(enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        repository.setSoundEnabled(enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        repository.setVibrationEnabled(enabled)
    }
}

data class SettingsUiState(
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
