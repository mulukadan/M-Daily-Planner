package com.example.m_dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.data.*
import com.example.m_dailyplanner.notification.ReminderScheduler
import com.example.m_dailyplanner.widget.TodayTasksWidget
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

    // True once the user has explicitly picked a date, so refreshToday() stops
    // auto-advancing the default "today" selection across a midnight rollover.
    private var userSelectedDate = false

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
                    { TaskPriority.fromLabel(it.priority).ordinal },
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
        val sorted = when (sort) {
            SortOption.PRIORITY -> filtered.sortedBy { TaskPriority.fromLabel(it.priority).ordinal }
            SortOption.TIME -> filtered.sortedBy { it.time.ifEmpty { "23:59" } }
            SortOption.CREATION_DATE -> filtered.sortedByDescending { it.createdAt }
            SortOption.MANUAL -> filtered.sortedBy { it.position }
        }
        // Stable sort: completed tasks sink to bottom, relative order within each group preserved
        sorted.sortedBy { if (it.status == TaskStatus.COMPLETED.name) 1 else 0 }
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
        userSelectedDate = true
        _selectedDate.value = date
    }

    // Called whenever the UI observes a (possibly new) current date, e.g. on resume or at
    // midnight. Only advances the default "today" selection if the user hasn't manually
    // navigated to a specific date, so it never overrides an intentional selection.
    fun refreshToday() {
        if (!userSelectedDate) {
            _selectedDate.value = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun getTasksForDate(date: String): Flow<List<Task>> =
        repository.getTasksForDate(date)

    fun getTaskById(taskId: Int): Flow<Task?> = repository.getTaskByIdFlow(taskId)

    // Best-effort — the widget also self-refreshes on its periodic update, so a failure
    // here (e.g. no widget currently placed) should never break the underlying task action.
    private suspend fun refreshWidget() {
        val context = getApplication<Application>()
        try {
            TodayTasksWidget().updateAll(context)
        } catch (e: Exception) {
            // Best-effort — see the comment above.
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            val finalTask = repository.insertTask(task)
            if (finalTask.reminderEnabled && finalTask.time.isNotEmpty()) {
                ReminderScheduler.scheduleReminder(getApplication(), finalTask)
            }
            refreshWidget()
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
            refreshWidget()
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
            refreshWidget()
        }
    }

    fun carryForwardTasks() {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.carryForwardAllPending(today)
            dataStoreManager.clearCarryForward()
            refreshWidget()
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
