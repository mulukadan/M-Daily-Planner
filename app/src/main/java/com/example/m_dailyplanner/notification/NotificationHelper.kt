package com.example.m_dailyplanner.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.m_dailyplanner.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val ALARM_CHANNEL_ID = "task_alarms"

        const val ACTION_MARK_DONE = "com.example.m_dailyplanner.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.m_dailyplanner.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    init {
        createNotificationChannel()
        createAlarmChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for task reminders"
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun createAlarmChannel() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Task Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen alarm notifications for tasks"
            setSound(alarmUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
        }
        notificationManager().createNotificationChannel(channel)
    }

    fun showAlarmNotification(
        taskId: Int,
        title: String,
        description: String,
        fullScreenPendingIntent: PendingIntent,
        markDonePendingIntent: PendingIntent,
        snoozePendingIntent: PendingIntent
    ) {
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description.ifEmpty { "Tap to open alarm" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Mark Done", markDonePendingIntent)
            .addAction(0, "Snooze 15 min", snoozePendingIntent)
            .build()

        notificationManager().notify(taskId, notification)
    }

    fun showNotification(taskId: Int, title: String, description: String) {
        val markDoneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context, taskId * 2, markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, taskId * 2 + 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Mark Done", markDonePendingIntent)
            .addAction(0, "Snooze 15 min", snoozePendingIntent)
            .build()

        notificationManager().notify(taskId, notification)
    }

    fun cancelNotification(taskId: Int) {
        notificationManager().cancel(taskId)
    }

    private fun notificationManager() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
