package com.muneeb.coursemanager.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

object NotificationChannels {
    const val CHANNEL_CLASS_REMINDERS = "class_reminders"
    const val CHANNEL_STUDY_REMINDERS = "study_reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_CLASS_REMINDERS,
                    "Class Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for upcoming classes"
                    enableVibration(true)  // Fixed: method call, not property assignment
                },
                NotificationChannel(
                    CHANNEL_STUDY_REMINDERS,
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for study sessions"
                    enableVibration(true)  // Fixed: method call, not property assignment
                }
            )
            NotificationManagerCompat.from(context).createNotificationChannels(channels)
        }
    }
}