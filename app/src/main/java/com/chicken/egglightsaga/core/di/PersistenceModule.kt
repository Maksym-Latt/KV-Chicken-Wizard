package com.chicken.egglightsaga.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json

private val Context.gameSessionDataStore by preferencesDataStore(name = "game_session")
private val Context.spellbookDataStore by preferencesDataStore(name = "spellbook")

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    @GameSessionStore
    fun provideGameSessionDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.gameSessionDataStore

    @Provides
    @Singleton
    @SpellbookStore
    fun provideSpellbookDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.spellbookDataStore
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GameSessionStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SpellbookStore
