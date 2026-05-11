package com.example.m_dailyplanner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.m_dailyplanner.data.Task
import com.example.m_dailyplanner.data.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit,
    onTaskClick: () -> Unit,
    onMoveToDate: (String) -> Unit = {},
    enableLongClick: Boolean = true,
    showDragHandle: Boolean = false,
    showDate: Boolean = false
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isHighlighted by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete '${task.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isDone = task.status == TaskStatus.COMPLETED.name
    val priorityColor = when (task.priority.lowercase()) {
        "high" -> Color(0xFFD32F2F)
        "medium" -> Color(0xFFF57C00)
        "low" -> Color(0xFF388E3C)
        else -> MaterialTheme.colorScheme.outline
    }

    val (dateLabel, dateCategory) = remember(task.date) {
        val taskDate = runCatching { LocalDate.parse(task.date) }.getOrNull()
        val today = LocalDate.now()
        val label = when (taskDate) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> taskDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy")) ?: task.date
        }
        val category = when {
            taskDate == null -> "neutral"
            taskDate.isBefore(today) -> "past"
            taskDate.isEqual(today) -> "today"
            else -> "future"
        }
        label to category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enableLongClick) {
                    Modifier.combinedClickable(
                        onClick = onTaskClick,
                        onLongClick = { isHighlighted = !isHighlighted }
                    )
                } else {
                    // Simple clickable lets long-press propagate to the parent LazyColumn
                    // for the drag-after-long-press gesture to work.
                    Modifier.clickable(onClick = onTaskClick)
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(priorityColor)
            )

            Checkbox(
                checked = isDone,
                onCheckedChange = { checked ->
                    val newStatus = if (checked) TaskStatus.COMPLETED else TaskStatus.PENDING
                    onStatusChange(newStatus)
                },
                modifier = Modifier.padding(start = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (task.time.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.time,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (showDate) {
                    val dateTint = when (dateCategory) {
                        "past" -> MaterialTheme.colorScheme.error
                        "today" -> MaterialTheme.colorScheme.primary
                        "future" -> Color(0xFF388E3C)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = dateTint
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = dateTint
                        )
                    }
                }
            }

            if (showDragHandle) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Box(modifier = Modifier.padding(end = 4.dp)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onTaskClick()
                            showMenu = false
                        }
                    )
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

                    if (task.date != today) {
                        DropdownMenuItem(
                            text = { Text("Move to today") },
                            leadingIcon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onMoveToDate(today)
                                showMenu = false
                            }
                        )
                    }
                    if (task.date != tomorrow) {
                        DropdownMenuItem(
                            text = { Text("Move to tomorrow") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onMoveToDate(tomorrow)
                                showMenu = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showDeleteConfirmDialog = true
                            showMenu = false
                        },
                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}

@Composable
fun ReorderableTaskList(
    tasksFlow: List<Task>,
    dragEnabled: Boolean,
    modifier: Modifier = Modifier,
    onOrderChanged: (List<Task>) -> Unit,
    onStatusChange: (Task, TaskStatus) -> Unit,
    onDelete: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onMoveToDate: (Task, String) -> Unit
) {
    val tasks = remember { mutableStateListOf<Task>() }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()

    LaunchedEffect(tasksFlow) {
        if (draggedItemIndex == null) {
            tasks.clear()
            tasks.addAll(tasksFlow)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .pointerInput(dragEnabled) {
                if (!dragEnabled) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        listState.layoutInfo.visibleItemsInfo
                            .find { item ->
                                offset.y.toInt() in item.offset..(item.offset + item.size)
                            }
                            ?.let { draggedItemIndex = it.index }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y

                        draggedItemIndex?.let { currentIndex ->
                            val targetIndex = listState.layoutInfo.visibleItemsInfo
                                .find { item ->
                                    change.position.y.toInt() in item.offset..(item.offset + item.size)
                                }
                                ?.index

                            if (targetIndex != null && targetIndex != currentIndex && targetIndex < tasks.size) {
                                tasks.add(targetIndex, tasks.removeAt(currentIndex))
                                draggedItemIndex = targetIndex
                                dragOffset = 0f
                            }
                        }
                    },
                    onDragEnd = {
                        onOrderChanged(tasks.toList())
                        draggedItemIndex = null
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        draggedItemIndex = null
                        dragOffset = 0f
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
    ) {
        itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
            val isDragging = index == draggedItemIndex
            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        scaleX = scale
                        scaleY = scale
                    }
                    .zIndex(if (isDragging) 1f else 0f)
            ) {
                TaskItem(
                    task = task,
                    onStatusChange = { newStatus -> onStatusChange(task, newStatus) },
                    onDelete = { onDelete(task) },
                    onTaskClick = { onTaskClick(task) },
                    onMoveToDate = { newDate -> onMoveToDate(task, newDate) },
                    enableLongClick = !dragEnabled,
                    showDragHandle = dragEnabled
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var reminderEnabled by remember { mutableStateOf(false) }

    val priorities = listOf("High", "Medium", "Low")

    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { },
                    label = { Text("Time") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    enabled = false,
                    trailingIcon = {
                        Row {
                            if (time.isNotEmpty()) {
                                IconButton(onClick = {
                                    time = ""
                                    reminderEnabled = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Time")
                                }
                            }
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Select Time")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorities.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                if (time.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                        Text("Enable Alarm")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description, time, priority, reminderEnabled)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatusBadge(status: TaskStatus) {
    val backgroundColor = when (status) {
        TaskStatus.COMPLETED -> Color(0xFFE8F5E9)
        TaskStatus.IN_PROGRESS -> Color(0xFFE3F2FD)
        TaskStatus.PENDING -> Color(0xFFFFF3E0)
    }
    val contentColor = when (status) {
        TaskStatus.COMPLETED -> Color(0xFF2E7D32)
        TaskStatus.IN_PROGRESS -> Color(0xFF1565C0)
        TaskStatus.PENDING -> Color(0xFFEF6C00)
    }

    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = CircleShape,
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = iconColor.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun getPriorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "high" -> Color(0xFFD32F2F)
        "medium" -> Color(0xFFF57C00)
        "low" -> Color(0xFF388E3C)
        else -> MaterialTheme.colorScheme.primary
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var description by remember { mutableStateOf(task.description) }
    var priority by remember { mutableStateOf(task.priority) }
    var reminderEnabled by remember { mutableStateOf(task.reminderEnabled) }
    var time by remember { mutableStateOf(task.time) }
    var date by remember { mutableStateOf(task.date) }

    val priorities = listOf("High", "Medium", "Low")
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val timeParts = remember(task.time) {
        if (task.time.isNotEmpty()) task.time.split(":") else null
    }
    val timePickerState = rememberTimePickerState(
        initialHour = timeParts?.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = timeParts?.getOrNull(1)?.toIntOrNull() ?: calendar.get(Calendar.MINUTE)
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }.getOrNull()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    label = { Text("Time") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    enabled = false,
                    trailingIcon = {
                        Row {
                            if (time.isNotEmpty()) {
                                IconButton(onClick = {
                                    time = ""
                                    reminderEnabled = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Time")
                                }
                            }
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Select Time")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Priority", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorities.forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                if (time.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                        Text("Enable Alarm")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        task.copy(
                            name = name,
                            description = description,
                            date = date,
                            time = time,
                            priority = priority,
                            reminderEnabled = reminderEnabled
                        )
                    )
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Update Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
