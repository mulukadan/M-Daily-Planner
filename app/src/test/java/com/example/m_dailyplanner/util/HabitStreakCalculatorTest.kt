package com.example.m_dailyplanner.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStreakCalculatorTest {

    private val today = LocalDate.of(2024, 1, 10)

    @Test
    fun `no logged dates means no streak`() {
        assertEquals(0, HabitStreakCalculator.currentStreak(emptySet(), today))
        assertEquals(0, HabitStreakCalculator.bestStreak(emptySet()))
    }

    @Test
    fun `single day logged today is a streak of 1`() {
        val dates = setOf(today)

        assertEquals(1, HabitStreakCalculator.currentStreak(dates, today))
        assertEquals(1, HabitStreakCalculator.bestStreak(dates))
    }

    @Test
    fun `streak continues counting from yesterday when today is not logged yet`() {
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))

        assertEquals(3, HabitStreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `streak is zero when the most recent log is older than yesterday`() {
        val dates = setOf(today.minusDays(2), today.minusDays(3))

        assertEquals(0, HabitStreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `a gap breaks the current streak even though older days were logged`() {
        // Logged today and yesterday, but there's a gap before that.
        val dates = setOf(today, today.minusDays(1), today.minusDays(5), today.minusDays(6))

        assertEquals(2, HabitStreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `best streak finds the longest run even if it is not the current one`() {
        // A 4-day streak two weeks ago, then a broken 1-day streak today.
        val dates = setOf(
            today.minusDays(14), today.minusDays(13), today.minusDays(12), today.minusDays(11),
            today
        )

        assertEquals(1, HabitStreakCalculator.currentStreak(dates, today))
        assertEquals(4, HabitStreakCalculator.bestStreak(dates))
    }

    @Test
    fun `consecutive run ending today is both the current and best streak`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(3))

        assertEquals(4, HabitStreakCalculator.currentStreak(dates, today))
        assertEquals(4, HabitStreakCalculator.bestStreak(dates))
    }
}
