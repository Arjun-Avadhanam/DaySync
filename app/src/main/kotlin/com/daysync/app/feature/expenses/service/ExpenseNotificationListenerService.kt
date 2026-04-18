package com.daysync.app.feature.expenses.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.daysync.app.MainActivity
import com.daysync.app.R
import com.daysync.app.feature.expenses.data.ExpenseRepository
import com.daysync.app.feature.expenses.data.ProcessResult
import com.daysync.app.feature.expenses.model.formatIndianCurrency
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ExpenseNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processingMutex = Mutex()
    private lateinit var repository: ExpenseRepository

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ExpenseListenerEntryPoint::class.java,
        )
        repository = entryPoint.expenseRepository()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName !in NotificationParser.MONITORED_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val text = bigText ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        val parsed = NotificationParser.parse(packageName, title, text) ?: return
        if (!parsed.isDebit) return

        scope.launch {
            try {
                // Mutex ensures only one notification processes at a time,
                // preventing the race where two identical notifications both
                // pass dedup check before either inserts.
                when (val result = processingMutex.withLock { repository.processNotification(parsed) }) {
                    is ProcessResult.Saved -> {
                        Log.d(TAG, "Saved expense: ${result.expense.formattedAmount}")
                    }
                    is ProcessResult.Deduplicated -> {
                        Log.d(TAG, "Duplicate skipped: ${result.existingId}")
                    }
                    is ProcessResult.NeedsClassification -> {
                        postClassificationNotification(result.expense.id, result.expense.totalAmount)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification from $packageName", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No action needed
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun postClassificationNotification(expenseId: String, amount: Double) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "expense_detail")
            putExtra("expense_id", expenseId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, expenseId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, ExpenseNotificationChannel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New expense: ${formatIndianCurrency(amount)}")
            .setContentText("Tap to categorize this expense")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(expenseId.hashCode(), notification)
    }

    companion object {
        private const val TAG = "ExpenseNotifListener"
    }
}
