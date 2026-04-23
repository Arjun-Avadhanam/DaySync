package com.daysync.app.core.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Reminder alarm fired")

        // Schedule next day's alarm immediately
        scheduleReminder(context)

        // Check missing data and post notification via async work
        // Use a one-shot WorkManager job since we need Hilt-injected DAO
        androidx.work.OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .addTag("daily_reminder_check")
            .build()
            .let { androidx.work.WorkManager.getInstance(context).enqueue(it) }
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        private const val REQUEST_CODE = 2001

        fun scheduleReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // On Android 12+ (API 31+), must check canScheduleExactAlarms() before using exact alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission not granted, skipping reminder scheduling")
                return
            }

            val prefs = com.daysync.app.core.config.UserPreferences(context)
            val intent = Intent(context, ReminderAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val now = ZonedDateTime.now(prefs.javaZoneId)
            var target = now.withHour(prefs.reminderHour).withMinute(prefs.reminderMinute).withSecond(0).withNano(0)
            if (now.isAfter(target)) {
                target = target.plusDays(1)
            }
            val triggerMillis = target.toInstant().toEpochMilli()

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent,
            )
            Log.d(TAG, "Reminder alarm scheduled for $target")
        }
    }
}
