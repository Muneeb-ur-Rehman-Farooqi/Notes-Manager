package com.muneeb.coursemanager.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.muneeb.coursemanager.data.database.AppDatabase

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != android.content.Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).timetableDao()
                val entries = dao.getAllOnce()
                entries.forEach { entry ->
                    val triggerMillis = ReminderScheduler.computeNextTriggerMillis(
                        dayOfWeek = entry.dayOfWeek,
                        hour = entry.startHour,
                        minute = entry.startMinute,
                        minutesBefore = 5
                    )
                    val title = entry.subjectName
                    val body = buildString {
                        if (entry.room != null && entry.teacher != null) {
                            append("${entry.room} · ${entry.teacher}")
                        } else if (entry.room != null) {
                            append("Room ${entry.room}")
                        } else if (entry.teacher != null) {
                            append("Teacher ${entry.teacher}")
                        } else {
                            append("Class starting soon")
                        }
                    }
                    ReminderScheduler.scheduleExactReminder(
                        context = context,
                        requestCode = entry.entryId.toInt(),
                        triggerAtMillis = triggerMillis,
                        channelId = NotificationChannels.CHANNEL_CLASS_REMINDERS,
                        title = title,
                        body = body,
                        isWeeklyRecurring = true
                    )
                }
            } catch (_: Exception) {
                // Log error
            } finally {
                pendingResult.finish()
            }
        }
    }
}