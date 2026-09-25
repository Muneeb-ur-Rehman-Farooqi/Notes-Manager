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
        val isFrozen = userPreferences.isTimetableFrozen.firstOrNull() ?: false

        if (isFrozen) {
            val untilMillis = userPreferences.timetableFreezeUntilMillis.firstOrNull()
            when {
                untilMillis == null -> {
                    // frozen indefinitely — leave every entry's alarm cancelled
                    return
                }
                untilMillis <= System.currentTimeMillis() -> {
                    // freeze window already passed while the device was off
                    TimetableFreezeManager.unfreeze(context)
                }
                else -> {
                    // still frozen — reboot wiped the unfreeze alarm, so re-arm just that
                    ReminderScheduler.scheduleExactReminder(
                        context = context,
                        requestCode = TimetableFreezeManager.UNFREEZE_REQUEST_CODE,
                        triggerAtMillis = untilMillis,
                        channelId = NotificationChannels.CHANNEL_CLASS_REMINDERS,
                        title = "Timetable",
                        body = "Reminders resumed",
                        isWeeklyRecurring = false,
                        receiverClass = TimetableFreezeReceiver::class.java
                    )
                }
            }
            return
        }

        TimetableFreezeManager.rescheduleAllEntries(context)
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