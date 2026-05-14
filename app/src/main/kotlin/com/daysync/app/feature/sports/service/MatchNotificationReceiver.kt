package com.daysync.app.feature.sports.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.daysync.app.MainActivity
import com.daysync.app.R

/**
 * Handles two types of match notifications for watchlisted events:
 * - PRE_MATCH: fires 30 min before kick-off ("Match starting soon")
 * - POST_MATCH: fires 30 min after completion ("Add your Watchnotes")
 */
class MatchNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return
        val eventName = intent.getStringExtra(EXTRA_EVENT_NAME) ?: "Match"
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return

        ensureChannel(context)

        val (title, message) = when (type) {
            TYPE_PRE_MATCH -> "Match starting soon" to "$eventName kicks off in 30 minutes"
            TYPE_POST_MATCH -> "Add your Watchnotes" to "$eventName has finished — record your thoughts"
            else -> return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "sports")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTapIntent = PendingIntent.getActivity(
            context, eventId.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingTapIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(eventId.hashCode() + type.hashCode(), notification)
        Log.d(TAG, "$type notification posted for $eventName")
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, "Match Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Reminders for watchlisted sports matches" }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "MatchNotification"
        private const val CHANNEL_ID = "daysync_match"
        const val EXTRA_TYPE = "match_notif_type"
        const val EXTRA_EVENT_NAME = "match_event_name"
        const val EXTRA_EVENT_ID = "match_event_id"
        const val TYPE_PRE_MATCH = "PRE_MATCH"
        const val TYPE_POST_MATCH = "POST_MATCH"

        /**
         * Schedule a notification at [triggerAtMillis] for the given event.
         */
        fun schedule(
            context: Context,
            eventId: String,
            eventName: String,
            type: String,
            triggerAtMillis: Long,
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarm for match notification")
                return
            }

            val intent = Intent(context, MatchNotificationReceiver::class.java).apply {
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_EVENT_NAME, eventName)
                putExtra(EXTRA_EVENT_ID, eventId)
            }
            val requestCode = (eventId + type).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
            Log.d(TAG, "Scheduled $type for $eventName at ${java.time.Instant.ofEpochMilli(triggerAtMillis)}")
        }

        /**
         * Cancel a previously scheduled notification.
         */
        fun cancel(context: Context, eventId: String, type: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MatchNotificationReceiver::class.java)
            val requestCode = (eventId + type).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                Log.d(TAG, "Cancelled $type for $eventId")
            }
        }
    }
}
