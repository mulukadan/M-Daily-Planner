package com.example.m_dailyplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_CATEGORY_ID = 1
const val DEFAULT_CATEGORY_COLOR = "#6750A4"

val CATEGORY_COLORS = listOf(
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

@Entity(tableName = "note_categories")
data class NoteCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String = DEFAULT_CATEGORY_COLOR,
    val createdAt: Long = System.currentTimeMillis()
)
