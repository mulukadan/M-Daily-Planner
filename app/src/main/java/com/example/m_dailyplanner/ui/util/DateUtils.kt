package com.example.m_dailyplanner.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A single source of truth for "today" inside Compose UI. Refreshes on every lifecycle
 * resume (app backgrounded overnight, reopened) and self-schedules a refresh at the next
 * midnight (app left open across a day boundary), so screens never disagree about the date.
 */
@Composable
fun rememberCurrentDate(): State<LocalDate> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentDate = remember { mutableStateOf(LocalDate.now()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentDate.value = LocalDate.now()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentDate.value) {
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000))
        currentDate.value = LocalDate.now()
    }

    return currentDate
}
