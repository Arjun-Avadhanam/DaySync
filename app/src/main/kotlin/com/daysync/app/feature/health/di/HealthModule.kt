package com.daysync.app.feature.health.di

import android.content.Context
import com.daysync.app.core.database.dao.ExerciseSessionDao
import com.daysync.app.core.database.dao.HealthMetricDao
import com.daysync.app.core.database.dao.SleepSessionDao
import com.daysync.app.feature.health.data.HealthConnectManager
import com.daysync.app.feature.health.data.HealthRepository
import com.daysync.app.feature.health.data.HealthRepositoryImpl
import com.daysync.app.feature.health.model.HeartRateZoneConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthModule {

    @Provides
    @Singleton
    fun provideHeartRateZoneConfig(): HeartRateZoneConfig = HeartRateZoneConfig(
        zone1Ceiling = 96,
        zone2Ceiling = 124,
        zone3Ceiling = 145,
        zone4Ceiling = 166,
        zone5Ceiling = 180,
    )

    @Provides
    @Singleton
    fun provideHealthConnectManager(
        @ApplicationContext context: Context,
    ): HealthConnectManager = HealthConnectManager(context)

    @Provides
    @Singleton
    fun provideHealthRepository(
        healthConnectManager: HealthConnectManager,
        healthMetricDao: HealthMetricDao,
        sleepSessionDao: SleepSessionDao,
        exerciseSessionDao: ExerciseSessionDao,
        zoneConfig: HeartRateZoneConfig,
    ): HealthRepository = HealthRepositoryImpl(
        healthConnectManager,
        healthMetricDao,
        sleepSessionDao,
        exerciseSessionDao,
        zoneConfig,
    )
}
