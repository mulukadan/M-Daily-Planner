package com.example.m_dailyplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors Material 3's default ColorScheme doesn't provide roles for
 * (Medium priority / "Pending" = warning, Low priority / "Completed" = success),
 * following the same tone conventions (base/on/container/onContainer) as the rest
 * of the color scheme so they stay legible in both light and dark themes.
 */
data class ExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

val LightExtendedColors = ExtendedColors(
    warning = Color(0xFF8B5000),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDDB3),
    onWarningContainer = Color(0xFF2C1600),
    success = Color(0xFF2E6E3D),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFB3F1C0),
    onSuccessContainer = Color(0xFF00210D)
)

val DarkExtendedColors = ExtendedColors(
    warning = Color(0xFFFFB865),
    onWarning = Color(0xFF4A2800),
    warningContainer = Color(0xFF693C00),
    onWarningContainer = Color(0xFFFFDDB3),
    success = Color(0xFF98D5A4),
    onSuccess = Color(0xFF00391B),
    successContainer = Color(0xFF005227),
    onSuccessContainer = Color(0xFFB3F1C0)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current
