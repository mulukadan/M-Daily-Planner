package com.example.m_dailyplanner.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)

    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)

    fun getTaskByIdFlow(id: Int): Flow<Task?> = taskDao.getTaskByIdFlow(id)

    suspend fun getPendingTasksForDate(date: String): List<Task> = taskDao.getPendingTasksForDate(date)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun updateTasks(tasks: List<Task>) = taskDao.updateTasks(tasks)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun carryForwardTasks(oldDate: String, newDate: String) {
        taskDao.carryForwardTasks(oldDate, newDate)
    }
}
