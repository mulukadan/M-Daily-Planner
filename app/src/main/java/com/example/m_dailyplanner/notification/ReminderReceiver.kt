package com.example.m_dailyplanner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.m_dailyplanner.data.TaskDatabase
import com.example.m_dailyplanner.data.TaskRepository
import com.example.m_dailyplanner.data.TaskStatus
import com.example.m_dailyplanner.sync.FirestoreSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(NotificationHelper.EXTRA_TASK_ID, -1)
        if (taskId == -1) return

        when (intent.action) {
            NotificationHelper.ACTION_MARK_DONE -> {
                markTaskAsDone(context, taskId)
            }
            NotificationHelper.ACTION_SNOOZE -> {
                snoozeTask(context, taskId)
            }
        }
    }

    private fun markTaskAsDone(context: Context, taskId: Int) {
        val database = TaskDatabase.getDatabase(context)
        val repository = TaskRepository(database.taskDao(), FirestoreSync())
        
        CoroutineScope(Dispatchers.IO).launch {
            repository.getTaskById(taskId)?.let { task ->
                repository.updateTask(task.copy(status = TaskStatus.COMPLETED.name))
                NotificationHelper(context).cancelNotification(taskId)
                ReminderScheduler.cancelReminder(context, taskId)
            }
        }
    }

    private fun snoozeTask(context: Context, taskId: Int) {
        val database = TaskDatabase.getDatabase(context)
        val repository = TaskRepository(database.taskDao(), FirestoreSync())

        CoroutineScope(Dispatchers.IO).launch {
            repository.getTaskById(taskId)?.let { task ->
                ReminderScheduler.scheduleReminder(context, task, snoozeMinutes = 15)
                NotificationHelper(context).cancelNotification(taskId)
            }
        }
    }

    companion object {
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_TASK_DESCRIPTION = "task_description"
        const val EXTRA_TASK_ID = "task_id"
    }
}
