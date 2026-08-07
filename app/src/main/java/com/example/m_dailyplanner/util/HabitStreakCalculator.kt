package com.example.m_dailyplanner.util

import java.time.LocalDate

/**
 * Pure streak math over a habit's set of completed dates — kept free of any
 * Android/ViewModel dependency so it can be unit tested directly.
 */
object HabitStreakCalculator {

    // Consecutive completed days ending today (or ending yesterday if today isn't logged
    // yet, so the streak doesn't reset to 0 the moment the clock ticks past midnight).
    fun currentStreak(dates: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
        if (dates.isEmpty()) return 0
        var cursor = if (today in dates) today else today.minusDays(1)
        if (cursor !in dates) return 0
        var streak = 0
        while (cursor in dates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun bestStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            current = if (sorted[i] == sorted[i - 1].plusDays(1)) current + 1 else 1
            best = maxOf(best, current)
        }
        return best
    }
}
