package com.example.m_dailyplanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NaturalLanguageDateParserTest {

    // 2024-01-01 is a Monday — fixed so weekday math in these tests is deterministic.
    private val monday = LocalDate.of(2024, 1, 1)

    @Test
    fun `plain text with no date or time passes through unchanged`() {
        val result = NaturalLanguageDateParser.parse("Buy milk", monday)

        assertEquals("Buy milk", result.cleanedName)
        assertNull(result.date)
        assertNull(result.time)
    }

    @Test
    fun `tomorrow and pm time are both extracted and stripped from the name`() {
        val result = NaturalLanguageDateParser.parse("Call dentist tomorrow at 3pm", monday)

        assertEquals("Call dentist", result.cleanedName)
        assertEquals(monday.plusDays(1), result.date)
        assertEquals(LocalTime.of(15, 0), result.time)
    }

    @Test
    fun `today and tonight both resolve to the reference date`() {
        assertEquals(monday, NaturalLanguageDateParser.parse("Standup today", monday).date)
        assertEquals(monday, NaturalLanguageDateParser.parse("Party tonight", monday).date)
    }

    @Test
    fun `noon and midnight resolve to fixed times`() {
        assertEquals(LocalTime.NOON, NaturalLanguageDateParser.parse("Lunch at noon", monday).time)
        assertEquals(LocalTime.MIDNIGHT, NaturalLanguageDateParser.parse("Reset at midnight", monday).time)
    }

    @Test
    fun `12am is midnight and 12pm is noon`() {
        assertEquals(LocalTime.of(0, 0), NaturalLanguageDateParser.parse("Call at 12am", monday).time)
        assertEquals(LocalTime.of(12, 0), NaturalLanguageDateParser.parse("Call at 12pm", monday).time)
    }

    @Test
    fun `am pm matching is case insensitive`() {
        val result = NaturalLanguageDateParser.parse("Call TOMORROW at 3PM", monday)

        assertEquals(monday.plusDays(1), result.date)
        assertEquals(LocalTime.of(15, 0), result.time)
    }

    @Test
    fun `24-hour time is parsed when no am pm suffix is present`() {
        val result = NaturalLanguageDateParser.parse("Doctor 15:30", monday)

        assertEquals(LocalTime.of(15, 30), result.time)
        assertEquals("Doctor", result.cleanedName)
    }

    @Test
    fun `in N days resolves relative to the reference date`() {
        val result = NaturalLanguageDateParser.parse("Renew passport in 3 days", monday)

        assertEquals(monday.plusDays(3), result.date)
    }

    @Test
    fun `next week resolves a full week ahead`() {
        val result = NaturalLanguageDateParser.parse("Follow up next week", monday)

        assertEquals(monday.plusWeeks(1), result.date)
    }

    @Test
    fun `bare weekday mention on that same weekday resolves to today, not next week`() {
        // monday is itself a Monday, so a bare "monday" should mean today.
        val result = NaturalLanguageDateParser.parse("Team sync monday", monday)

        assertEquals(monday, result.date)
    }

    @Test
    fun `bare weekday mention resolves to the closest upcoming occurrence`() {
        // monday + 4 days = Friday of the same week.
        val result = NaturalLanguageDateParser.parse("Deploy friday", monday)

        assertEquals(monday.plusDays(4), result.date)
    }

    @Test
    fun `next weekday explicitly skips this week even if today matches`() {
        // "next monday" said on a Monday should mean 7 days out, not today.
        val result = NaturalLanguageDateParser.parse("Review next monday", monday)

        assertEquals(monday.plusDays(7), result.date)
    }

    @Test
    fun `when the whole input is the date phrase the name falls back to the raw text`() {
        val result = NaturalLanguageDateParser.parse("tomorrow", monday)

        assertEquals("tomorrow", result.cleanedName)
        assertEquals(monday.plusDays(1), result.date)
    }

    @Test
    fun `weekday abbreviations are recognized`() {
        val result = NaturalLanguageDateParser.parse("Gym fri", monday)

        assertEquals(monday.plusDays(4), result.date)
        assertEquals("Gym", result.cleanedName)
    }
}
