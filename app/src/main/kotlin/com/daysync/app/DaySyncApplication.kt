package com.daysync.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.daysync.app.core.sync.DailySyncWorker
import com.daysync.app.feature.expenses.service.ExpenseNotificationChannel
import dagger.hilt.android.HiltAndroidApp
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class DaySyncApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        ExpenseNotificationChannel.createChannel(this)
        scheduleDailySync()
    }

    private fun scheduleDailySync() {
        val now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
        var targetTime = now.withHour(23).withMinute(59).withSecond(0).withNano(0)
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        val initialDelayMillis = Duration.between(now, targetTime).toMillis()

        val syncRequest = PeriodicWorkRequestBuilder<DailySyncWorker>(
            24, TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("daily_sync")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daysync_daily_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest,
        )
    }
}
