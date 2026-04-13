package com.example.m_dailyplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String,
    val date: String,            // Format: "yyyy-MM-dd"
    val time: String,            // Format: "HH:mm", empty if not set
    val reminderEnabled: Boolean,
    val status: String,          // TaskStatus enum name
    val category: String = "Work",
    val priority: String = "Medium",
    val createdAt: Long = System.currentTimeMillis()
)

enum class TaskStatus(val displayName: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    companion object {
        fun fromString(value: String): TaskStatus {
            return entries.find { it.name == value } ?: PENDING
        }
    }
}

enum class TaskPriority {
    HIGH, MEDIUM, LOW
}
