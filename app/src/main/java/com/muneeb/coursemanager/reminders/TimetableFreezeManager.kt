package com.muneeb.coursemanager.reminders

import android.content.Context
import com.muneeb.coursemanager.data.database.AppDatabase
import com.muneeb.coursemanager.data.preferences.UserPreferences
import kotlinx.coroutines.flow.firstOrNull

object TimetableFreezeManager {

    const val UNFREEZE_REQUEST_CODE = 900001

    suspend fun freeze(context: Context, untilMillis: Long?) {
        cancelAllEntryAlarms(context)

        val userPreferences = UserPreferences(context)
        userPreferences.setTimetableFreeze(frozen = true, untilMillis = untilMillis)

        if (untilMillis != null) {
            ReminderScheduler.scheduleExactReminder(
                context = context,
                requestCode = UNFREEZE_REQUEST_CODE,
                triggerAtMillis = untilMillis,
                channelId = NotificationChannels.CHANNEL_CLASS_REMINDERS,
                title = "Timetable",
                body = "Reminders resumed",
                isWeeklyRecurring = false,
                receiverClass = TimetableFreezeReceiver::class.java
            )
        } else {
            ReminderScheduler.cancelReminder(context, UNFREEZE_REQUEST_CODE)
        }
    }

    suspend fun unfreeze(context: Context) {
        ReminderScheduler.cancelReminder(context, UNFREEZE_REQUEST_CODE)

        val userPreferences = UserPreferences(context)
        userPreferences.setTimetableFreeze(frozen = false, untilMillis = null)

        rescheduleAllEntries(context)
    }

    suspend fun cancelAllEntryAlarms(context: Context) {
        val dao = AppDatabase.getInstance(context).timetableDao()
        val entries = dao.getAllOnce()
        entries.forEach { entry ->
            ReminderScheduler.cancelReminder(context, entry.entryId.toInt())
        }
    }

    /** Re-arms every timetable entry's weekly alarm. Shared by BootReceiver and unfreeze(). */
    suspend fun rescheduleAllEntries(context: Context) {
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
}