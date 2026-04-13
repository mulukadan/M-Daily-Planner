package com.example.m_dailyplanner.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.m_dailyplanner.notification.NotificationHelper

class TaskReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt(EXTRA_TASK_ID, -1)
        val taskTitle = inputData.getString(EXTRA_TASK_TITLE) ?: return Result.failure()
        val taskDescription = inputData.getString(EXTRA_TASK_DESCRIPTION) ?: ""

        if (taskId == -1) return Result.failure()

        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showNotification(taskId, taskTitle, taskDescription)

        return Result.success()
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    }
}
