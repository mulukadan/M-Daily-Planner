package com.example.m_dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.data.*
import com.example.m_dailyplanner.notification.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class SortOption(val displayName: String) {
    PRIORITY("Priority"),
    TIME("Time"),
    CREATION_DATE("Creation Date"),
    MANUAL("Manual")
}

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository,
    private val dataStoreManager: DataStoreManager
) : AndroidViewModel(application) {

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.MANUAL)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val pendingTasksByPriority: StateFlow<List<Task>> = allTasks
        .map { tasks ->
            tasks
                .filter { it.status != TaskStatus.COMPLETED.name }
                .sortedWith(compareBy(
                    { try { TaskPriority.valueOf(it.priority.uppercase()).ordinal } catch (e: Exception) { 1 } },
                    { it.date }
                ))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks, _selectedDate, _sortOption
    ) { tasks, date, sort ->
        val filtered = tasks.filter { it.date == date }
        
        when (sort) {
            SortOption.PRIORITY -> filtered.sortedBy { 
                try { TaskPriority.valueOf(it.priority.uppercase()).ordinal } catch (e: Exception) { 1 }
            }
            SortOption.TIME -> filtered.sortedBy { it.time.ifEmpty { "23:59" } }
            SortOption.CREATION_DATE -> filtered.sortedByDescending { it.createdAt }
            SortOption.MANUAL -> filtered.sortedBy { it.position }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val carryForwardEvent: StateFlow<CarryForwardData?> = dataStoreManager.carryForwardEvent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val showOnboarding: StateFlow<Boolean> = dataStoreManager.showOnboarding
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun getTasksForDate(date: String): Flow<List<Task>> =
        repository.getTasksForDate(date)

    fun getTaskById(taskId: Int): Flow<Task?> = repository.getTaskByIdFlow(taskId)

    fun addTask(task: Task) {
        viewModelScope.launch {
            // Find max position for the date
            val currentTasks = repository.getTasksForDate(task.date).firstOrNull() ?: emptyList()
            val maxPos = currentTasks.maxOfOrNull { it.position } ?: -1
            val newTaskWithPos = task.copy(position = maxPos + 1)
            
            val id = repository.insertTask(newTaskWithPos)
            val finalTask = newTaskWithPos.copy(id = id.toInt())
            if (finalTask.reminderEnabled && finalTask.time.isNotEmpty()) {
                ReminderScheduler.scheduleReminder(getApplication(), finalTask)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            if (task.status == TaskStatus.COMPLETED.name) {
                ReminderScheduler.cancelReminder(getApplication(), task.id)
            } else if (task.reminderEnabled && task.time.isNotEmpty()) {
                ReminderScheduler.scheduleReminder(getApplication(), task)
            } else {
                ReminderScheduler.cancelReminder(getApplication(), task.id)
            }
        }
    }

    fun updateTaskOrder(tasks: List<Task>) {
        viewModelScope.launch {
            val updatedTasks = tasks.mapIndexed { index, task ->
                task.copy(position = index)
            }
            repository.updateTasks(updatedTasks)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            ReminderScheduler.cancelReminder(getApplication(), task.id)
        }
    }

    fun carryForwardTasks(oldDate: String) {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.carryForwardTasks(oldDate, today)
            dataStoreManager.clearCarryForward()
        }
    }

    fun dismissCarryForward() {
        viewModelScope.launch {
            dataStoreManager.clearCarryForward()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStoreManager.setOnboardingCompleted()
        }
    }
}

class TaskViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application, repository, dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
