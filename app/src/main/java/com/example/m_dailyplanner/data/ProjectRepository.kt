package com.example.m_dailyplanner.data

import com.example.m_dailyplanner.sync.FirestoreSync
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val projectTaskDao: ProjectTaskDao,
    private val firestoreSync: FirestoreSync
) {
    fun getProjectsWithStats(): Flow<List<ProjectWithStats>> = projectDao.getProjectsWithStats()

    suspend fun getProjectById(id: Int): Project? = projectDao.getProjectById(id)

    suspend fun insertProject(project: Project): Long {
        val id = projectDao.insertProject(project)
        firestoreSync.upsertProject(project.copy(id = id.toInt()))
        return id
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
        firestoreSync.upsertProject(project)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteTasksForProject(project.id)
        projectDao.deleteProject(project)
        firestoreSync.deleteProject(project.id)
    }

    fun getTasksForProject(projectId: Int): Flow<List<ProjectTask>> =
        projectTaskDao.getTasksForProject(projectId)

    suspend fun insertTask(task: ProjectTask): Long {
        val id = projectTaskDao.insertTask(task)
        firestoreSync.upsertProjectTask(task.copy(id = id.toInt()))
        return id
    }

    suspend fun updateTask(task: ProjectTask) {
        projectTaskDao.updateTask(task)
        firestoreSync.upsertProjectTask(task)
    }

    suspend fun updateTasks(tasks: List<ProjectTask>) {
        projectTaskDao.updateTasks(tasks)
        tasks.forEach { firestoreSync.upsertProjectTask(it) }
    }

    suspend fun deleteTask(task: ProjectTask) {
        projectTaskDao.deleteTask(task)
        firestoreSync.deleteProjectTask(task.id)
    }
}
