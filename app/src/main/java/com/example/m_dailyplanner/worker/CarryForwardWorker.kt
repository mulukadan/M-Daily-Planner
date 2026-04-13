package com.example.m_dailyplanner.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.m_dailyplanner.data.DataStoreManager
import com.example.m_dailyplanner.data.TaskDatabase
import com.example.m_dailyplanner.data.TaskRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CarryForwardWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = TaskDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao())
        val dataStoreManager = DataStoreManager(applicationContext)

        val yesterday = LocalDate.now().minusDays(1)
        val yesterdayString = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val pendingTasks = repository.getPendingTasksForDate(yesterdayString)

        if (pendingTasks.isNotEmpty()) {
            dataStoreManager.setCarryForward(pendingTasks.size, yesterdayString)
        }

        return Result.success()
    }
}
