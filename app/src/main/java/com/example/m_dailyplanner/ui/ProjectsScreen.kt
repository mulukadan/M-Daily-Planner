package com.example.m_dailyplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.m_dailyplanner.data.DEFAULT_PROJECT_COLOR
import com.example.m_dailyplanner.data.PROJECT_COLORS
import com.example.m_dailyplanner.data.Project
import com.example.m_dailyplanner.data.ProjectWithStats
import com.example.m_dailyplanner.viewmodel.ProjectViewModel
import java.text.SimpleDateFormat
import java.util.*

private fun parseProjectColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF6750A4.toInt())
}

// ── Drag-drop state ────────────────────────────────────────────────────────────

private class DragDropState(
    val listState: androidx.compose.foundation.lazy.LazyListState,
    private val onSwap: (Int, Int) -> Unit
) {
    var draggingIndex by mutableStateOf<Int?>(null)
        private set
    var dragOffset by mutableStateOf(0f)
        private set

    fun onDragStart(index: Int) {
        draggingIndex = index
    }

    fun onDragEnd() {
        draggingIndex = null
        dragOffset = 0f
    }

    fun onDrag(deltaY: Float) {
        dragOffset += deltaY
        val current = draggingIndex ?: return
        val currentInfo = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == current } ?: return
        val centerY = currentInfo.offset + currentInfo.size / 2f + dragOffset
        val target = listState.layoutInfo.visibleItemsInfo.find { item ->
            item.index != current && centerY.toInt() in item.offset..(item.offset + item.size)
        } ?: return
        onSwap(current, target.index)
        draggingIndex = target.index
        dragOffset = 0f
    }
}

@Composable
private fun rememberDragDropState(
    listState: androidx.compose.foundation.lazy.LazyListState,
    localProjects: SnapshotStateList<ProjectWithStats>
): DragDropState = remember(listState) {
    DragDropState(listState) { from, to ->
        val item = localProjects.removeAt(from)
        localProjects.add(to, item)
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (Int) -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    val projects by viewModel.projects.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val localProjects = remember { mutableStateListOf<ProjectWithStats>() }
    val dragState = rememberDragDropState(listState, localProjects)

    LaunchedEffect(projects) {
        if (dragState.draggingIndex == null) {
            localProjects.clear()
            localProjects.addAll(projects)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Projects",
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
                Icon(Icons.Default.Add, contentDescription = "New Project")
            }
        }
    ) { padding ->
        if (localProjects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        "No projects yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to create your first project",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(localProjects, key = { it.id }) { project ->
                    val index = localProjects.indexOfFirst { it.id == project.id }
                    val dragHandleModifier = Modifier.pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragState.onDragStart(index) },
                            onDragEnd = {
                                dragState.onDragEnd()
                                viewModel.updateProjectOrder(localProjects.toList())
                            },
                            onDragCancel = { dragState.onDragEnd() },
                            onDrag = { _, amount -> dragState.onDrag(amount.y) }
                        )
                    }
                    ProjectCard(
                        modifier = Modifier.animateItem(),
                        project = project,
                        dragHandleModifier = dragHandleModifier,
                        onTap = { onProjectClick(project.id) },
                        onEdit = { viewModel.updateProject(it) },
                        onDelete = {
                            viewModel.deleteProject(
                                Project(
                                    id = project.id,
                                    name = project.name,
                                    description = project.description,
                                    color = project.color,
                                    position = project.position,
                                    createdAt = project.createdAt
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditProjectDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc, color ->
                viewModel.addProject(Project(name = name, description = desc, color = color))
                showAddDialog = false
            }
        )
    }
}

// ── Project card ───────────────────────────────────────────────────────────────

@Composable
private fun ProjectCard(
    modifier: Modifier = Modifier,
    project: ProjectWithStats,
    dragHandleModifier: Modifier = Modifier,
    onTap: () -> Unit,
    onEdit: (Project) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val progress = if (project.totalTasks > 0)
        project.completedTasks.toFloat() / project.totalTasks else 0f

    val createdDate = remember(project.createdAt) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(project.createdAt))
    }

    val accentColor = parseProjectColor(project.color)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Project") },
            text = { Text("Delete '${project.name}' and all its tasks? This cannot be undone.") },
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
        AddEditProjectDialog(
            initial = Triple(project.name, project.description, project.color),
            onDismiss = { showEditDialog = false },
            onConfirm = { name, desc, color ->
                onEdit(
                    Project(
                        id = project.id,
                        name = name,
                        description = desc,
                        color = color,
                        position = project.position,
                        createdAt = project.createdAt
                    )
                )
                showEditDialog = false
            }
        )
    }

    Card(
        onClick = onTap,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (project.description.isNotBlank()) {
                            Text(
                                text = project.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .then(dragHandleModifier)
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = { showEditDialog = true; showMenu = false }
                            )
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
                                onClick = { showDeleteConfirm = true; showMenu = false },
                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            createdDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${project.completedTasks}/${project.totalTasks} tasks",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (project.totalTasks == 0) "No tasks yet"
                               else "${(progress * 100).toInt()}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// ── Dialog ─────────────────────────────────────────────────────────────────────

@Composable
private fun AddEditProjectDialog(
    initial: Triple<String, String, String>? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.first ?: "") }
    var description by remember { mutableStateOf(initial?.second ?: "") }
    var selectedColor by remember { mutableStateOf(initial?.third ?: DEFAULT_PROJECT_COLOR) }
    val isEdit = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Project" else "New Project") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
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

                Text("Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                val rows = PROJECT_COLORS.chunked(6)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowColors.forEach { color ->
                                val isSelected = color == selectedColor
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(parseProjectColor(color))
                                        .clickable { selectedColor = color }
                                        .then(
                                            if (isSelected) Modifier.border(
                                                width = 3.dp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = CircleShape
                                            ) else Modifier
                                        )
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), description.trim(), selectedColor) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (isEdit) "Save" else "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
