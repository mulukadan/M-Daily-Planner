package com.example.m_dailyplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("""
        SELECT p.id, p.name, p.description, p.createdAt,
               COUNT(pt.id) AS totalTasks,
               COALESCE(SUM(CASE WHEN pt.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedTasks
        FROM projects p
        LEFT JOIN project_tasks pt ON pt.projectId = p.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    fun getProjectsWithStats(): Flow<List<ProjectWithStats>>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjectsList(): List<Project>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM project_tasks WHERE projectId = :projectId")
    suspend fun deleteTasksForProject(projectId: Int)
}
