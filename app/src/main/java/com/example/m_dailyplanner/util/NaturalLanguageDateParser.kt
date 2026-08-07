package com.example.m_dailyplanner.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class ParsedTaskInput(
    val cleanedName: String,
    val date: LocalDate?,
    val time: LocalTime?
)

/**
 * Lightweight, fully on-device quick-add parser, e.g.
 * "Call dentist tomorrow at 3pm" -> name="Call dentist", date=tomorrow, time=15:00.
 * Covers the common Todoist/TickTick-style shorthands rather than full NLP — if nothing
 * matches, the original text passes through untouched as the task name.
 */
object NaturalLanguageDateParser {

    private val weekdayNames = mapOf(
        "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY
    )

    private val amPmTimeRegex = Regex("""\b(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\s*([aApP][mM])\b""")
    private val twentyFourHourTimeRegex = Regex("""\b([01]?[0-9]|2[0-3]):([0-5][0-9])\b""")
    private val noonMidnightRegex = Regex("""\b(noon|midnight)\b""", RegexOption.IGNORE_CASE)

    private val tomorrowRegex = Regex("""\b(tomorrow|tmrw)\b""", RegexOption.IGNORE_CASE)
    private val todayRegex = Regex("""\b(today|tonight)\b""", RegexOption.IGNORE_CASE)
    private val nextWeekRegex = Regex("""\bnext week\b""", RegexOption.IGNORE_CASE)
    private val inDaysRegex = Regex("""\bin\s+(\d+)\s+days?\b""", RegexOption.IGNORE_CASE)
    private val nextWeekdayRegex = Regex("""\bnext\s+(\w+)\b""", RegexOption.IGNORE_CASE)
    private val weekdayRegex = Regex(
        "\\b(" + weekdayNames.keys.joinToString("|") { Regex.escape(it) } + ")\\b",
        RegexOption.IGNORE_CASE
    )
    private val fillerWordsRegex = Regex("""\b(at|on)\b""", RegexOption.IGNORE_CASE)
    private val extraSpacesRegex = Regex("""\s{2,}""")

    fun parse(input: String, referenceDate: LocalDate = LocalDate.now()): ParsedTaskInput {
        var remaining = input
        var time: LocalTime? = null
        var date: LocalDate? = null

        amPmTimeRegex.find(remaining)?.let { match ->
            time = parseAmPmTime(match)
            remaining = remaining.removeRange(match.range)
        } ?: noonMidnightRegex.find(remaining)?.let { match ->
            time = if (match.value.equals("noon", ignoreCase = true)) LocalTime.NOON else LocalTime.MIDNIGHT
            remaining = remaining.removeRange(match.range)
        } ?: twentyFourHourTimeRegex.find(remaining)?.let { match ->
            time = LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
            remaining = remaining.removeRange(match.range)
        }

        tomorrowRegex.find(remaining)?.let { match ->
            date = referenceDate.plusDays(1)
            remaining = remaining.removeRange(match.range)
        } ?: todayRegex.find(remaining)?.let { match ->
            date = referenceDate
            remaining = remaining.removeRange(match.range)
        } ?: nextWeekRegex.find(remaining)?.let { match ->
            date = referenceDate.plusWeeks(1)
            remaining = remaining.removeRange(match.range)
        } ?: inDaysRegex.find(remaining)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { days ->
                date = referenceDate.plusDays(days.toLong())
                remaining = remaining.removeRange(match.range)
            }
        } ?: nextWeekdayRegex.find(remaining)?.let { match ->
            weekdayNames[match.groupValues[1].lowercase()]?.let { day ->
                date = nextOccurrence(day, referenceDate, skipToday = true)
                remaining = remaining.removeRange(match.range)
            }
        } ?: weekdayRegex.find(remaining)?.let { match ->
            weekdayNames[match.value.lowercase()]?.let { day ->
                date = nextOccurrence(day, referenceDate, skipToday = false)
                remaining = remaining.removeRange(match.range)
            }
        }

        val cleaned = remaining
            .replace(fillerWordsRegex, " ")
            .replace(extraSpacesRegex, " ")
            .trim()

        return ParsedTaskInput(
            cleanedName = cleaned.ifBlank { input.trim() },
            date = date,
            time = time
        )
    }

    private fun parseAmPmTime(match: MatchResult): LocalTime {
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
        val isPm = match.groupValues[3].equals("pm", ignoreCase = true)
        if (hour == 12) hour = 0
        return LocalTime.of(if (isPm) hour + 12 else hour, minute)
    }

    private fun nextOccurrence(day: DayOfWeek, from: LocalDate, skipToday: Boolean): LocalDate {
        var daysUntil = (day.value - from.dayOfWeek.value + 7) % 7
        if (daysUntil == 0 && skipToday) daysUntil = 7
        return from.plusDays(daysUntil.toLong())
    }
}
