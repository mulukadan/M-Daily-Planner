package com.example.m_dailyplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectTaskDao {

    @Query("SELECT * FROM project_tasks WHERE projectId = :projectId ORDER BY position ASC, createdAt ASC")
    fun getTasksForProject(projectId: Int): Flow<List<ProjectTask>>

    @Query("SELECT * FROM project_tasks")
    suspend fun getAllProjectTasksList(): List<ProjectTask>

    @Query("SELECT * FROM project_tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): ProjectTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProjectTask): Long

    @Query("SELECT COALESCE(MAX(position), -1) FROM project_tasks WHERE projectId = :projectId")
    suspend fun getMaxPosition(projectId: Int): Int

    // Reads the current max position and inserts within the same transaction, so two
    // rapid inserts for the same project can never observe the same max and collide.
    @Transaction
    suspend fun insertTaskAtEnd(task: ProjectTask): Long {
        val nextPosition = getMaxPosition(task.projectId) + 1
        return insertTask(task.copy(position = nextPosition))
    }

    @Update
    suspend fun updateTask(task: ProjectTask)

    @Transaction
    suspend fun updateTasks(tasks: List<ProjectTask>) {
        tasks.forEach { updateTask(it) }
    }

    @Delete
    suspend fun deleteTask(task: ProjectTask)
}
