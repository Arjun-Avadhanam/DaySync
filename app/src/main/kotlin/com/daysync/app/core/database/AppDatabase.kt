package com.daysync.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.daysync.app.core.database.entity.CompetitionEntity
import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.DailyHealthOverrideEntity
import com.daysync.app.core.database.entity.DailyMealEntryEntity
import com.daysync.app.core.database.entity.WorkoutMetadataEntity
import com.daysync.app.core.database.entity.DailyNutritionSummaryEntity
import com.daysync.app.core.database.entity.EventParticipantEntity
import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.ExpenseEntity
import com.daysync.app.core.database.entity.FollowedCompetitionEntity
import com.daysync.app.core.database.entity.FollowedCompetitorEntity
import com.daysync.app.core.database.entity.FoodItemEntity
import com.daysync.app.core.database.entity.HealthMetricEntity
import com.daysync.app.core.database.entity.JournalEntryEntity
import com.daysync.app.core.database.entity.MealTemplateEntity
import com.daysync.app.core.database.entity.MealTemplateItemEntity
import com.daysync.app.core.database.entity.MediaItemEntity
import com.daysync.app.core.database.entity.PayeeRuleEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import com.daysync.app.core.database.entity.SportEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.database.entity.SyncLogEntity
import com.daysync.app.core.database.entity.VenueEntity
import com.daysync.app.core.database.entity.WatchlistEntryEntity

@Database(
    entities = [
        // Health (5)
        HealthMetricEntity::class,
        SleepSessionEntity::class,
        ExerciseSessionEntity::class,
        DailyHealthOverrideEntity::class,
        WorkoutMetadataEntity::class,
        // Nutrition (5)
        FoodItemEntity::class,
        MealTemplateEntity::class,
        MealTemplateItemEntity::class,
        DailyMealEntryEntity::class,
        DailyNutritionSummaryEntity::class,
        // Expenses (2)
        ExpenseEntity::class,
        PayeeRuleEntity::class,
        // Sports (10)
        SportEntity::class,
        CompetitionEntity::class,
        CompetitorEntity::class,
        VenueEntity::class,
        SportEventEntity::class,
        EventParticipantEntity::class,
        WatchlistEntryEntity::class,
        FollowedCompetitorEntity::class,
        FollowedCompetitionEntity::class,
        SyncLogEntity::class,
        // Journal (1)
        JournalEntryEntity::class,
        // Media (1)
        MediaItemEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // Health
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun dailyHealthOverrideDao(): DailyHealthOverrideDao
    abstract fun workoutMetadataDao(): WorkoutMetadataDao

    // Nutrition
    abstract fun foodItemDao(): FoodItemDao
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun mealTemplateItemDao(): MealTemplateItemDao
    abstract fun dailyMealEntryDao(): DailyMealEntryDao
    abstract fun dailyNutritionSummaryDao(): DailyNutritionSummaryDao

    // Expenses
    abstract fun expenseDao(): ExpenseDao
    abstract fun payeeRuleDao(): PayeeRuleDao

    // Sports
    abstract fun sportEventDao(): SportEventDao

    // Journal
    abstract fun journalEntryDao(): JournalEntryDao

    // Media
    abstract fun mediaItemDao(): MediaItemDao

    // Sync
    abstract fun syncLogDao(): SyncLogDao
}
