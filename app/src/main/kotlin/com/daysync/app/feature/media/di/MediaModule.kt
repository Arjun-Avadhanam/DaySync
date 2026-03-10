package com.daysync.app.feature.media.di

import com.daysync.app.BuildConfig
import com.daysync.app.feature.media.data.MediaRepository
import com.daysync.app.feature.media.data.MediaRepositoryImpl
import com.daysync.app.feature.media.data.remote.GoogleBooksApiClient
import com.daysync.app.feature.media.data.remote.MediaMetadataService
import com.daysync.app.feature.media.data.remote.RawgApiClient
import com.daysync.app.feature.media.data.remote.TmdbApiClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MediaHttpClient

@Module
@InstallIn(SingletonComponent::class)
object MediaNetworkModule {

    @Provides
    @Singleton
    @MediaHttpClient
    fun provideMediaHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    @Provides
    @Singleton
    fun provideTmdbApiClient(@MediaHttpClient httpClient: HttpClient): TmdbApiClient {
        return TmdbApiClient(httpClient, BuildConfig.TMDB_API_KEY)
    }

    @Provides
    @Singleton
    fun provideGoogleBooksApiClient(@MediaHttpClient httpClient: HttpClient): GoogleBooksApiClient {
        return GoogleBooksApiClient(httpClient)
    }

    @Provides
    @Singleton
    fun provideRawgApiClient(@MediaHttpClient httpClient: HttpClient): RawgApiClient {
        return RawgApiClient(httpClient, BuildConfig.RAWG_API_KEY)
    }

    @Provides
    @Singleton
    fun provideMediaMetadataService(
        tmdbClient: TmdbApiClient,
        googleBooksClient: GoogleBooksApiClient,
        rawgClient: RawgApiClient,
    ): MediaMetadataService {
        return MediaMetadataService(tmdbClient, googleBooksClient, rawgClient)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaBindingsModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
