package com.example.m_dailyplanner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.m_dailyplanner.data.ProjectWithStats
import com.example.m_dailyplanner.data.Task
import com.example.m_dailyplanner.data.TaskStatus
import com.example.m_dailyplanner.ui.theme.extendedColors
import com.example.m_dailyplanner.ui.util.rememberCurrentDate
import com.example.m_dailyplanner.viewmodel.HabitViewModel
import com.example.m_dailyplanner.viewmodel.HabitWithStreak
import com.example.m_dailyplanner.viewmodel.ProjectViewModel
import com.example.m_dailyplanner.viewmodel.TaskViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    taskViewModel: TaskViewModel,
    habitViewModel: HabitViewModel,
    projectViewModel: ProjectViewModel,
    userName: String?,
    onOpenDrawer: () -> Unit = {},
    onTaskClick: (Int) -> Unit = {},
    onProjectClick: (Int) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToPending: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {}
) {
    val allTasks by taskViewModel.allTasks.collectAsState()
    val habits by habitViewModel.habitsWithStreaks.collectAsState()
    val projects by projectViewModel.projects.collectAsState()
    val today by rememberCurrentDate()
    val todayStr = remember(today) { today.format(DateTimeFormatter.ISO_LOCAL_DATE) }

    val todaysTasks = remember(allTasks, todayStr) { allTasks.filter { it.date == todayStr } }
    val completedToday = todaysTasks.count { it.status == TaskStatus.COMPLETED.name }
    val todayProgress = if (todaysTasks.isEmpty()) 0f else completedToday.toFloat() / todaysTasks.size

    val pendingCount = remember(allTasks) { allTasks.count { it.status != TaskStatus.COMPLETED.name } }

    val upcomingTasks = remember(allTasks, todayStr) {
        allTasks
            .filter { it.status != TaskStatus.COMPLETED.name && it.date >= todayStr }
            .sortedWith(compareBy({ it.date }, { it.time.ifEmpty { "23:59" } }))
            .take(5)
    }

    val bestActiveStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
    val habitsDoneToday = habits.count { it.completedToday }

    val isBrandNew = allTasks.isEmpty() && habits.isEmpty() && projects.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        if (isBrandNew) {
            EmptyState(
                icon = Icons.Default.WavingHand,
                title = "Welcome to M-Daily Planner",
                subtitle = "Add your first task, habit, or project to see your dashboard come alive",
                modifier = Modifier.padding(padding)
            )
        } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { GreetingHeader(userName = userName) }

            item {
                TodayProgressCard(
                    progress = todayProgress,
                    completed = completedToday,
                    total = todaysTasks.size,
                    onClick = onNavigateToTasks
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.PendingActions,
                        value = pendingCount.toString(),
                        label = "Pending",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToPending
                    )
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Whatshot,
                        value = bestActiveStreak.toString(),
                        label = "Best streak",
                        color = MaterialTheme.extendedColors.warning,
                        onClick = onNavigateToHabits
                    )
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Folder,
                        value = projects.size.toString(),
                        label = "Projects",
                        color = MaterialTheme.extendedColors.success,
                        onClick = onNavigateToProjects
                    )
                }
            }

            if (habits.isNotEmpty()) {
                item {
                    DashboardSectionHeader(
                        title = "Today's Habits",
                        subtitle = "$habitsDoneToday/${habits.size} done",
                        onSeeAll = onNavigateToHabits
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        habits.take(4).forEach { entry ->
                            HabitMiniChip(entry = entry, onClick = { habitViewModel.toggleToday(entry.habit.id) })
                        }
                    }
                }
            }

            if (upcomingTasks.isNotEmpty()) {
                item {
                    DashboardSectionHeader(
                        title = "Upcoming Tasks",
                        subtitle = null,
                        onSeeAll = onNavigateToPending
                    )
                }
                items(upcomingTasks, key = { "task_${it.id}" }) { task ->
                    UpcomingTaskRow(task = task, today = todayStr, onClick = { onTaskClick(task.id) })
                }
            }

            if (projects.isNotEmpty()) {
                item {
                    DashboardSectionHeader(
                        title = "Projects",
                        subtitle = null,
                        onSeeAll = onNavigateToProjects
                    )
                }
                items(projects.take(3), key = { "project_${it.id}" }) { project ->
                    ProjectOverviewRow(project = project, onClick = { onProjectClick(project.id) })
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        }
    }
}

@Composable
private fun GreetingHeader(userName: String?) {
    val hour = remember { LocalTime.now().hour }
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val today by rememberCurrentDate()
    val formattedDate = remember(today) { today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) }

    Column {
        Text(
            text = if (userName.isNullOrBlank()) "$greeting!" else "$greeting, ${userName.substringBefore(" ")}!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayProgressCard(
    progress: Float,
    completed: Int,
    total: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProgressRing(
                progress = progress,
                modifier = Modifier.size(72.dp),
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                progressColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column {
                Text(
                    text = "Today's Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (total == 0) "No tasks scheduled today" else "$completed of $total tasks done",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    trackColor: Color,
    progressColor: Color,
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "dashboardRing")
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        centerContent()
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardSectionHeader(title: String, subtitle: String?, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onSeeAll) { Text("See all") }
    }
}

@Composable
private fun HabitMiniChip(entry: HabitWithStreak, onClick: () -> Unit) {
    val habitColor = remember(entry.habit.color) {
        runCatching { Color(android.graphics.Color.parseColor(entry.habit.color)) }.getOrDefault(Color(0xFF005AC1))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (entry.completedToday) habitColor.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (entry.completedToday) Icons.Filled.CheckCircle else Icons.Default.Whatshot,
                contentDescription = entry.habit.name,
                tint = if (entry.completedToday) habitColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = entry.habit.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun UpcomingTaskRow(task: Task, today: String, onClick: () -> Unit) {
    val priorityColor = getPriorityColor(task.priority)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (task.date == today) (task.time.ifEmpty { "Today" }) else task.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProjectOverviewRow(project: ProjectWithStats, onClick: () -> Unit) {
    val progress = if (project.totalTasks > 0) project.completedTasks.toFloat() / project.totalTasks else 0f
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${project.completedTasks}/${project.totalTasks}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
