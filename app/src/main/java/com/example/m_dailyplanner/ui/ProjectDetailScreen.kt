package com.example.m_dailyplanner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.m_dailyplanner.data.ProjectTask
import com.example.m_dailyplanner.data.TaskStatus
import com.example.m_dailyplanner.viewmodel.ProjectViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Int,
    projectName: String,
    viewModel: ProjectViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(projectId) { viewModel.selectProject(projectId) }

    val tasks by viewModel.projectTasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val total = tasks.size
    val completed = tasks.count { it.status == TaskStatus.COMPLETED.name }
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "projectProgress")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (total > 0) {
                            Text(
                                text = "$completed/$total tasks done",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (total > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("$completed/$total (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall)
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Text("No tasks yet", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap + to add a task", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                ReorderableProjectTaskList(
                    tasksFlow = tasks,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    onOrderChanged = { viewModel.updateTaskOrder(it) },
                    onStatusChange = { task, status ->
                        viewModel.updateTask(task.copy(status = status.name))
                    },
                    onEdit = { viewModel.updateTask(it) },
                    onDelete = { viewModel.deleteTask(it) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddProjectTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc, priority ->
                viewModel.addTask(
                    ProjectTask(
                        projectId = projectId,
                        name = name,
                        description = desc,
                        priority = priority
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ReorderableProjectTaskList(
    tasksFlow: List<ProjectTask>,
    modifier: Modifier = Modifier,
    onOrderChanged: (List<ProjectTask>) -> Unit,
    onStatusChange: (ProjectTask, TaskStatus) -> Unit,
    onEdit: (ProjectTask) -> Unit,
    onDelete: (ProjectTask) -> Unit
) {
    val tasks = remember { mutableStateListOf<ProjectTask>() }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val edgeScrollPx = with(density) { 56.dp.toPx() }

    LaunchedEffect(tasksFlow) {
        if (draggedItemIndex == null) {
            tasks.clear()
            tasks.addAll(tasksFlow)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        listState.layoutInfo.visibleItemsInfo
                            .find { item ->
                                offset.y.toInt() in item.offset..(item.offset + item.size)
                            }
                            ?.let {
                                draggedItemIndex = it.index
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
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

                        // Auto-scroll the list when dragging near the top/bottom edge, so a task
                        // can be reordered into a position that's currently off-screen.
                        val viewportHeight = listState.layoutInfo.viewportSize.height
                        val y = change.position.y
                        when {
                            y < edgeScrollPx -> coroutineScope.launch { listState.scrollBy(-24f) }
                            y > viewportHeight - edgeScrollPx ->
                                coroutineScope.launch { listState.scrollBy(24f) }
                        }
                    },
                    onDragEnd = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
            val isDragging = index == draggedItemIndex
            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "projectTaskDragScale")

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
                ProjectTaskItem(
                    task = task,
                    onStatusChange = { status -> onStatusChange(task, status) },
                    onEdit = onEdit,
                    onDelete = { onDelete(task) },
                    showDragHandle = true
                )
            }
        }
    }
}

@Composable
private fun ProjectTaskItem(
    task: ProjectTask,
    onStatusChange: (TaskStatus) -> Unit,
    onEdit: (ProjectTask) -> Unit,
    onDelete: () -> Unit,
    showDragHandle: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val isDone = task.status == TaskStatus.COMPLETED.name
    val priorityColor = getPriorityColor(task.priority)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Task") },
            text = { Text("Delete '${task.name}'? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog) {
        AddProjectTaskDialog(
            initial = Triple(task.name, task.description, task.priority),
            onDismiss = { showEditDialog = false },
            onConfirm = { name, desc, priority ->
                onEdit(task.copy(name = name, description = desc, priority = priority))
                showEditDialog = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.fillMaxHeight().width(6.dp).background(priorityColor)
            )
            Checkbox(
                checked = isDone,
                onCheckedChange = { checked ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStatusChange(if (checked) TaskStatus.COMPLETED else TaskStatus.PENDING)
                },
                modifier = Modifier.padding(start = 8.dp)
            )
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
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
                    Icon(Icons.Default.MoreVert, contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = { showEditDialog = true; showMenu = false }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { showDeleteConfirm = true; showMenu = false },
                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddProjectTaskDialog(
    initial: Triple<String, String, String>? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, priority: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.first ?: "") }
    var description by remember { mutableStateOf(initial?.second ?: "") }
    var priority by remember { mutableStateOf(initial?.third ?: "Medium") }
    val isEdit = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Task" else "Add Task") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), description.trim(), priority) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (isEdit) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
