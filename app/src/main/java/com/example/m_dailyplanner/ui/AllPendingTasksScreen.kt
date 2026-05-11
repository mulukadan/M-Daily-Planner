package com.example.m_dailyplanner.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.m_dailyplanner.data.Task
import com.example.m_dailyplanner.viewmodel.TaskViewModel
import java.time.LocalDate

private enum class PendingSortMode { PRIORITY, TIME }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllPendingTasksScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit,
    onTaskClick: (Int) -> Unit
) {
    val tasks by viewModel.pendingTasksByPriority.collectAsState()
    var sortMode by remember { mutableStateOf(PendingSortMode.PRIORITY) }
    val today = remember { LocalDate.now() }

    val groupedTasks: Map<String, List<Task>> = remember(tasks, sortMode, today) {
        when (sortMode) {
            PendingSortMode.PRIORITY -> linkedMapOf(
                "HIGH" to tasks.filter { it.priority.uppercase() == "HIGH" },
                "MEDIUM" to tasks.filter { it.priority.uppercase() == "MEDIUM" },
                "LOW" to tasks.filter { it.priority.uppercase() == "LOW" }
            ).filter { it.value.isNotEmpty() }

            PendingSortMode.TIME -> {
                val timeSorted = tasks.sortedWith(compareBy(
                    { it.date },
                    { it.time.ifEmpty { "23:59" } }
                ))
                linkedMapOf(
                    "OVERDUE" to timeSorted.filter {
                        runCatching { LocalDate.parse(it.date).isBefore(today) }.getOrDefault(false)
                    },
                    "TODAY" to timeSorted.filter {
                        runCatching { LocalDate.parse(it.date).isEqual(today) }.getOrDefault(false)
                    },
                    "UPCOMING" to timeSorted.filter {
                        runCatching { LocalDate.parse(it.date).isAfter(today) }.getOrDefault(false)
                    }
                ).filter { it.value.isNotEmpty() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "All Pending Tasks",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (tasks.isNotEmpty()) {
                            Text(
                                text = "${tasks.size} task${if (tasks.size != 1) "s" else ""} remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SegmentedButton(
                    selected = sortMode == PendingSortMode.PRIORITY,
                    onClick = { sortMode = PendingSortMode.PRIORITY },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Priority") }
                )
                SegmentedButton(
                    selected = sortMode == PendingSortMode.TIME,
                    onClick = { sortMode = PendingSortMode.TIME },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Time") }
                )
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "All caught up!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No pending tasks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    groupedTasks.forEach { (group, groupTasks) ->
                        stickyHeader(key = "header_$group") {
                            if (sortMode == PendingSortMode.PRIORITY) {
                                PriorityGroupHeader(priority = group, count = groupTasks.size)
                            } else {
                                DateGroupHeader(group = group, count = groupTasks.size)
                            }
                        }
                        items(groupTasks, key = { it.id }) { task ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TaskItem(
                                    task = task,
                                    onStatusChange = { status ->
                                        viewModel.updateTask(task.copy(status = status.name))
                                    },
                                    onDelete = { viewModel.deleteTask(task) },
                                    onTaskClick = { onTaskClick(task.id) },
                                    onMoveToDate = { date ->
                                        viewModel.updateTask(task.copy(date = date))
                                    },
                                    showDate = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityGroupHeader(priority: String, count: Int) {
    val color = when (priority.uppercase()) {
        "HIGH" -> Color(0xFFD32F2F)
        "MEDIUM" -> Color(0xFFF57C00)
        "LOW" -> Color(0xFF388E3C)
        else -> MaterialTheme.colorScheme.outline
    }
    PendingSectionHeader(
        label = priority.replaceFirstChar { it.titlecase() },
        count = count,
        color = color
    )
}

@Composable
fun DateGroupHeader(group: String, count: Int) {
    val label: String
    val color: Color
    when (group) {
        "OVERDUE" -> { label = "Overdue"; color = MaterialTheme.colorScheme.error }
        "TODAY" -> { label = "Today"; color = MaterialTheme.colorScheme.primary }
        "UPCOMING" -> { label = "Upcoming"; color = Color(0xFF388E3C) }
        else -> { label = group; color = MaterialTheme.colorScheme.outline }
    }
    PendingSectionHeader(label = label, count = count, color = color)
}

@Composable
private fun PendingSectionHeader(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.2f)
        )
    }
}
