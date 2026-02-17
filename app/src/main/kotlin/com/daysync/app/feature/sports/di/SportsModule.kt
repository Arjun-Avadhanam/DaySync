package com.daysync.app.feature.sports.di

import com.daysync.app.BuildConfig
import com.daysync.app.feature.sports.data.SportsRepository
import com.daysync.app.feature.sports.data.SportsRepositoryImpl
import com.daysync.app.feature.sports.data.remote.ApiFootballKey
import com.daysync.app.feature.sports.data.remote.FootballDataApiKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SportsHttpClient

@Module
@InstallIn(SingletonComponent::class)
object SportsProviderModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @SportsHttpClient
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    @Provides
    @Singleton
    fun provideFootballDataApiKey(): FootballDataApiKey =
        FootballDataApiKey(BuildConfig.FOOTBALL_DATA_API_KEY)

    @Provides
    @Singleton
    fun provideApiFootballKey(): ApiFootballKey =
        ApiFootballKey(BuildConfig.API_FOOTBALL_KEY)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SportsBindingsModule {

    @Binds
    @Singleton
    abstract fun bindSportsRepository(impl: SportsRepositoryImpl): SportsRepository
}
