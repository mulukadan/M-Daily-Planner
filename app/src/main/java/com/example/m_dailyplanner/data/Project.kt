package com.example.m_dailyplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_PROJECT_COLOR = "#6750A4"

val PROJECT_COLORS = listOf(
    "#6750A4", // Purple
    "#0288D1", // Blue
    "#00897B", // Teal
    "#388E3C", // Green
    "#F57C00", // Orange
    "#D32F2F", // Red
    "#7B1FA2", // Deep Purple
    "#1565C0", // Dark Blue
    "#6D4C41", // Brown
    "#455A64", // Blue Grey
    "#C2185B", // Pink
    "#FBC02D", // Amber
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val color: String = DEFAULT_PROJECT_COLOR,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class ProjectWithStats(
    val id: Int,
    val name: String,
    val description: String,
    val color: String,
    val position: Int,
    val createdAt: Long,
    val totalTasks: Int,
    val completedTasks: Int
)
