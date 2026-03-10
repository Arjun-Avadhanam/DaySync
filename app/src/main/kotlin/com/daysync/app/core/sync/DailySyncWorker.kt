package com.daysync.app.core.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEngine: SyncEngine,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Daily sync started (attempt ${runAttemptCount + 1})")
        return try {
            setForeground(createForegroundInfo())
            val result = syncEngine.syncAll()
            if (result.isSuccess) {
                Log.d(TAG, "Daily sync completed successfully")
                Result.success()
            } else {
                Log.e(TAG, "Daily sync failed: ${result.exceptionOrNull()?.message}")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Daily sync failed with exception", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = CHANNEL_ID
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "DaySync Background Sync",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when DaySync is syncing data to cloud"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("DaySync")
            .setContentText("Syncing data...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "DailySyncWorker"
        private const val CHANNEL_ID = "daysync_sync"
        private const val NOTIFICATION_ID = 1001
    }
}
