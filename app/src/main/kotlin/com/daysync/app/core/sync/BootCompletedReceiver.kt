package com.daysync.app.core.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device rebooted, rescheduling reminder alarm")
            // WorkManager auto-reschedules periodic work after reboot
            // AlarmManager alarms do NOT survive reboot — reschedule manually
            ReminderAlarmReceiver.scheduleReminder(context)
        }
    }
}
