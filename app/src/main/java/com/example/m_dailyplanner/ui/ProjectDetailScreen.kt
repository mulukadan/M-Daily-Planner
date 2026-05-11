package com.example.m_dailyplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.m_dailyplanner.data.ProjectTask
import com.example.m_dailyplanner.data.TaskStatus
import com.example.m_dailyplanner.viewmodel.ProjectViewModel

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (total > 0) {
                            Text(
                                text = "$completed/$total tasks done",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
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
                            progress = { progress },
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        ProjectTaskItem(
                            task = task,
                            onStatusChange = { status ->
                                viewModel.updateTask(task.copy(status = status.name))
                            },
                            onEdit = { viewModel.updateTask(it) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
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
private fun ProjectTaskItem(
    task: ProjectTask,
    onStatusChange: (TaskStatus) -> Unit,
    onEdit: (ProjectTask) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isDone = task.status == TaskStatus.COMPLETED.name
    val priorityColor = when (task.priority.lowercase()) {
        "high" -> Color(0xFFD32F2F)
        "medium" -> Color(0xFFF57C00)
        "low" -> Color(0xFF388E3C)
        else -> MaterialTheme.colorScheme.outline
    }

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
