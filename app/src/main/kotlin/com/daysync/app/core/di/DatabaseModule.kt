package com.daysync.app.core.di

import android.content.Context
import androidx.room.Room
import com.daysync.app.core.database.AppDatabase
import com.daysync.app.core.database.dao.DailyHealthOverrideDao
import com.daysync.app.core.database.dao.DailyMealEntryDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExerciseSessionDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.dao.PayeeRuleDao
import com.daysync.app.core.database.dao.HealthMetricDao
import com.daysync.app.core.database.dao.JournalEntryDao
import com.daysync.app.core.database.dao.MealTemplateDao
import com.daysync.app.core.database.dao.MealTemplateItemDao
import com.daysync.app.core.database.dao.MediaItemDao
import com.daysync.app.core.database.dao.SleepSessionDao
import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.core.database.dao.SyncLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "daysync.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // Health DAOs
    @Provides fun provideHealthMetricDao(db: AppDatabase): HealthMetricDao = db.healthMetricDao()
    @Provides fun provideSleepSessionDao(db: AppDatabase): SleepSessionDao = db.sleepSessionDao()
    @Provides fun provideExerciseSessionDao(db: AppDatabase): ExerciseSessionDao = db.exerciseSessionDao()
    @Provides fun provideDailyHealthOverrideDao(db: AppDatabase): DailyHealthOverrideDao = db.dailyHealthOverrideDao()

    // Nutrition DAOs
    @Provides fun provideFoodItemDao(db: AppDatabase): FoodItemDao = db.foodItemDao()
    @Provides fun provideMealTemplateDao(db: AppDatabase): MealTemplateDao = db.mealTemplateDao()
    @Provides fun provideMealTemplateItemDao(db: AppDatabase): MealTemplateItemDao = db.mealTemplateItemDao()
    @Provides fun provideDailyMealEntryDao(db: AppDatabase): DailyMealEntryDao = db.dailyMealEntryDao()
    @Provides fun provideDailyNutritionSummaryDao(db: AppDatabase): DailyNutritionSummaryDao = db.dailyNutritionSummaryDao()

    // Expense DAOs
    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides fun providePayeeRuleDao(db: AppDatabase): PayeeRuleDao = db.payeeRuleDao()

    // Sport DAO
    @Provides fun provideSportEventDao(db: AppDatabase): SportEventDao = db.sportEventDao()

    // Journal DAO
    @Provides fun provideJournalEntryDao(db: AppDatabase): JournalEntryDao = db.journalEntryDao()

    // Media DAO
    @Provides fun provideMediaItemDao(db: AppDatabase): MediaItemDao = db.mediaItemDao()

    // Sync DAO
    @Provides fun provideSyncLogDao(db: AppDatabase): SyncLogDao = db.syncLogDao()
}
