package com.daysync.app.feature.expenses.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.daysync.app.feature.expenses.service.ExpenseNotificationListenerService

object NotificationAccessHelper {

    fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false

        val componentName = ComponentName(
            context,
            ExpenseNotificationListenerService::class.java,
        ).flattenToString()

        return enabledListeners.contains(componentName)
    }

    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
