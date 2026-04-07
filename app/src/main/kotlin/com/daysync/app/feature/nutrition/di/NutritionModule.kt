package com.daysync.app.feature.nutrition.di

import android.content.Context
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.feature.nutrition.data.NotionMealImporter
import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.data.repository.NutritionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NutritionProviderModule {

    @Provides
    @Singleton
    fun provideNotionMealImporter(
        @ApplicationContext context: Context,
        foodItemDao: FoodItemDao,
    ): NotionMealImporter {
        return NotionMealImporter(context, foodItemDao)
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
