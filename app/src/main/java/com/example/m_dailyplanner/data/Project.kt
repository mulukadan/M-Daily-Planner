package com.example.m_dailyplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ProjectWithStats(
    val id: Int,
    val name: String,
    val description: String,
    val createdAt: Long,
    val totalTasks: Int,
    val completedTasks: Int
)
