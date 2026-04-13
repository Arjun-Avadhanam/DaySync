package com.daysync.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Catches uncaught exceptions, saves the stack trace to a file AND
 * posts a notification with the crash summary. The notification is
 * visible even if the app can't start — no ADB needed.
 */
object CrashLogger {

    private const val FILE_NAME = "last_crash.txt"
    private const val CHANNEL_ID = "daysync_crash"
    private const val NOTIFICATION_ID = 9999

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val stackTrace = throwable.stackTraceToString().take(4000)
                val log = buildString {
                    appendLine("=== DaySync Crash ===")
                    appendLine("Time: $timestamp")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    appendLine(stackTrace)
                }
                File(context.filesDir, FILE_NAME).writeText(log)

                // Post a notification with the crash summary so the user
                // can see it even if the app won't open.
                val firstLine = "${throwable::class.simpleName}: ${throwable.message?.take(150)}"
                postCrashNotification(context, firstLine)
            } catch (_: Exception) {
                // Can't do anything if even the logger fails
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun postCrashNotification(context: Context, message: String) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID, "Crash Reports",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Shows crash details when the app fails to start" }
            manager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("DaySync crashed")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Best effort — if notifications fail too, at least the file is written
        }
    }

    fun getLastCrash(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else null
    }

    fun clearLastCrash(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}
