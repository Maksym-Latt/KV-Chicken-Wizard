package com.chicken.egglightsaga.core.di

import android.content.Context
import com.chicken.egglightsaga.core.Audio.AudioOrchestrator
import com.chicken.egglightsaga.ui.screens.game.engine.GameEngine
import com.chicken.egglightsaga.ui.screens.game.RuneAnimator
import com.chicken.egglightsaga.ui.screens.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object GameModule {

    @Provides
    @Singleton
    fun provideRandom(): Random = Random.Default

    @Provides
    @Singleton
    fun provideGameEngine(random: Random): GameEngine = GameEngine(random)

    @Provides
    @Singleton
    fun provideRuneAnimator(): RuneAnimator = RuneAnimator()

    @Provides
    @Singleton
    fun provideAudioController(@ApplicationContext context: Context, settingsRepository: SettingsRepository): AudioOrchestrator {
        return AudioOrchestrator(context, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }
}
