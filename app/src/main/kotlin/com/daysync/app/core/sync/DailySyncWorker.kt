package com.daysync.app.core.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import android.content.pm.ServiceInfo
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
        ensureChannel()
        return try {
            // Android 12+ blocks startForegroundService() from background.
            // The sync still works without the foreground notification.
            try { setForeground(createForegroundInfo()) } catch (_: Exception) {}
            val result = syncEngine.syncAll()
            if (result.isSuccess) {
                Log.d(TAG, "Daily sync completed successfully")
                postResultNotification("Sync completed", "All data synced to cloud.")
                Result.success()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Daily sync failed: $msg")
                postResultNotification("Sync failed", msg)
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Daily sync failed with exception", e)
            postResultNotification("Sync failed", e.message ?: "Unknown error")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun ensureChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DaySync Background Sync",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when DaySync is syncing data to cloud"
        }
        manager.createNotificationChannel(channel)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("DaySync")
            .setContentText("Syncing data...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun postResultNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setAutoCancel(true)
            .build()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.notify(RESULT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "DailySyncWorker"
        private const val CHANNEL_ID = "daysync_sync"
        private const val NOTIFICATION_ID = 1001
        private const val RESULT_NOTIFICATION_ID = 1002
    }
}
