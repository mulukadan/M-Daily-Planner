package com.example.m_dailyplanner.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.m_dailyplanner.data.Task
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun scheduleReminder(context: Context, task: Task, snoozeMinutes: Long = 0) {
        if (!task.reminderEnabled && snoozeMinutes == 0L) return
        if (task.time.isEmpty() && snoozeMinutes == 0L) return

        val triggerTime = if (snoozeMinutes > 0) {
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(snoozeMinutes)
        } else {
            parseDateTime(task.date, task.time) ?: return
        }

        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, task)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(context: Context, task: Task): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(AlarmReceiver.EXTRA_TASK_NAME, task.name)
            putExtra(AlarmReceiver.EXTRA_TASK_DESCRIPTION, task.description)
        }
        return PendingIntent.getBroadcast(
            context, task.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun parseDateTime(date: String, time: String): Long? {
        return try {
            val localDateTime = LocalDateTime.parse("$date $time", dateTimeFormatter)
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}
