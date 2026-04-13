package com.daysync.app.core

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Catches uncaught exceptions and saves them to a file so the user
 * can view the crash log in Settings without needing ADB.
 */
object CrashLogger {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val log = buildString {
                    appendLine("=== DaySync Crash ===")
                    appendLine("Time: $timestamp")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    appendLine(throwable.stackTraceToString().take(4000))
                }
                File(context.filesDir, FILE_NAME).writeText(log)
            } catch (_: Exception) {
                // Can't do anything if even the logger fails
            }
            defaultHandler?.uncaughtException(thread, throwable)
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
