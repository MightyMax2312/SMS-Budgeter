package com.budgettracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC0F3A8),
    onPrimary = Color(0xFF0C0C0C),
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color(0xFFC0F3A8),
    secondary = Color(0xFFC0F3A8),
    onSecondary = Color(0xFF0C0C0C),
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color(0xFFC0F3A8),
    tertiary = Color(0xFFC0F3A8),
    onTertiary = Color(0xFF0C0C0C),
    background = Color(0xFF0C0C0C),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF131313),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFF8A8A8A),
    outline = Color(0xFF262626),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF0C0C0C),
    error = Color(0xFFE0704E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9F2E2),
    onPrimaryContainer = Color(0xFF0C2E1A),
    secondary = Color(0xFF2E7D52),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9F2E2),
    onSecondaryContainer = Color(0xFF0C2E1A),
    tertiary = Color(0xFF2E7D52),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFFD6D6D6),
    inverseSurface = Color(0xFF111111),
    inverseOnSurface = Color(0xFFF7F7F7),
    error = Color(0xFFE0704E)
)

@Composable
fun SMSBudgetTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
