package com.example.m_dailyplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectTaskDao {

    @Query("SELECT * FROM project_tasks WHERE projectId = :projectId ORDER BY position ASC, createdAt ASC")
    fun getTasksForProject(projectId: Int): Flow<List<ProjectTask>>

    @Query("SELECT * FROM project_tasks")
    suspend fun getAllProjectTasksList(): List<ProjectTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProjectTask): Long

    @Update
    suspend fun updateTask(task: ProjectTask)

    @Transaction
    suspend fun updateTasks(tasks: List<ProjectTask>) {
        tasks.forEach { updateTask(it) }
    }

    @Delete
    suspend fun deleteTask(task: ProjectTask)
}
