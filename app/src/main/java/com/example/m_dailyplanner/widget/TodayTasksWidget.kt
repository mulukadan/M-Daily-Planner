package com.example.m_dailyplanner.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.m_dailyplanner.MainActivity
import com.example.m_dailyplanner.data.TaskDatabase
import com.example.m_dailyplanner.data.TaskStatus
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val MAX_VISIBLE_TASKS = 5

/**
 * Read-only "today's tasks" home-screen widget. Deliberately simple (no per-row
 * interactivity) — tapping anywhere opens the app to today's list.
 */
class TodayTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val tasks = TaskDatabase.getDatabase(context).taskDao().getTasksForDate(today).first()
        val pending = tasks.filter { it.status != TaskStatus.COMPLETED.name }
        val completedCount = tasks.size - pending.size

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onBackground
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "$completedCount/${tasks.size}",
                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))

                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks today",
                            style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else if (pending.isEmpty()) {
                        Text(
                            text = "All done for today!",
                            style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else {
                        pending.take(MAX_VISIBLE_TASKS).forEach { task ->
                            Text(
                                text = "•  ${task.name}",
                                maxLines = 1,
                                modifier = GlanceModifier.padding(vertical = 2.dp),
                                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onBackground)
                            )
                        }
                        val remaining = pending.size - MAX_VISIBLE_TASKS
                        if (remaining > 0) {
                            Text(
                                text = "+$remaining more",
                                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget()
}
