package com.example.m_dailyplanner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.m_dailyplanner.data.TaskDatabase
import com.example.m_dailyplanner.data.TaskRepository
import com.example.m_dailyplanner.sync.FirestoreSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val repository = TaskRepository(TaskDatabase.getDatabase(context).taskDao(), FirestoreSync())
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        CoroutineScope(Dispatchers.IO).launch {
            repository.getUpcomingReminders(today, currentTime).forEach { task ->
                ReminderScheduler.scheduleReminder(context, task)
            }
        }
    }
}
