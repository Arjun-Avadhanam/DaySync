package com.daysync.app.feature.nutrition.di

import com.daysync.app.BuildConfig
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.feature.nutrition.data.NotionMealImporter
import com.daysync.app.feature.nutrition.data.remote.NotionMealApiClient
import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.data.repository.NutritionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NotionHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NutritionProviderModule {

    @Provides
    @Singleton
    @NotionHttpClient
    fun provideNotionHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(NotionMealApiClient.jsonConfig)
            }
        }
    }

    @Provides
    @Singleton
    fun provideNotionMealApiClient(
        @NotionHttpClient httpClient: HttpClient,
    ): NotionMealApiClient {
        return NotionMealApiClient(
            httpClient = httpClient,
            apiKey = BuildConfig.NOTION_API_KEY,
            databaseId = BuildConfig.NOTION_MEAL_DATABASE_ID,
        )
    }

    @Provides
    @Singleton
    fun provideNotionMealImporter(
        apiClient: NotionMealApiClient,
        foodItemDao: FoodItemDao,
    ): NotionMealImporter {
        return NotionMealImporter(apiClient, foodItemDao)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NutritionModule {

    @Binds
    @Singleton
    abstract fun bindNutritionRepository(
        impl: NutritionRepositoryImpl,
    ): NutritionRepository
}
