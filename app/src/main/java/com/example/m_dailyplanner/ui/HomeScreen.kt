package com.example.m_dailyplanner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.m_dailyplanner.data.Task
import com.example.m_dailyplanner.data.TaskStatus
import com.example.m_dailyplanner.ui.util.rememberCurrentDate
import com.example.m_dailyplanner.viewmodel.SortOption
import com.example.m_dailyplanner.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DotColorAllDone = Color(0xFF4CAF50)
private val DotColorPartial = Color(0xFFFFA726)
private val DotColorNoneDone = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onDayClick: (String) -> Unit = {},
    onTaskClick: (Int) -> Unit = {},
    onNavigateToPending: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val tasksFlow by viewModel.filteredTasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val carryForwardEvent by viewModel.carryForwardEvent.collectAsState()
    val pendingCount by viewModel.pendingTasksByPriority.collectAsState()

    var showSortSheet by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var isCarryingForward by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val today by rememberCurrentDate()
    val days = remember(today) { (-30..30).map { offset -> today.plusDays(offset.toLong()) } }

    LaunchedEffect(today) { viewModel.refreshToday() }

    // Reset the in-flight flag whenever the event clears (accept or dismiss), so a later,
    // unrelated carry-forward prompt doesn't open straight into a stuck loading spinner.
    LaunchedEffect(carryForwardEvent) {
        if (carryForwardEvent == null) isCarryingForward = false
    }

    if (carryForwardEvent != null) {
        AlertDialog(
            onDismissRequest = { if (!isCarryingForward) viewModel.dismissCarryForward() },
            title = { Text("Unfinished Tasks") },
            text = {
                val count = carryForwardEvent!!.count
                Text("You have $count unfinished task${if (count != 1) "s" else ""} from previous days. Carry ${if (count != 1) "them" else "it"} forward to today?")
            },
            confirmButton = {
                if (isCarryingForward) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                } else {
                    TextButton(onClick = {
                        isCarryingForward = true
                        viewModel.carryForwardTasks()
                    }) {
                        Text("Yes")
                    }
                }
            },
            dismissButton = {
                if (!isCarryingForward) {
                    TextButton(onClick = { viewModel.dismissCarryForward() }) {
                        Text("No")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "M-Daily Planner",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open menu",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (pendingCount.isNotEmpty()) {
                                Badge { Text(pendingCount.size.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToPending) {
                            Icon(
                                imageVector = Icons.Outlined.PendingActions,
                                contentDescription = "View pending tasks",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            WeeklyStrip(
                today = today,
                days = days,
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) },
                allTasks = allTasks
            )

            DailyProgressView(tasks = tasksFlow)

            Box(modifier = Modifier.weight(1f)) {
                if (tasksFlow.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.PendingActions,
                        title = "No tasks for this day",
                        subtitle = "Tap + to add your first task"
                    )
                } else {
                    ReorderableTaskList(
                        tasksFlow = tasksFlow,
                        dragEnabled = sortOption == SortOption.MANUAL,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        onOrderChanged = { viewModel.updateTaskOrder(it) },
                        onStatusChange = { task, status ->
                            viewModel.updateTask(task.copy(status = status.name))
                        },
                        onDelete = { viewModel.deleteTask(it) },
                        onTaskClick = { onTaskClick(it.id) },
                        onMoveToDate = { task, date -> viewModel.updateTask(task.copy(date = date)) }
                    )
                }
            }

        }

        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                sheetState = sheetState
            ) {
                SortOptionsContent(
                    selectedOption = sortOption,
                    onOptionSelected = {
                        viewModel.setSortOption(it)
                        showSortSheet = false
                    }
                )
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                defaultDate = runCatching { LocalDate.parse(selectedDate) }.getOrDefault(today),
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { name, description, date, time, priority, reminderEnabled ->
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

@Composable
fun DailyProgressView(tasks: List<Task>) {
    if (tasks.isEmpty()) return

    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED.name }
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val percentage = (progress * 100).toInt()
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "dailyProgress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedTasks/$totalTasks ($percentage%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun WeeklyStrip(
    today: LocalDate,
    days: List<LocalDate>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    allTasks: List<Task> = emptyList()
) {
    val todayIndex = remember(days, today) {
        days.indexOfFirst { it.isEqual(today) }.coerceAtLeast(0)
    }

    val taskStatusByDate = remember(allTasks) {
        allTasks.groupBy { it.date }.mapValues { (_, tasks) ->
            when {
                tasks.all { it.status == TaskStatus.COMPLETED.name } -> DotColorAllDone
                tasks.any { it.status == TaskStatus.COMPLETED.name } -> DotColorPartial
                else -> DotColorNoneDone
            }
        }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (todayIndex - 3).coerceAtLeast(0)
    )

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { date ->
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val isSelected = dateString == selectedDate
            val dotColor = taskStatusByDate[dateString]

            Column(
                modifier = Modifier
                    .width(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onDateSelected(dateString) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMM")),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEE")),
                    fontSize = 10.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.size(6.dp)) {
                    if (dotColor != null) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.85f) else dotColor
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortOptionsContent(
    selectedOption: SortOption,
    onOptionSelected: (SortOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Sort By",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        SortOption.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = option.displayName)
            }
        }
    }
}
