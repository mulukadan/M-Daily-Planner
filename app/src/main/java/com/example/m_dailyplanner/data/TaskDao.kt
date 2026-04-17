package com.example.m_dailyplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY date ASC, position ASC, time ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY position ASC, time ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Int): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE date = :date AND status = 'PENDING'")
    suspend fun getPendingTasksForDate(date: String): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Transaction
    suspend fun updateTasks(tasks: List<Task>) {
        tasks.forEach { updateTask(it) }
    }

    @Query("UPDATE tasks SET date = :newDate WHERE date = :oldDate AND status = 'PENDING'")
    suspend fun carryForwardTasks(oldDate: String, newDate: String)

    @Delete
    suspend fun deleteTask(task: Task)
}
