package com.example.m_dailyplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY position ASC, createdAt ASC")
    fun getActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsList(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Query("SELECT COALESCE(MAX(position), -1) FROM habits")
    suspend fun getMaxPosition(): Int

    @Transaction
    suspend fun insertHabitAtEnd(habit: Habit): Long {
        val nextPosition = getMaxPosition() + 1
        return insertHabit(habit.copy(position = nextPosition))
    }

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habit_logs")
    fun getAllLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLog(habitId: Int, date: String): HabitLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: Int, date: String)

    // Toggling on the same (habitId, date) from two quick taps must not race: read-then-write
    // happens inside one transaction, so the second call always sees the first's result.
    @Transaction
    suspend fun toggleLog(habitId: Int, date: String) {
        if (getLog(habitId, date) != null) deleteLog(habitId, date)
        else insertLog(HabitLog(habitId = habitId, date = date))
    }

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: Int)
}
