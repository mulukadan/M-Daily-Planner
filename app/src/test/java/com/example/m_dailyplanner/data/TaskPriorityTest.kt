package com.example.m_dailyplanner.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskPriorityTest {

    @Test
    fun `fromLabel matches the stored title-case labels`() {
        assertEquals(TaskPriority.HIGH, TaskPriority.fromLabel("High"))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromLabel("Medium"))
        assertEquals(TaskPriority.LOW, TaskPriority.fromLabel("Low"))
    }

    @Test
    fun `fromLabel is case insensitive`() {
        assertEquals(TaskPriority.HIGH, TaskPriority.fromLabel("HIGH"))
        assertEquals(TaskPriority.HIGH, TaskPriority.fromLabel("high"))
        assertEquals(TaskPriority.LOW, TaskPriority.fromLabel("lOw"))
    }

    @Test
    fun `fromLabel falls back to MEDIUM for unrecognized input`() {
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromLabel(""))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromLabel("Urgent"))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromLabel("!!!"))
    }
}
