package com.example.m_dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.data.Habit
import com.example.m_dailyplanner.data.HabitLog
import com.example.m_dailyplanner.data.HabitRepository
import com.example.m_dailyplanner.util.HabitStreakCalculator
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
                currentStreak = HabitStreakCalculator.currentStreak(dates),
                bestStreak = HabitStreakCalculator.bestStreak(dates)
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

    fun updateHabitOrder(habits: List<Habit>) {
        viewModelScope.launch {
            repository.updateHabits(habits.mapIndexed { index, habit -> habit.copy(position = index) })
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { repository.deleteHabit(habit) }
    }

    fun toggleToday(habitId: Int) {
        viewModelScope.launch {
            repository.toggleLog(habitId, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
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
