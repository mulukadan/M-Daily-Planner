package com.example.m_dailyplanner.data

import com.example.m_dailyplanner.sync.FirestoreSync
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val firestoreSync: FirestoreSync
) {

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)

    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)

    fun getTaskByIdFlow(id: Int): Flow<Task?> = taskDao.getTaskByIdFlow(id)

    suspend fun getPendingTasksForDate(date: String): List<Task> = taskDao.getPendingTasksForDate(date)

    suspend fun getUpcomingReminders(today: String, currentTime: String): List<Task> =
        taskDao.getUpcomingReminders(today, currentTime)

    suspend fun insertTask(task: Task): Long {
        val id = taskDao.insertTask(task)
        firestoreSync.upsertTask(task.copy(id = id.toInt()))
        return id
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        firestoreSync.upsertTask(task)
    }

    suspend fun updateTasks(tasks: List<Task>) {
        taskDao.updateTasks(tasks)
        tasks.forEach { firestoreSync.upsertTask(it) }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        firestoreSync.deleteTask(task.id)
    }

    suspend fun carryForwardTasks(oldDate: String, newDate: String) {
        taskDao.carryForwardTasks(oldDate, newDate)
        // Sync updated tasks after carry-forward
        taskDao.getAllTasksList()
            .filter { it.date == newDate }
            .forEach { firestoreSync.upsertTask(it) }
    }
}
