package com.daysync.app.core.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daysync.app.MainActivity
import com.daysync.app.core.database.dao.DailyHealthOverrideDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dailyHealthOverrideDao: DailyHealthOverrideDao,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val override = dailyHealthOverrideDao.get(today)

        val missingCalories = override?.totalCalories == null
        val missingWeight = override?.weightMorning == null ||
            override.weightEvening == null ||
            override.weightNight == null

        if (missingCalories || missingWeight) {
            val message = buildString {
                if (missingCalories) append("calories burned")
                if (missingCalories && missingWeight) append(" and ")
                if (missingWeight) append("weight")
            }
            postReminder("Don't forget to log today's $message")
        }
        return Result.success()
    }

    private fun postReminder(message: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID, "Daily Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Reminds you to log daily health data" }
        manager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("navigate_to", "health")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DaySync Reminder")
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "daysync_reminder"
        private const val NOTIFICATION_ID = 1003
    }
}
