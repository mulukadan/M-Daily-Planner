package com.example.m_dailyplanner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.m_dailyplanner.data.CATEGORY_COLORS
import com.example.m_dailyplanner.data.Habit
import com.example.m_dailyplanner.ui.theme.extendedColors
import com.example.m_dailyplanner.viewmodel.HabitViewModel
import com.example.m_dailyplanner.viewmodel.HabitWithStreak

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitViewModel,
    onOpenDrawer: () -> Unit = {}
) {
    val habits by viewModel.habitsWithStreaks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Habits", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Habit")
            }
        }
    ) { padding ->
        if (habits.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Whatshot,
                title = "No habits yet",
                subtitle = "Tap + to start building a streak",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(habits, key = { it.habit.id }) { entry ->
                    HabitCard(
                        entry = entry,
                        onToggleToday = { viewModel.toggleToday(entry.habit.id) },
                        onEdit = { name, color -> viewModel.updateHabit(entry.habit.copy(name = name, color = color)) },
                        onDelete = { viewModel.deleteHabit(entry.habit) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditHabitDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                viewModel.addHabit(name, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun HabitCard(
    entry: HabitWithStreak,
    onToggleToday: () -> Unit,
    onEdit: (name: String, color: String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val habitColor = remember(entry.habit.color) {
        runCatching { Color(android.graphics.Color.parseColor(entry.habit.color)) }.getOrDefault(Color(0xFF005AC1))
    }
    val checkScale by animateFloatAsState(if (entry.completedToday) 1.1f else 1f, label = "habitCheckScale")

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit") },
            text = { Text("Delete '${entry.habit.name}' and its whole history? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showEditDialog) {
        AddEditHabitDialog(
            initial = entry.habit,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, color -> onEdit(name, color); showEditDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(habitColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (entry.currentStreak > 0) MaterialTheme.extendedColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (entry.currentStreak > 0) "${entry.currentStreak}-day streak" else "No streak yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.bestStreak > entry.currentStreak) {
                        Text(
                            text = "· best ${entry.bestStreak}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleToday()
                }
            ) {
                Icon(
                    imageVector = if (entry.completedToday) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (entry.completedToday) "Mark not done today" else "Mark done today",
                    tint = if (entry.completedToday) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp).scale(checkScale)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
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
                                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
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
private fun AddEditHabitDialog(
    initial: Habit?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: CATEGORY_COLORS.first()) }
    val isEdit = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Habit" else "New Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g. Drink water, Read 10 pages") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CATEGORY_COLORS.take(8).forEach { hex ->
                        val swatch = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .clickable { color = hex }
                                .then(
                                    if (color == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), color) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (isEdit) "Save" else "Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
