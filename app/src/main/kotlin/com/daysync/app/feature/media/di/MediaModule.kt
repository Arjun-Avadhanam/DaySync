package com.daysync.app.feature.media.di

import com.daysync.app.BuildConfig
import com.daysync.app.feature.media.data.MediaRepository
import com.daysync.app.feature.media.data.MediaRepositoryImpl
import com.daysync.app.feature.media.data.remote.GoogleBooksApiClient
import com.daysync.app.feature.media.data.remote.ItunesApiClient
import com.daysync.app.feature.media.data.remote.JikanApiClient
import com.daysync.app.feature.media.data.remote.MediaMetadataService
import com.daysync.app.feature.media.data.remote.OmdbApiClient
import com.daysync.app.feature.media.data.remote.OpenLibraryApiClient
import com.daysync.app.feature.media.data.remote.SteamApiClient
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
    fun provideOmdbApiClient(@MediaHttpClient httpClient: HttpClient): OmdbApiClient {
        return OmdbApiClient(httpClient, BuildConfig.OMDB_API_KEY)
    }

    @Provides
    @Singleton
    fun provideGoogleBooksApiClient(@MediaHttpClient httpClient: HttpClient): GoogleBooksApiClient {
        return GoogleBooksApiClient(httpClient)
    }

    @Provides
    @Singleton
    fun provideJikanApiClient(@MediaHttpClient httpClient: HttpClient): JikanApiClient {
        return JikanApiClient(httpClient)
    }

    @Provides
    @Singleton
    fun provideSteamApiClient(@MediaHttpClient httpClient: HttpClient): SteamApiClient {
        return SteamApiClient(httpClient)
    }

    @Provides
    @Singleton
    fun provideItunesApiClient(@MediaHttpClient httpClient: HttpClient): ItunesApiClient {
        return ItunesApiClient(httpClient)
    }

    @Provides
    @Singleton
    fun provideOpenLibraryApiClient(@MediaHttpClient httpClient: HttpClient): OpenLibraryApiClient {
        return OpenLibraryApiClient(httpClient)
    }

    @Provides
    @Singleton
    fun provideMediaMetadataService(
        omdbClient: OmdbApiClient,
        googleBooksClient: GoogleBooksApiClient,
        jikanClient: JikanApiClient,
        steamClient: SteamApiClient,
        itunesClient: ItunesApiClient,
        openLibraryClient: OpenLibraryApiClient,
    ): MediaMetadataService {
        return MediaMetadataService(omdbClient, googleBooksClient, jikanClient, steamClient, itunesClient, openLibraryClient)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaBindingsModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
