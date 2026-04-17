package com.example.m_dailyplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text(text = formattedDate) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks for this day",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { name, description, time, priority, reminderEnabled ->
                    viewModel.addTask(
                        Task(
                            name = name,
                            description = description,
                            date = date,
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
