package com.example.m_dailyplanner.notification

import android.content.Context
import androidx.work.*
import com.example.m_dailyplanner.data.Task
import com.example.m_dailyplanner.worker.TaskReminderWorker
import java.time.Duration
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

        val delay = triggerTime - System.currentTimeMillis()
        if (delay < 0) return

        val data = Data.Builder()
            .putInt(TaskReminderWorker.EXTRA_TASK_ID, task.id)
            .putString(TaskReminderWorker.EXTRA_TASK_TITLE, task.name)
            .putString(TaskReminderWorker.EXTRA_TASK_DESCRIPTION, task.description)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("reminder_${task.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_${task.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(context: Context, taskId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$taskId")
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
