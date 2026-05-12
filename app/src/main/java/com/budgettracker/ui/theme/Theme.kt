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
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF282550),
    onPrimaryContainer = Color(0xFFD4D0FF),
    secondary = Color(0xFF00BFA5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFF60F5D6),
    tertiary = Color(0xFFFF6D00),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF12121E),
    onBackground = Color(0xFFE4E4EC),
    surface = Color(0xFF1C1C2E),
    onSurface = Color(0xFFE4E4EC),
    surfaceVariant = Color(0xFF282840),
    onSurfaceVariant = Color(0xFFA8A8C4),
    outline = Color(0xFF404060),
    inverseSurface = Color(0xFFE4E4EC),
    inverseOnSurface = Color(0xFF12121E),
    error = Color(0xFFFF5252)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8EAF6),
    onPrimaryContainer = Color(0xFF10103A),
    secondary = Color(0xFF00BFA5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF00362E),
    tertiary = Color(0xFFFF6D00),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F8FC),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFE8E8F0),
    onSurfaceVariant = Color(0xFF484860),
    outline = Color(0xFFC8C8D8),
    inverseSurface = Color(0xFF1A1A2E),
    inverseOnSurface = Color(0xFFF0F0FA),
    error = Color(0xFFB3261E)
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}