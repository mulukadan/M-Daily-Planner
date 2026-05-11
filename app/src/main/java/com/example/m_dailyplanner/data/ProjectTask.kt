package com.example.m_dailyplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_tasks")
data class ProjectTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val name: String,
    val description: String = "",
    val priority: String = "Medium",
    val status: String = TaskStatus.PENDING.name,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
