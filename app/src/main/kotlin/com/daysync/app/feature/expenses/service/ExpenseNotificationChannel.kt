package com.daysync.app.feature.expenses.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object ExpenseNotificationChannel {
    const val CHANNEL_ID = "expense_classification"
    private const val CHANNEL_NAME = "Expense Classification"
    private const val CHANNEL_DESCRIPTION = "Prompts to categorize uncategorized expenses"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
