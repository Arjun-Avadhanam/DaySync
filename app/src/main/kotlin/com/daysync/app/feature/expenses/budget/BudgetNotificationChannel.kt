package com.daysync.app.feature.expenses.budget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object BudgetNotificationChannel {
    const val CHANNEL_ID = "budget_alerts"
    private const val CHANNEL_NAME = "Budget Alerts"
    private const val CHANNEL_DESCRIPTION = "Alerts when you reach 50/75/100% of a budget"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = CHANNEL_DESCRIPTION }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
