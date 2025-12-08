package com.chicken.egglightsaga.core.Audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.chicken.egglightsaga.R
import com.chicken.egglightsaga.ui.screens.settings.SettingsRepository

@Singleton
class AudioController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SettingsRepository,
) {
    private enum class TrackSlot { MENU, GAME, SPELLBOOK }

    private val jobScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var pendingSlot: TrackSlot? = null
    private var activeSlot: TrackSlot? = null
    private var resumeAfterSuspend: Boolean = false

    private var musicToggle: Boolean = preferences.settings.value.musicEnabled
    private var effectsToggle: Boolean = preferences.settings.value.soundEnabled
    private var hapticToggle: Boolean = preferences.settings.value.vibrationEnabled

    private val lobbyStream by lazy { buildPlayer(R.raw.menu_music) }
    private val sessionStream by lazy { buildPlayer(R.raw.game_music) }

    private val fxPool: SoundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setMaxStreams(2)
        .build()

    private val preparedFx = mutableSetOf<Int>()
    private val fxWin: Int
    private val fxFail: Int
    private val fxIgnite: Int
    private val fxShock: Int

    private val haptic: Vibrator? = context.getSystemService(Vibrator::class.java)

    init {
        fxPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                preparedFx.add(sampleId)
            }
        }
        fxWin = fxPool.load(context, R.raw.win_sound, 1)
        fxFail = fxPool.load(context, R.raw.lose_sound, 1)
        fxIgnite = fxPool.load(context, R.raw.fire_egg, 1)
        fxShock = fxPool.load(context, R.raw.lightning, 1)

        jobScope.launch {
            preferences.settings.collectLatest { preferences ->
                val musicWasEnabled = musicToggle
                musicToggle = preferences.musicEnabled
                effectsToggle = preferences.soundEnabled
                hapticToggle = preferences.vibrationEnabled

                if (!musicToggle && musicWasEnabled) {
                    pauseAllMusic()
                } else if (musicToggle && !musicWasEnabled) {
                    resumeRequestedMusic()
                }
            }
        }
    }

    fun playMenuMusic() {
        pendingSlot = TrackSlot.MENU
        startTrack(TrackSlot.MENU)
    }

    fun stopMenuMusic() {
        if (pendingSlot == TrackSlot.MENU) {
            pendingSlot = null
        }
    }

    fun playSpellbookMusic() {
        pendingSlot = TrackSlot.SPELLBOOK
        startTrack(TrackSlot.SPELLBOOK)
    }

    fun stopSpellbookMusic() {
        if (pendingSlot == TrackSlot.SPELLBOOK) {
            pendingSlot = null
        }
    }

    fun playGameMusic() {
        pendingSlot = TrackSlot.GAME
        startTrack(TrackSlot.GAME)
    }

    fun stopGameMusic() {
        if (pendingSlot == TrackSlot.GAME) {
            pendingSlot = null
        }
        pauseTrack(TrackSlot.GAME)
    }

    fun playWinSound() {
        if (!effectsToggle) return
        playSound(fxWin)
    }

    fun playLoseSound() {
        if (!effectsToggle) return
        playSound(fxFail)
    }

    fun playFireEggSound() {
        if (!effectsToggle) return
        playSound(fxIgnite)
    }

    fun playLightningSound() {
        if (!effectsToggle) return
        playSound(fxShock)
    }

    fun vibrate(type: VibrationType) {
        if (!hapticToggle) return
        val vibrator = haptic ?: return
        if (!vibrator.hasVibrator()) return
        val (duration, amplitude) = when (type) {
            VibrationType.Win -> 120L to 200
            VibrationType.Lose -> 200L to 180
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun onAppBackground() {
        resumeAfterSuspend = pendingSlot != null && musicToggle
        pauseAllMusic()
    }

    fun onAppForeground() {
        if (resumeAfterSuspend) {
            resumeRequestedMusic()
        }
        resumeAfterSuspend = false
    }

    private fun startTrack(track: TrackSlot) {
        if (!musicToggle) return
        if (activeSlot == track && getPlayer(track)?.isPlaying == true) return

        when (track) {
            TrackSlot.MENU -> pauseTrack(TrackSlot.GAME)
            TrackSlot.GAME -> pauseTrack(TrackSlot.MENU)
            TrackSlot.SPELLBOOK -> pauseTrack(TrackSlot.SPELLBOOK)
        }

        val player = getPlayer(track) ?: return
        if (!player.isPlaying) {
            player.start()
        }
        activeSlot = track
    }

    private fun pauseTrack(track: TrackSlot) {
        val player = getPlayer(track) ?: return
        if (player.isPlaying) {
            player.pause()
        }
        if (activeSlot == track) {
            activeSlot = null
        }
    }

    private fun pauseAllMusic() {
        pauseTrack(TrackSlot.MENU)
        pauseTrack(TrackSlot.GAME)
        pauseTrack(TrackSlot.SPELLBOOK)
    }

    private fun resumeRequestedMusic() {
        val track = pendingSlot ?: return
        startTrack(track)
    }

    private fun playSound(soundId: Int) {
        if (soundId == 0 || soundId !in preparedFx) return
        fxPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun getPlayer(track: TrackSlot): MediaPlayer? = when (track) {
        TrackSlot.MENU,
        TrackSlot.SPELLBOOK -> lobbyStream
        TrackSlot.GAME -> sessionStream
    }

    private fun buildPlayer(@RawRes resId: Int): MediaPlayer = MediaPlayer.create(context, resId).apply {
        isLooping = true
        setVolume(0.4f, 0.4f)
    }

    fun release() {
        pauseAllMusic()
        lobbyStream.releaseSafely()
        sessionStream.releaseSafely()
        fxPool.release()
        jobScope.cancel()
    }

    private fun MediaPlayer.releaseSafely() {
        try {
            release()
        } catch (_: Exception) {
        }
    }

    enum class VibrationType { Win, Lose }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AudioControllerEntryPoint {
    fun audioController(): AudioController
}

@Composable
fun rememberAudioController(): AudioController {
    val context = LocalContext.current.applicationContext
    return remember {
        EntryPointAccessors.fromApplication(context, AudioControllerEntryPoint::class.java).audioController()
    }
}