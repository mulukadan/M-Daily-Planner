package com.example.m_dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProjectViewModel(
    application: Application,
    private val repository: ProjectRepository
) : AndroidViewModel(application) {

    val projects: StateFlow<List<ProjectWithStats>> = repository.getProjectsWithStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _selectedProjectId = MutableStateFlow<Int?>(null)

    val projectTasks: StateFlow<List<ProjectTask>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getTasksForProject(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun selectProject(projectId: Int) {
        _selectedProjectId.value = projectId
    }

    fun addProject(project: Project) {
        viewModelScope.launch { repository.insertProject(project) }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch { repository.updateProject(project) }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch { repository.deleteProject(project) }
    }

    fun addTask(task: ProjectTask) {
        viewModelScope.launch {
            val current = _selectedProjectId.value?.let {
                repository.getTasksForProject(it).firstOrNull()
            } ?: emptyList()
            val maxPos = current.maxOfOrNull { it.position } ?: -1
            repository.insertTask(task.copy(position = maxPos + 1))
        }
    }

    fun updateTask(task: ProjectTask) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun deleteTask(task: ProjectTask) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun updateTaskOrder(tasks: List<ProjectTask>) {
        viewModelScope.launch {
            repository.updateTasks(tasks.mapIndexed { i, t -> t.copy(position = i) })
        }
    }
}

class ProjectViewModelFactory(
    private val application: Application,
    private val repository: ProjectRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
