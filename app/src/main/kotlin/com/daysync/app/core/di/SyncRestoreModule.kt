package com.daysync.app.core.di

import com.daysync.app.BuildConfig
import com.daysync.app.core.database.dao.DailyHealthOverrideDao
import com.daysync.app.core.database.dao.DailyMealEntryDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.dao.JournalEntryDao
import com.daysync.app.core.database.dao.MealTemplateDao
import com.daysync.app.core.database.dao.MealTemplateItemDao
import com.daysync.app.core.database.dao.MediaItemDao
import com.daysync.app.core.sync.SyncRestoreEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncRestoreModule {

    @Provides
    @Singleton
    fun provideSyncRestoreEngine(
        foodItemDao: FoodItemDao,
        mealTemplateDao: MealTemplateDao,
        mealTemplateItemDao: MealTemplateItemDao,
        dailyMealEntryDao: DailyMealEntryDao,
        dailyNutritionSummaryDao: DailyNutritionSummaryDao,
        expenseDao: ExpenseDao,
        journalEntryDao: JournalEntryDao,
        mediaItemDao: MediaItemDao,
        dailyHealthOverrideDao: DailyHealthOverrideDao,
    ): SyncRestoreEngine {
        val httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        return SyncRestoreEngine(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            httpClient = httpClient,
            foodItemDao = foodItemDao,
            mealTemplateDao = mealTemplateDao,
            mealTemplateItemDao = mealTemplateItemDao,
            dailyMealEntryDao = dailyMealEntryDao,
            dailyNutritionSummaryDao = dailyNutritionSummaryDao,
            expenseDao = expenseDao,
            journalEntryDao = journalEntryDao,
            mediaItemDao = mediaItemDao,
            dailyHealthOverrideDao = dailyHealthOverrideDao,
        )
    }
}
