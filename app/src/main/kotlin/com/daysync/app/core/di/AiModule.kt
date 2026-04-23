package com.daysync.app.core.di

import com.daysync.app.BuildConfig
import com.daysync.app.core.ai.GeminiRestClient
import com.daysync.app.core.database.dao.DailyHealthOverrideDao
import com.daysync.app.core.database.dao.DailyMealEntryDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExerciseSessionDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.dao.HealthMetricDao
import com.daysync.app.core.database.dao.JournalEntryDao
import com.daysync.app.core.database.dao.MediaItemDao
import com.daysync.app.core.database.dao.SleepSessionDao
import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.feature.ai.data.AiRepository
import com.daysync.app.feature.ai.data.AiRepositoryImpl
import com.daysync.app.feature.ai.data.DataContextBuilder
import com.daysync.app.feature.ai.data.GeminiChatService
import com.daysync.app.feature.ai.data.GroqChatService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGeminiRestClient(@Named("gemini") httpClient: HttpClient): GeminiRestClient {
        return GeminiRestClient(
            httpClient = httpClient,
            apiKey = BuildConfig.GEMINI_API_KEY,
        )
    }

    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                requestTimeoutMillis = 60_000
            }
        }
    }

    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqHttpClient(): HttpClient {
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
    fun provideDataContextBuilder(
        healthMetricDao: HealthMetricDao,
        sleepSessionDao: SleepSessionDao,
        exerciseSessionDao: ExerciseSessionDao,
        dailyNutritionSummaryDao: DailyNutritionSummaryDao,
        dailyMealEntryDao: DailyMealEntryDao,
        foodItemDao: FoodItemDao,
        expenseDao: ExpenseDao,
        journalEntryDao: JournalEntryDao,
        mediaItemDao: MediaItemDao,
        sportEventDao: SportEventDao,
        dailyHealthOverrideDao: DailyHealthOverrideDao,
        userPreferences: com.daysync.app.core.config.UserPreferences,
    ): DataContextBuilder {
        return DataContextBuilder(
            healthMetricDao = healthMetricDao,
            sleepSessionDao = sleepSessionDao,
            exerciseSessionDao = exerciseSessionDao,
            nutritionSummaryDao = dailyNutritionSummaryDao,
            dailyMealEntryDao = dailyMealEntryDao,
            foodItemDao = foodItemDao,
            expenseDao = expenseDao,
            journalEntryDao = journalEntryDao,
            mediaItemDao = mediaItemDao,
            sportEventDao = sportEventDao,
            dailyHealthOverrideDao = dailyHealthOverrideDao,
            userPreferences = userPreferences,
        )
    }

    @Provides
    @Singleton
    fun provideGeminiChatService(geminiClient: GeminiRestClient): GeminiChatService {
        return GeminiChatService(geminiClient)
    }

    @Provides
    @Singleton
    fun provideGroqChatService(@Named("groq") httpClient: HttpClient): GroqChatService {
        return GroqChatService(httpClient)
    }

    @Provides
    @Singleton
    fun provideAiRepository(
        geminiChatService: GeminiChatService,
        groqChatService: GroqChatService,
        dataContextBuilder: DataContextBuilder,
    ): AiRepository {
        return AiRepositoryImpl(
            geminiChatService = geminiChatService,
            groqChatService = groqChatService,
            dataContextBuilder = dataContextBuilder,
        )
    }
}
