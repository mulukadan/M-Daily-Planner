package com.example.m_dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.BuildConfig
import com.example.m_dailyplanner.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset

// ── Data ──────────────────────────────────────────────────────────────────────

data class AppStats(
    val totalTasks: Int,
    val completedTasks: Int,
    val inProgressTasks: Int,
    val pendingTasks: Int,
    val overdueTasks: Int,
    val completionRate: Float,
    val highTotal: Int, val highCompleted: Int,
    val mediumTotal: Int, val mediumCompleted: Int,
    val lowTotal: Int, val lowCompleted: Int,
    val last7Created: Int, val last7Completed: Int,
    val last30Created: Int, val last30Completed: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val mostProductiveDay: String,
    val leastProductiveDay: String,
    val tasksWithReminders: Int,
    val avgTasksPerActiveDay: Float,
    val totalProjects: Int,
    val projectSummaries: List<ProjectSummary>,
    val totalNotes: Int,
    val notesLast7Days: Int,
    val noteCategories: Int
)

data class ProjectSummary(val name: String, val total: Int, val completed: Int)

sealed class ReportState {
    object Idle : ReportState()
    object Loading : ReportState()
    data class Success(val report: String, val stats: AppStats) : ReportState()
    data class Error(val message: String) : ReportState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ReportViewModel(
    application: Application,
    private val database: TaskDatabase
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ReportState>(ReportState.Idle)
    val state: StateFlow<ReportState> = _state.asStateFlow()

    fun generateReport() {
        viewModelScope.launch {
            _state.value = ReportState.Loading
            runCatching {
                val stats = collectStats()
                val report = callClaude(stats)
                _state.value = ReportState.Success(report, stats)
            }.onFailure { e ->
                _state.value = ReportState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun reset() { _state.value = ReportState.Idle }

    // ── Stats collection ──────────────────────────────────────────────────────

    private suspend fun collectStats(): AppStats = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val allTasks        = database.taskDao().getAllTasksList()
        val allProjectTasks = database.projectTaskDao().getAllProjectTasksList()
        val allProjects     = database.projectDao().getAllProjectsList()
        val notes           = database.noteDao().getAllNotesList()
        val noteCategories  = database.noteCategoryDao().getAllCategoriesList()

        val completed  = allTasks.filter { it.status == TaskStatus.COMPLETED.name }
        val inProgress = allTasks.filter { it.status == TaskStatus.IN_PROGRESS.name }
        val pending    = allTasks.filter { it.status == TaskStatus.PENDING.name }
        val overdue    = pending.filter {
            runCatching { LocalDate.parse(it.date).isBefore(today) }.getOrDefault(false)
        }

        fun pri(p: String) = allTasks.filter { it.priority.uppercase() == p }
        val high   = pri("HIGH");  val medium = pri("MEDIUM");  val low = pri("LOW")
        fun doneIn(list: List<Task>) = list.count { it.status == TaskStatus.COMPLETED.name }

        val sevenAgo  = today.minusDays(7)
        val thirtyAgo = today.minusDays(30)
        fun inRange(t: Task, from: LocalDate) =
            runCatching { LocalDate.parse(t.date) >= from }.getOrDefault(false)

        // Streaks (based on dates that have at least one completed task)
        val completedDates = completed
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .toSortedSet()

        var currentStreak = 0
        var check = today
        while (completedDates.contains(check)) { currentStreak++; check = check.minusDays(1) }

        var longestStreak = 0; var run = 0
        completedDates.toList().forEachIndexed { i, d ->
            val prev = completedDates.toList().getOrNull(i - 1)
            run = if (prev != null && d.minusDays(1) == prev) run + 1 else 1
            longestStreak = maxOf(longestStreak, run)
        }

        // Day-of-week productivity
        val byDay = completed.groupBy {
            runCatching { LocalDate.parse(it.date).dayOfWeek }.getOrNull()
        }.filterKeys { it != null }
        fun dayName(e: Map.Entry<*, List<Task>>?) =
            (e?.key as? java.time.DayOfWeek)?.name
                ?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "N/A"

        val activeDays = allTasks
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .toSet().size.coerceAtLeast(1)

        val sevenAgoMs = sevenAgo.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

        val projectTasksByProject = allProjectTasks.groupBy { it.projectId }
        val projectSummaries = allProjects.map { p ->
            val tasks = projectTasksByProject[p.id] ?: emptyList()
            ProjectSummary(p.name, tasks.size, tasks.count { it.status == TaskStatus.COMPLETED.name })
        }

        AppStats(
            totalTasks      = allTasks.size,
            completedTasks  = completed.size,
            inProgressTasks = inProgress.size,
            pendingTasks    = pending.size,
            overdueTasks    = overdue.size,
            completionRate  = if (allTasks.isEmpty()) 0f else completed.size.toFloat() / allTasks.size,
            highTotal       = high.size,   highCompleted   = doneIn(high),
            mediumTotal     = medium.size, mediumCompleted = doneIn(medium),
            lowTotal        = low.size,    lowCompleted    = doneIn(low),
            last7Created    = allTasks.count { inRange(it, sevenAgo) },
            last7Completed  = completed.count { inRange(it, sevenAgo) },
            last30Created   = allTasks.count { inRange(it, thirtyAgo) },
            last30Completed = completed.count { inRange(it, thirtyAgo) },
            currentStreak   = currentStreak,
            longestStreak   = longestStreak,
            mostProductiveDay  = dayName(byDay.maxByOrNull { it.value.size }),
            leastProductiveDay = dayName(byDay.minByOrNull { it.value.size }),
            tasksWithReminders = allTasks.count { it.reminderEnabled },
            avgTasksPerActiveDay = allTasks.size.toFloat() / activeDays,
            totalProjects    = allProjects.size,
            projectSummaries = projectSummaries,
            totalNotes       = notes.size,
            notesLast7Days   = notes.count { it.createdAt >= sevenAgoMs },
            noteCategories   = noteCategories.size
        )
    }

    // ── Claude API ────────────────────────────────────────────────────────────

    private suspend fun callClaude(stats: AppStats): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.CLAUDE_API_KEY
        if (apiKey.isBlank())
            throw Exception("Add CLAUDE_API_KEY=sk-ant-... to local.properties and rebuild.")

        val conn = (URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            connectTimeout = 30_000
            readTimeout    = 60_000
            doOutput = true
        }

        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 1500)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", buildPrompt(stats))
                })
            })
        }.toString()

        conn.outputStream.bufferedWriter().use { it.write(body) }

        val code = conn.responseCode
        val text = (if (code == 200) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText() ?: ""
        conn.disconnect()

        if (code != 200) throw Exception("Claude API error $code: $text")
        JSONObject(text).getJSONArray("content").getJSONObject(0).getString("text")
    }

    private fun buildPrompt(s: AppStats): String {
        fun pct(n: Int, d: Int) = if (d == 0) "0%" else "${n * 100 / d}%"
        val projects = if (s.projectSummaries.isEmpty()) "  No projects yet."
        else s.projectSummaries.joinToString("\n") {
            "  • ${it.name}: ${it.completed}/${it.total} done (${pct(it.completed, it.total)})"
        }

        return """
You are an expert productivity coach and behavioral analyst. Analyze this user's task management data and generate a warm, insightful productivity report.

=== USER DATA ===

TASKS (All Time):
Total: ${s.totalTasks} | Completed: ${s.completedTasks} (${pct(s.completedTasks, s.totalTasks)}) | In Progress: ${s.inProgressTasks} | Pending: ${s.pendingTasks} | Overdue: ${s.overdueTasks}

PRIORITY BREAKDOWN:
High:   ${s.highTotal} tasks, ${s.highCompleted} done (${pct(s.highCompleted, s.highTotal)})
Medium: ${s.mediumTotal} tasks, ${s.mediumCompleted} done (${pct(s.mediumCompleted, s.mediumTotal)})
Low:    ${s.lowTotal} tasks, ${s.lowCompleted} done (${pct(s.lowCompleted, s.lowTotal)})

RECENT ACTIVITY:
Last  7 days: ${s.last7Created} created, ${s.last7Completed} completed
Last 30 days: ${s.last30Created} created, ${s.last30Completed} completed

HABITS & PATTERNS:
Current streak: ${s.currentStreak} days | Best streak: ${s.longestStreak} days
Most productive day: ${s.mostProductiveDay} | Least productive: ${s.leastProductiveDay}
Avg tasks per active day: ${"%.1f".format(s.avgTasksPerActiveDay)} | With reminders: ${s.tasksWithReminders}

PROJECTS (${s.totalProjects}):
$projects

NOTES: ${s.totalNotes} total | ${s.notesLast7Days} this week | ${s.noteCategories} categories

=== REQUIRED FORMAT ===
Use exactly these section headings:

PRODUCTIVITY SCORE: [X]/10
[One sentence explanation of the score]

BEHAVIORAL PROFILE
[2–3 sentences describing their productivity personality based on the patterns]

STRENGTHS
• [Strength with specific data reference]
• [Strength with specific data reference]
• [Strength with specific data reference]

AREAS TO IMPROVE
• [Constructive point with data]
• [Constructive point with data]
• [Constructive point with data]

RECOMMENDATIONS
• [Specific, actionable recommendation]
• [Specific, actionable recommendation]
• [Specific, actionable recommendation]
• [Specific, actionable recommendation]

WEEKLY CHALLENGE
[One concrete, measurable challenge for the next 7 days]

Be warm, specific, and reference actual numbers from the data.
        """.trimIndent()
    }
}

class ReportViewModelFactory(
    private val application: Application,
    private val database: TaskDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ReportViewModel(application, database) as T
}
