package com.daysync.app.feature.nutrition.di

import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.data.repository.NutritionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NutritionModule {

    @Binds
    @Singleton
    abstract fun bindNutritionRepository(
        impl: NutritionRepositoryImpl,
    ): NutritionRepository
}
