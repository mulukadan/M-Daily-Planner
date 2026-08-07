package com.example.m_dailyplanner.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.m_dailyplanner.data.DataStoreManager
import com.example.m_dailyplanner.data.TaskDatabase
import com.example.m_dailyplanner.data.TaskRepository
import com.example.m_dailyplanner.sync.FirestoreSync
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CarryForwardWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = TaskDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao(), FirestoreSync())
        val dataStoreManager = DataStoreManager(applicationContext)

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Count everything still pending from any prior day, not just yesterday, so a
        // multi-day gap in opening the app never causes older overdue tasks to be missed.
        val pendingTasks = repository.getPendingTasksBefore(today)

        if (pendingTasks.isNotEmpty()) {
            dataStoreManager.setCarryForward(pendingTasks.size)
        }

        return Result.success()
    }
}
