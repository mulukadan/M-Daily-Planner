package com.example.m_dailyplanner.data

import kotlinx.coroutines.flow.Flow

// Local-only for now — unlike Task/Project/Note, habit data isn't mirrored to Firestore yet.
class HabitRepository(private val habitDao: HabitDao) {

    fun getActiveHabits(): Flow<List<Habit>> = habitDao.getActiveHabits()

    fun getAllLogs(): Flow<List<HabitLog>> = habitDao.getAllLogs()

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabitAtEnd(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteLogsForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    suspend fun toggleLog(habitId: Int, date: String) = habitDao.toggleLog(habitId, date)
}
