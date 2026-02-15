package com.daysync.app.core.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device rebooted, WorkManager will auto-reschedule")
            // WorkManager automatically reschedules periodic work after reboot
            // No manual rescheduling needed
        }
    }
}
