package com.example.m_dailyplanner.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) return
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: return
        val taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: ""

        val alarmActivityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_NAME, taskName)
            putExtra(EXTRA_TASK_DESCRIPTION, taskDescription)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, taskId, alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationHelper.ACTION_MARK_DONE
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context, taskId * 2, markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationHelper.ACTION_SNOOZE
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, taskId * 2 + 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationHelper(context).showAlarmNotification(
            taskId, taskName, taskDescription,
            fullScreenPendingIntent, markDonePendingIntent, snoozePendingIntent
        )
    }
}
