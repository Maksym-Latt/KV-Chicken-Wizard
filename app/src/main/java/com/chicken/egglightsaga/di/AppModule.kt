package com.chicken.egglightsaga.di

import android.content.Context
import com.chicken.egglightsaga.data.cloak.CloakInfoRepositoryImpl
import com.chicken.egglightsaga.data.decision.RemoteDecisionRepository
import com.chicken.egglightsaga.data.local.DecisionCacheRepositoryImpl
import com.chicken.egglightsaga.domain.repository.CloakInfoRepository
import com.chicken.egglightsaga.domain.repository.DecisionCacheRepository
import com.chicken.egglightsaga.domain.repository.DecisionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object AppProvideModule {

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()

    @Provides
    @Singleton
    fun provideDecisionRepository(
        client: OkHttpClient,
        @ApplicationContext context: Context
    ): DecisionRepository {
        return RemoteDecisionRepository(client, context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindModule {

    @Binds
    abstract fun bindCloakInfoRepository(
        impl: CloakInfoRepositoryImpl
    ): CloakInfoRepository

    @Binds
    abstract fun bindDecisionCacheRepository(
        impl: DecisionCacheRepositoryImpl
    ): DecisionCacheRepository
}