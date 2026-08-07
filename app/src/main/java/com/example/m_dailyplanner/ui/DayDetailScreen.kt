package com.example.m_dailyplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.m_dailyplanner.data.Task
import com.example.m_dailyplanner.data.TaskStatus
import com.example.m_dailyplanner.viewmodel.SortOption
import com.example.m_dailyplanner.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: String,
    viewModel: TaskViewModel,
    onBack: () -> Unit,
    onTaskClick: (Int) -> Unit = {}
) {
    val tasksFlow by viewModel.getTasksForDate(date).collectAsState(initial = emptyList())
    val sortOption by viewModel.sortOption.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val formattedDate = remember(date) {
        val localDate = LocalDate.parse(date)
        localDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = formattedDate, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        if (tasksFlow.isEmpty()) {
            EmptyState(
                icon = Icons.Default.EventAvailable,
                title = "No tasks for this day",
                subtitle = "Tap + to add a task",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            ReorderableTaskList(
                tasksFlow = tasksFlow,
                dragEnabled = sortOption == SortOption.MANUAL,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                onOrderChanged = { viewModel.updateTaskOrder(it) },
                onStatusChange = { task, status ->
                    viewModel.updateTask(task.copy(status = status.name))
                },
                onDelete = { viewModel.deleteTask(it) },
                onTaskClick = { onTaskClick(it.id) },
                onMoveToDate = { task, newDate -> viewModel.updateTask(task.copy(date = newDate)) }
            )
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                defaultDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { name, description, taskDate, time, priority, reminderEnabled ->
                    viewModel.addTask(
                        Task(
                            name = name,
                            description = description,
                            date = taskDate,
                            time = time,
                            priority = priority,
                            reminderEnabled = reminderEnabled,
                            status = TaskStatus.PENDING.name
                        )
                    )
                    showAddTaskDialog = false
                }
            )
        }
    }
}
