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
    private val settingsRepository: SettingsRepository,
) {
    private enum class MusicTrack { MENU, GAME, SPELLBOOK }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var requestedTrack: MusicTrack? = null
    private var activeTrack: MusicTrack? = null
    private var shouldResumeAfterBackground: Boolean = false

    private var musicEnabled: Boolean = settingsRepository.settings.value.musicEnabled
    private var soundEnabled: Boolean = settingsRepository.settings.value.soundEnabled
    private var vibrationEnabled: Boolean = settingsRepository.settings.value.vibrationEnabled

    private val menuPlayer by lazy { buildPlayer(R.raw.menu_music) }
    private val gamePlayer by lazy { buildPlayer(R.raw.game_music) }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setMaxStreams(2)
        .build()

    private val loadedSounds = mutableSetOf<Int>()
    private val winSoundId: Int
    private val loseSoundId: Int
    private val fireEggSoundId: Int
    private val lightningSoundId: Int

    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
            }
        }
        winSoundId = soundPool.load(context, R.raw.win_sound, 1)
        loseSoundId = soundPool.load(context, R.raw.lose_sound, 1)
        fireEggSoundId = soundPool.load(context, R.raw.fire_egg, 1)
        lightningSoundId = soundPool.load(context, R.raw.lightning, 1)

        scope.launch {
            settingsRepository.settings.collectLatest { preferences ->
                val musicWasEnabled = musicEnabled
                musicEnabled = preferences.musicEnabled
                soundEnabled = preferences.soundEnabled
                vibrationEnabled = preferences.vibrationEnabled

                if (!musicEnabled && musicWasEnabled) {
                    pauseAllMusic()
                } else if (musicEnabled && !musicWasEnabled) {
                    resumeRequestedMusic()
                }
            }
        }
    }

    fun playMenuMusic() {
        requestedTrack = MusicTrack.MENU
        startTrack(MusicTrack.MENU)
    }

    fun stopMenuMusic() {
        if (requestedTrack == MusicTrack.MENU) {
            requestedTrack = null
        }
    }

    fun playSpellbookMusic() {
        requestedTrack = MusicTrack.SPELLBOOK
        startTrack(MusicTrack.SPELLBOOK)
    }

    fun stopSpellbookMusic() {
        if (requestedTrack == MusicTrack.SPELLBOOK) {
            requestedTrack = null
        }
    }

    fun playGameMusic() {
        requestedTrack = MusicTrack.GAME
        startTrack(MusicTrack.GAME)
    }

    fun stopGameMusic() {
        if (requestedTrack == MusicTrack.GAME) {
            requestedTrack = null
        }
        pauseTrack(MusicTrack.GAME)
    }

    fun playWinSound() {
        if (!soundEnabled) return
        playSound(winSoundId)
    }

    fun playLoseSound() {
        if (!soundEnabled) return
        playSound(loseSoundId)
    }

    fun playFireEggSound() {
        if (!soundEnabled) return
        playSound(fireEggSoundId)
    }

    fun playLightningSound() {
        if (!soundEnabled) return
        playSound(lightningSoundId)
    }

    fun vibrate(type: VibrationType) {
        if (!vibrationEnabled) return
        val vibrator = vibrator ?: return
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
        shouldResumeAfterBackground = requestedTrack != null && musicEnabled
        pauseAllMusic()
    }

    fun onAppForeground() {
        if (shouldResumeAfterBackground) {
            resumeRequestedMusic()
        }
        shouldResumeAfterBackground = false
    }

    private fun startTrack(track: MusicTrack) {
        if (!musicEnabled) return
        if (activeTrack == track && getPlayer(track)?.isPlaying == true) return

        when (track) {
            MusicTrack.MENU -> pauseTrack(MusicTrack.GAME)
            MusicTrack.GAME -> pauseTrack(MusicTrack.MENU)
            MusicTrack.SPELLBOOK -> pauseTrack(MusicTrack.SPELLBOOK)
        }

        val player = getPlayer(track) ?: return
        if (!player.isPlaying) {
            player.start()
        }
        activeTrack = track
    }

    private fun pauseTrack(track: MusicTrack) {
        val player = getPlayer(track) ?: return
        if (player.isPlaying) {
            player.pause()
        }
        if (activeTrack == track) {
            activeTrack = null
        }
    }

    private fun pauseAllMusic() {
        pauseTrack(MusicTrack.MENU)
        pauseTrack(MusicTrack.GAME)
        pauseTrack(MusicTrack.SPELLBOOK)
    }

    private fun resumeRequestedMusic() {
        val track = requestedTrack ?: return
        startTrack(track)
    }

    private fun playSound(soundId: Int) {
        if (soundId == 0 || soundId !in loadedSounds) return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun getPlayer(track: MusicTrack): MediaPlayer? = when (track) {
        MusicTrack.MENU,
        MusicTrack.SPELLBOOK -> menuPlayer
        MusicTrack.GAME -> gamePlayer
    }

    private fun buildPlayer(@RawRes resId: Int): MediaPlayer = MediaPlayer.create(context, resId).apply {
        isLooping = true
        setVolume(0.4f, 0.4f)
    }

    fun release() {
        pauseAllMusic()
        menuPlayer.releaseSafely()
        gamePlayer.releaseSafely()
        soundPool.release()
        scope.cancel()
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