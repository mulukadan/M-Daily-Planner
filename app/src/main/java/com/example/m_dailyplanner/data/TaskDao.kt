package com.example.m_dailyplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY date ASC, CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END ASC, position ASC, time ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<Task>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END ASC, position ASC, time ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Int): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE date = :date AND status = 'PENDING'")
    suspend fun getPendingTasksForDate(date: String): List<Task>

    @Query("SELECT * FROM tasks WHERE date < :beforeDate AND status = 'PENDING'")
    suspend fun getPendingTasksBefore(beforeDate: String): List<Task>

    @Query("SELECT * FROM tasks WHERE reminderEnabled = 1 AND time != '' AND (date > :today OR (date = :today AND time >= :currentTime))")
    suspend fun getUpcomingReminders(today: String, currentTime: String): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Query("SELECT COALESCE(MAX(position), -1) FROM tasks WHERE date = :date")
    suspend fun getMaxPosition(date: String): Int

    // Reads the current max position and inserts within the same transaction, so two
    // rapid inserts for the same date can never observe the same max and collide.
    @Transaction
    suspend fun insertTaskAtEnd(task: Task): Long {
        val nextPosition = getMaxPosition(task.date) + 1
        return insertTask(task.copy(position = nextPosition))
    }

    @Update
    suspend fun updateTask(task: Task)

    @Transaction
    suspend fun updateTasks(tasks: List<Task>) {
        tasks.forEach { updateTask(it) }
    }

    @Query("UPDATE tasks SET date = :newDate WHERE date < :newDate AND status = 'PENDING'")
    suspend fun carryForwardAllPending(newDate: String)

    @Delete
    suspend fun deleteTask(task: Task)
}
