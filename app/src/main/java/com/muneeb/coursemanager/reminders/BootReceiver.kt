package com.muneeb.coursemanager.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muneeb.coursemanager.data.database.AppDatabase
import com.muneeb.coursemanager.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleTimetableReminders(context)
                rescheduleStudyTaskReminders(context)
            } catch (_: Exception) {
                // best-effort
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleTimetableReminders(context: Context) {
        val userPreferences = UserPreferences(context)
        val userName = userPreferences.userName.firstOrNull()
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
            val baseBody = buildString {
                val parts = mutableListOf<String>()
                entry.room?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                entry.teacher?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                if (parts.isEmpty()) append("Class starting soon") else append(parts.joinToString(" · "))
            }
            val body = if (userName != null) "Hey $userName, $baseBody" else baseBody
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
    }

    private suspend fun rescheduleStudyTaskReminders(context: Context) {
        // Re-arm the daily 4 PM ping
        ReminderScheduler.scheduleExactReminder(
            context = context,
            requestCode = StudyTaskReminderReceiver.DAILY_PING_REQUEST_CODE,
            triggerAtMillis = ReminderScheduler.computeNextDailyPingMillis(),
            channelId = NotificationChannels.CHANNEL_STUDY_REMINDERS,
            title = "To-Do List",
            body = "You have pending tasks",
            isWeeklyRecurring = false,
            receiverClass = StudyTaskReminderReceiver::class.java,
            extraAlarmType = StudyTaskReminderReceiver.ALARM_TYPE_DAILY_PING,
            extraTaskId = null
        )

        // Re-arm escalation for each incomplete deadline task
        val dao = AppDatabase.getInstance(context).studyTaskDao()
        val tasks = dao.getAllOnce()
        tasks.forEach { task ->
            if (task.isCompleted) return@forEach
            if (task.isTodayTask) return@forEach
            val dateMillis = task.deadlineDateMillis ?: return@forEach
            val effective = ReminderScheduler.computeEffectiveDeadlineMillis(
                dateMillis = dateMillis,
                hour = task.deadlineHour,
                minute = task.deadlineMinute
            )
            val trigger = ReminderScheduler.firstEscalationTriggerMillis(effective) ?: return@forEach
            ReminderScheduler.scheduleExactReminder(
                context = context,
                requestCode = task.taskId.toInt(),
                triggerAtMillis = trigger,
                channelId = NotificationChannels.CHANNEL_STUDY_REMINDERS,
                title = task.title,
                body = "Due soon",
                isWeeklyRecurring = false,
                receiverClass = StudyTaskReminderReceiver::class.java,
                extraAlarmType = StudyTaskReminderReceiver.ALARM_TYPE_ESCALATION,
                extraTaskId = task.taskId
            )
        }
    }
}