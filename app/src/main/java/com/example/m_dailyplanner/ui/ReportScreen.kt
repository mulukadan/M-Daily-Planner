package com.example.m_dailyplanner.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.m_dailyplanner.viewmodel.AppStats
import com.example.m_dailyplanner.viewmodel.ReportState
import com.example.m_dailyplanner.viewmodel.ReportViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel, onOpenDrawer: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Productivity Report",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Open menu", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = state) {
                is ReportState.Idle -> {
                    item { DateHeader() }
                    item { IdleContent(onGenerate = { viewModel.generateReport() }) }
                }

                is ReportState.Loading -> {
                    item { DateHeader() }
                    item { LoadingContent() }
                }

                is ReportState.Success -> {
                    item { StatsOverview(s.stats) }
                    item { AiReportCard(s.report) }
                    item {
                        OutlinedButton(
                            onClick = { viewModel.reset() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate New Report")
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                is ReportState.Error -> {
                    item { DateHeader() }
                    item { ErrorContent(s.message, onRetry = { viewModel.generateReport() }, onReset = { viewModel.reset() }) }
                }
            }
        }
    }
}

// ── Date header ───────────────────────────────────────────────────────────────

@Composable
private fun DateHeader() {
    val date = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy"))
    }
    Text(
        text = date,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

// ── Idle state ────────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(onGenerate: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Psychology, null, tint = Color.White, modifier = Modifier.size(52.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI Productivity Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Analyze your tasks, habits, and patterns.\nGet personalized insights powered by Claude AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Generate Report", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "Powered by Claude AI",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ── Loading state ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp), strokeWidth = 3.dp)

        Text(
            "Analyzing your productivity patterns…",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )

        // Shimmer placeholder cards
        repeat(3) {
            Card(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
            }
        }
    }
}

// ── Stats overview ────────────────────────────────────────────────────────────

@Composable
private fun StatsOverview(stats: AppStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF2E7D32),
                label = "Completion",
                value = "${(stats.completionRate * 100).toInt()}%"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFire,
                iconColor = Color(0xFFE65100),
                label = "Streak",
                value = "${stats.currentStreak}d"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                iconColor = Color(0xFFD32F2F),
                label = "Overdue",
                value = "${stats.overdueTasks}"
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Task,
                iconColor = MaterialTheme.colorScheme.primary,
                label = "Total Tasks",
                value = "${stats.totalTasks}"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.TrendingUp,
                iconColor = Color(0xFF6A1B9A),
                label = "This Week",
                value = "${stats.last7Completed}✓"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                iconColor = Color(0xFFF9A825),
                label = "Best Streak",
                value = "${stats.longestStreak}d"
            )
        }

        // Priority breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Priority Breakdown", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                PriorityRow("High", stats.highCompleted, stats.highTotal, Color(0xFFD32F2F))
                PriorityRow("Medium", stats.mediumCompleted, stats.mediumTotal, Color(0xFFF57C00))
                PriorityRow("Low", stats.lowCompleted, stats.lowTotal, Color(0xFF388E3C))
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PriorityRow(label: String, done: Int, total: Int, color: Color) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Medium)
            Text("$done/$total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

// ── AI Report card ────────────────────────────────────────────────────────────

@Composable
private fun AiReportCard(report: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("AI Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ReportText(report)
        }
    }
}

@Composable
private fun ReportText(text: String) {
    val sectionHeaders = setOf(
        "PRODUCTIVITY SCORE", "BEHAVIORAL PROFILE", "STRENGTHS",
        "AREAS TO IMPROVE", "RECOMMENDATIONS", "WEEKLY CHALLENGE"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        text.lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.isBlank() -> Spacer(Modifier.height(2.dp))

                // Score line e.g. "PRODUCTIVITY SCORE: 7/10"
                line.startsWith("PRODUCTIVITY SCORE") -> {
                    val parts = line.split(":", limit = 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            parts[0].trim(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (parts.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    parts[1].trim(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Section headers
                sectionHeaders.any { line.uppercase().startsWith(it) } -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        line,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Bullet points
                line.startsWith("•") || line.startsWith("-") -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            line.removePrefix("•").removePrefix("-").trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Body text
                else -> Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}

// ── Error state ───────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onReset: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
            Text("Report Failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReset) { Text("Back") }
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
