package com.muneeb.coursemanager.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("extra_title") ?: "Reminder"
        val body = intent.getStringExtra("extra_body") ?: ""
        val channelId = intent.getStringExtra("extra_channel") ?: NotificationChannels.CHANNEL_CLASS_REMINDERS
        val notificationId = intent.getIntExtra("extra_notification_id", 0)
        val isWeekly = intent.getBooleanExtra("extra_is_weekly", false)
        val originalTrigger = intent.getLongExtra("extra_original_trigger", 0)

        ReminderNotifier.showNotification(
            context = context,
            channelId = channelId,
            notificationId = notificationId,
            title = title,
            body = body
        )

        if (isWeekly && originalTrigger > 0) {
            // Reschedule for next week
            val nextTrigger = originalTrigger + (7 * 24 * 60 * 60 * 1000L)
            ReminderScheduler.scheduleExactReminder(
                context = context,
                requestCode = notificationId,
                triggerAtMillis = nextTrigger,
                channelId = channelId,
                title = title,
                body = body,
                isWeeklyRecurring = true
            )
        }
    }
}