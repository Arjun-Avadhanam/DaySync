package com.daysync.app.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.daysync.app.core.database.AppDatabase
import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.DailyHealthOverrideDao
import com.daysync.app.core.database.dao.DailyMealEntryDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExerciseSessionDao
import com.daysync.app.core.database.dao.WorkoutMetadataDao
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

    // v5 → v6: add `watchnotes` column to watchlist_entries for user match notes
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE watchlist_entries ADD COLUMN watchnotes TEXT")
        }
    }

    // v6 → v7: add budgets table
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `budgets` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `type` TEXT NOT NULL,
                    `category` TEXT,
                    `amount` REAL NOT NULL,
                    `recurring` INTEGER NOT NULL,
                    `yearMonth` TEXT,
                    `weekBlock` INTEGER,
                    `startDate` TEXT,
                    `endDate` TEXT,
                    `label` TEXT,
                    `syncStatus` TEXT NOT NULL,
                    `lastModified` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_type` ON `budgets` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_yearMonth` ON `budgets` (`yearMonth`)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "daysync.db"
        )
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // Health DAOs
    @Provides fun provideHealthMetricDao(db: AppDatabase): HealthMetricDao = db.healthMetricDao()
    @Provides fun provideSleepSessionDao(db: AppDatabase): SleepSessionDao = db.sleepSessionDao()
    @Provides fun provideExerciseSessionDao(db: AppDatabase): ExerciseSessionDao = db.exerciseSessionDao()
    @Provides fun provideDailyHealthOverrideDao(db: AppDatabase): DailyHealthOverrideDao = db.dailyHealthOverrideDao()
    @Provides fun provideWorkoutMetadataDao(db: AppDatabase): WorkoutMetadataDao = db.workoutMetadataDao()

    // Nutrition DAOs
    @Provides fun provideFoodItemDao(db: AppDatabase): FoodItemDao = db.foodItemDao()
    @Provides fun provideMealTemplateDao(db: AppDatabase): MealTemplateDao = db.mealTemplateDao()
    @Provides fun provideMealTemplateItemDao(db: AppDatabase): MealTemplateItemDao = db.mealTemplateItemDao()
    @Provides fun provideDailyMealEntryDao(db: AppDatabase): DailyMealEntryDao = db.dailyMealEntryDao()
    @Provides fun provideDailyNutritionSummaryDao(db: AppDatabase): DailyNutritionSummaryDao = db.dailyNutritionSummaryDao()

    // Expense DAOs
    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides fun providePayeeRuleDao(db: AppDatabase): PayeeRuleDao = db.payeeRuleDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    // Sport DAO
    @Provides fun provideSportEventDao(db: AppDatabase): SportEventDao = db.sportEventDao()

    // Journal DAO
    @Provides fun provideJournalEntryDao(db: AppDatabase): JournalEntryDao = db.journalEntryDao()

    // Media DAO
    @Provides fun provideMediaItemDao(db: AppDatabase): MediaItemDao = db.mediaItemDao()

    // Sync DAO
    @Provides fun provideSyncLogDao(db: AppDatabase): SyncLogDao = db.syncLogDao()
}
