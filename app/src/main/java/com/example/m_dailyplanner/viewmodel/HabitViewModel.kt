package com.example.m_dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.data.Habit
import com.example.m_dailyplanner.data.HabitLog
import com.example.m_dailyplanner.data.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HabitWithStreak(
    val habit: Habit,
    val completedToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int
)

class HabitViewModel(
    application: Application,
    private val repository: HabitRepository
) : AndroidViewModel(application) {

    val habitsWithStreaks: StateFlow<List<HabitWithStreak>> = combine(
        repository.getActiveHabits(), repository.getAllLogs()
    ) { habits, logs ->
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logsByHabit = logs.groupBy { it.habitId }
        habits.map { habit ->
            val habitLogs = logsByHabit[habit.id].orEmpty()
            val dates = habitLogs.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
            HabitWithStreak(
                habit = habit,
                completedToday = habitLogs.any { it.date == today },
                currentStreak = currentStreak(dates),
                bestStreak = bestStreak(dates)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addHabit(name: String, color: String) {
        viewModelScope.launch { repository.insertHabit(Habit(name = name.trim(), color = color)) }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch { repository.updateHabit(habit) }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { repository.deleteHabit(habit) }
    }

    fun toggleToday(habitId: Int) {
        viewModelScope.launch {
            repository.toggleLog(habitId, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
    }

    // Consecutive completed days ending today (or ending yesterday if today isn't logged
    // yet, so the streak doesn't reset to 0 the moment the clock ticks past midnight).
    private fun currentStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val today = LocalDate.now()
        var cursor = if (today in dates) today else today.minusDays(1)
        if (cursor !in dates) return 0
        var streak = 0
        while (cursor in dates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun bestStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            current = if (sorted[i] == sorted[i - 1].plusDays(1)) current + 1 else 1
            best = maxOf(best, current)
        }
        return best
    }
}

class HabitViewModelFactory(
    private val application: Application,
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
