package com.daysync.app.feature.expenses.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class ExpenseNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // TODO: Implement in feature/expenses branch
        // 1. Filter for financial app packages (GPay, PhonePe, HDFC, etc.)
        // 2. Parse notification text for amount, merchant
        // 3. Auto-categorize and store in Room
        Log.d(TAG, "Notification from: ${sbn.packageName}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No action needed
    }

    companion object {
        private const val TAG = "ExpenseNotifListener"
    }
}
