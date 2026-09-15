package com.muneeb.coursemanager.reminders

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.muneeb.coursemanager.R

object ReminderNotifier {
    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}