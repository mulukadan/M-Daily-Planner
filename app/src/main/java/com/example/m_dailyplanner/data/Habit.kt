package com.example.m_dailyplanner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val color: String = "#005AC1",
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)

// Presence of a row for (habitId, date) means the habit was completed that day.
@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "date"], unique = true)]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habitId: Int,
    val date: String, // Format: "yyyy-MM-dd"
    val completedAt: Long = System.currentTimeMillis()
)
