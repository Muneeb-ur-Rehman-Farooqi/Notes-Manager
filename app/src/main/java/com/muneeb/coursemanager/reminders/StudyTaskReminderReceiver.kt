package com.muneeb.coursemanager.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muneeb.coursemanager.data.dao.StudyTaskDao
import com.muneeb.coursemanager.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyTaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).studyTaskDao()
                when (alarmType) {
                    ALARM_TYPE_DAILY_PING -> handleDailyPing(context, dao)
                    ALARM_TYPE_ESCALATION -> {
                        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                        if (taskId > 0L) handleEscalation(context, dao, taskId)
                    }
                }
            } catch (_: Exception) {
                // best-effort
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleDailyPing(context: Context, dao: StudyTaskDao) {
        val tasks = dao.getAllOnce()
        val hasPending = tasks.any { task ->
            !task.isCompleted && (task.isTodayTask || task.deadlineDateMillis != null)
        }
        if (hasPending) {
            ReminderNotifier.showNotification(
                context = context,
                channelId = NotificationChannels.CHANNEL_STUDY_REMINDERS,
                notificationId = DAILY_PING_NOTIFICATION_ID,
                title = "To-Do List",
                body = "You have pending tasks"
            )
        }
        val nextPing = ReminderScheduler.computeNextDailyPingMillis()
        ReminderScheduler.scheduleExactReminder(
            context = context,
            requestCode = DAILY_PING_REQUEST_CODE,
            triggerAtMillis = nextPing,
            channelId = NotificationChannels.CHANNEL_STUDY_REMINDERS,
            title = "To-Do List",
            body = "You have pending tasks",
            isWeeklyRecurring = false,
            receiverClass = StudyTaskReminderReceiver::class.java,
            extraAlarmType = ALARM_TYPE_DAILY_PING,
            extraTaskId = null
        )
    }

    private suspend fun handleEscalation(context: Context, dao: StudyTaskDao, taskId: Long) {
        val task = dao.getById(taskId) ?: return
        if (task.isCompleted) return
        val dateMillis = task.deadlineDateMillis ?: return
        val effective = ReminderScheduler.computeEffectiveDeadlineMillis(
            dateMillis = dateMillis,
            hour = task.deadlineHour,
            minute = task.deadlineMinute
        )
        val now = System.currentTimeMillis()
        if (now >= effective) return
        val body = remainingLabel(effective - now)
        ReminderNotifier.showNotification(
            context = context,
            channelId = NotificationChannels.CHANNEL_STUDY_REMINDERS,
            notificationId = taskId.toInt(),
            title = task.title,
            body = body
        )
        val nextTrigger = now + 60L * 60L * 1000L
        ReminderScheduler.scheduleExactReminder(
            context = context,
            requestCode = taskId.toInt(),
            triggerAtMillis = nextTrigger,
            channelId = NotificationChannels.CHANNEL_STUDY_REMINDERS,
            title = task.title,
            body = body,
            isWeeklyRecurring = false,
            receiverClass = StudyTaskReminderReceiver::class.java,
            extraAlarmType = ALARM_TYPE_ESCALATION,
            extraTaskId = taskId
        )
    }

    private fun remainingLabel(remainingMillis: Long): String {
        val hours = remainingMillis / (60L * 60L * 1000L)
        val minutes = (remainingMillis % (60L * 60L * 1000L)) / (60L * 1000L)
        return when {
            hours > 0L && minutes > 0L -> "Due in ${hours}h ${minutes}m"
            hours > 0L -> "Due in ${hours}h"
            else -> "Due in ${minutes}m"
        }
    }

    companion object {
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val ALARM_TYPE_DAILY_PING = "DAILY_PING"
        const val ALARM_TYPE_ESCALATION = "ESCALATION"
        const val DAILY_PING_REQUEST_CODE = -100
        const val DAILY_PING_NOTIFICATION_ID = -100
    }
}