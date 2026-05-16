package com.example.m_dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                val report = buildReport(stats)
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

    // ── Local report builder ──────────────────────────────────────────────────

    private suspend fun buildReport(s: AppStats): String = withContext(Dispatchers.Default) {
        val completionPct = (s.completionRate * 100).toInt()

        // Score (1–10)
        var score = 5.0
        when {
            completionPct >= 80 -> score += 2.0
            completionPct >= 60 -> score += 1.0
            completionPct < 40  -> score -= 1.0
        }
        when {
            s.currentStreak >= 7 -> score += 1.5
            s.currentStreak >= 3 -> score += 0.5
        }
        when {
            s.overdueTasks > 10 -> score -= 2.0
            s.overdueTasks > 5  -> score -= 1.0
            s.overdueTasks > 0  -> score -= 0.5
        }
        if (s.last7Completed > 0) score += 0.5
        if (s.highTotal > 0 && s.highCompleted.toFloat() / s.highTotal >= 0.7f) score += 0.5
        val finalScore = score.coerceIn(1.0, 10.0).toInt()

        val scoreExplanation = when {
            finalScore >= 9 -> "Outstanding consistency and execution — you're operating at peak productivity."
            finalScore >= 7 -> "Strong productivity habits with room to push even further."
            finalScore >= 5 -> "Solid foundation — a few habit tweaks will unlock your next level."
            finalScore >= 3 -> "Building momentum; consistency is the key to breaking through."
            else            -> "Every completed task is progress — small daily wins compound fast."
        }

        // Behavioral profile (2–3 sentences)
        val styleBase = when {
            completionPct >= 80 && s.currentStreak >= 5 ->
                "You are a disciplined executor — someone who commits and follows through with impressive consistency."
            completionPct >= 70 ->
                "You have a results-driven approach to planning, completing most of what you set out to do."
            completionPct >= 50 ->
                "You plan ambitiously but face some follow-through challenges, common among high-energy planners."
            else ->
                "You're in a building phase, establishing the routines that will drive future productivity."
        }
        val dayNote = if (s.mostProductiveDay != "N/A")
            " Your peak performance lands on ${s.mostProductiveDay}s — your natural power day."
        else ""
        val streakNote = when {
            s.currentStreak >= 7  -> " Your ${s.currentStreak}-day streak shows remarkable daily commitment."
            s.currentStreak >= 3  -> " You're on a ${s.currentStreak}-day streak — good momentum building."
            s.longestStreak >= 5  -> " Your best streak of ${s.longestStreak} days proves you're capable of sustained effort."
            else                  -> " Building a consistent daily habit will be your highest-leverage next step."
        }
        val profile = "$styleBase$dayNote$streakNote"

        // Strengths (up to 3)
        val strengths = mutableListOf<String>()
        if (completionPct >= 70)
            strengths += "Completion rate of $completionPct% — you finish what you start."
        else if (s.completedTasks > 0)
            strengths += "${s.completedTasks} tasks completed — every done item builds momentum."
        if (s.currentStreak >= 3)
            strengths += "${s.currentStreak}-day active streak shows real daily dedication."
        else if (s.longestStreak >= 5)
            strengths += "Personal best streak of ${s.longestStreak} days proves your consistency potential."
        if (s.highTotal > 0 && s.highCompleted.toFloat() / s.highTotal >= 0.6f)
            strengths += "${s.highCompleted * 100 / s.highTotal}% of high-priority tasks done — solid prioritization."
        if (s.totalProjects > 0)
            strengths += "Managing ${s.totalProjects} project${if (s.totalProjects > 1) "s" else ""} shows you think in goals, not just tasks."
        if (s.totalNotes >= 5)
            strengths += "${s.totalNotes} notes across ${s.noteCategories} categories — you're a reflective thinker."
        if (s.last7Completed >= 5)
            strengths += "${s.last7Completed} tasks completed this week — you're in a productive flow."
        val topStrengths = strengths.take(3).ifEmpty {
            listOf(
                "You've started tracking your work — awareness is the first step.",
                "Using tasks, projects, and notes together gives you a complete system.",
                "${s.tasksWithReminders} tasks with reminders set shows planning intent."
            )
        }

        // Areas to improve (up to 3)
        val improvements = mutableListOf<String>()
        if (s.overdueTasks > 0)
            improvements += "${s.overdueTasks} overdue task${if (s.overdueTasks > 1) "s" else ""} — clearing these reduces mental clutter fast."
        if (completionPct < 60)
            improvements += "$completionPct% completion suggests over-planning. Commit to fewer, focused tasks each day."
        if (s.currentStreak == 0)
            improvements += "No active streak — even one small task per day restarts the habit loop quickly."
        else if (s.longestStreak > 4 && s.currentStreak < s.longestStreak / 2)
            improvements += "Current streak (${s.currentStreak}d) is well below your best (${s.longestStreak}d) — you know you can go longer."
        if (s.pendingTasks > 3 && s.tasksWithReminders < s.pendingTasks / 2)
            improvements += "Most pending tasks lack reminders — time-based nudges dramatically improve follow-through."
        if (s.last7Created > 0 && s.last7Completed == 0)
            improvements += "${s.last7Created} tasks created this week, zero completed — shift focus from planning to execution."
        if (s.highTotal > 0 && s.highCompleted.toFloat() / s.highTotal < 0.5f)
            improvements += "Under half your high-priority tasks are done — tackle these first thing each day."
        val topImprovements = improvements.take(3).ifEmpty {
            listOf(
                "Keep pushing your completion rate higher.",
                "Review your task list weekly to clear stale items.",
                "Set reminders on time-sensitive tasks to avoid overdue slippage."
            )
        }

        // Recommendations (up to 4)
        val recs = mutableListOf<String>()
        if (s.overdueTasks > 0)
            recs += "Spend 15 minutes now to reschedule or close your ${s.overdueTasks} overdue task${if (s.overdueTasks > 1) "s" else ""}."
        if (s.mostProductiveDay != "N/A")
            recs += "Schedule your hardest tasks on ${s.mostProductiveDay}s — your data confirms that's your peak day."
        if (s.tasksWithReminders < 3)
            recs += "Add reminder times to upcoming tasks — a single daily alarm anchors your planning habit."
        if (completionPct < 70)
            recs += "Cap yourself at 3–5 tasks per day. Fewer commitments, fully honored, beat long lists half-done."
        if (s.totalNotes < 3)
            recs += "Start a weekly reflection note — 5 minutes reviewing wins and slip-ups compounds over time."
        if (s.totalProjects == 0 && s.totalTasks > 10)
            recs += "Group related tasks into projects — progress becomes visible at a glance."
        if (s.leastProductiveDay != "N/A")
            recs += "Protect ${s.leastProductiveDay}s for lighter work or rest — don't fight your own patterns."
        val topRecs = recs.take(4).ifEmpty {
            listOf(
                "Keep your task list reviewed and up to date every morning.",
                "Use projects to group related tasks for better progress visibility.",
                "Add reminders to every task with a real deadline.",
                "Celebrate completed streaks — positive reinforcement sustains habits."
            )
        }

        // Weekly challenge
        val challenge = when {
            s.overdueTasks >= 5  -> "Clear all ${s.overdueTasks} overdue tasks before adding new ones — enter next week with a clean slate."
            s.currentStreak == 0 -> "Complete at least one task every single day for the next 7 days to rebuild your daily habit."
            completionPct < 50   -> "Complete 80%+ of every task you schedule this week — if you doubt you'll finish it, don't add it."
            s.last7Completed < 3 -> "Close at least ${(s.last7Completed + 3).coerceAtLeast(5)} tasks over the next 7 days — one per day minimum."
            s.tasksWithReminders == 0 -> "Add a reminder to every task you create this week and honor every notification that fires."
            s.totalNotes < 3     -> "Write at least 3 notes this week — one per topic area to capture your thinking."
            else                 -> "Extend your current ${s.currentStreak}-day streak to ${s.currentStreak + 7} days without missing a single day."
        }

        // Assemble
        buildString {
            appendLine("PRODUCTIVITY SCORE: $finalScore/10")
            appendLine(scoreExplanation)
            appendLine()
            appendLine("BEHAVIORAL PROFILE")
            appendLine(profile)
            appendLine()
            appendLine("STRENGTHS")
            topStrengths.forEach { appendLine("• $it") }
            appendLine()
            appendLine("AREAS TO IMPROVE")
            topImprovements.forEach { appendLine("• $it") }
            appendLine()
            appendLine("RECOMMENDATIONS")
            topRecs.forEach { appendLine("• $it") }
            appendLine()
            appendLine("WEEKLY CHALLENGE")
            append(challenge)
        }.trim()
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
