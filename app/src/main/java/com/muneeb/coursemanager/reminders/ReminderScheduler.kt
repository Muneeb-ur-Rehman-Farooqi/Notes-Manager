package com.muneeb.coursemanager.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {
    private const val EXTRA_TITLE = "extra_title"
    private const val EXTRA_BODY = "extra_body"
    private const val EXTRA_CHANNEL = "extra_channel"
    private const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    private const val EXTRA_IS_WEEKLY = "extra_is_weekly"
    private const val EXTRA_ORIGINAL_TRIGGER = "extra_original_trigger"

    fun scheduleExactReminder(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        channelId: String,
        title: String,
        body: String,
        isWeeklyRecurring: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_CHANNEL, channelId)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_IS_WEEKLY, isWeeklyRecurring)
            putExtra(EXTRA_ORIGINAL_TRIGGER, triggerAtMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            context.startActivity(Intent(intent))
        }
    }

    fun computeNextTriggerMillis(
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        minutesBefore: Int
    ): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute - minutesBefore)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Adjust to the next occurrence of dayOfWeek
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysUntil = dayOfWeek - currentDay
        if (daysUntil < 0) daysUntil += 7
        if (daysUntil == 0 && System.currentTimeMillis() > cal.timeInMillis) {
            daysUntil = 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysUntil)
        return cal.timeInMillis
    }
}