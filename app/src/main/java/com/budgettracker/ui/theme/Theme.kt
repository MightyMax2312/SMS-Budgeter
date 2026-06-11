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
    primary = Color(0xFFBFC6AD),
    onPrimary = Color(0xFF17241D),
    primaryContainer = Color(0xFF536051),
    onPrimaryContainer = Color(0xFFE4E6D4),
    secondary = Color(0xFFE0704E),
    onSecondary = Color(0xFF1F140F),
    secondaryContainer = Color(0xFF6F7B69),
    onSecondaryContainer = Color(0xFFE4E6D4),
    tertiary = Color(0xFFD8C18C),
    onTertiary = Color(0xFF211D12),
    background = Color(0xFF121711),
    onBackground = Color(0xFFE4E6D4),
    surface = Color(0xFF1B211A),
    onSurface = Color(0xFFE4E6D4),
    surfaceVariant = Color(0xFF30372E),
    onSurfaceVariant = Color(0xFFB8BEA8),
    outline = Color(0xFF77816F),
    inverseSurface = Color(0xFFE4E6D4),
    inverseOnSurface = Color(0xFF18261E),
    error = Color(0xFFE0704E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF536051),
    onPrimary = Color(0xFFE4E6D4),
    primaryContainer = Color(0xFFC9CEB9),
    onPrimaryContainer = Color(0xFF18261E),
    secondary = Color(0xFFE0704E),
    onSecondary = Color(0xFF1F140F),
    secondaryContainer = Color(0xFFF0B297),
    onSecondaryContainer = Color(0xFF26130E),
    tertiary = Color(0xFF7F8775),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFC9CEB9),
    onBackground = Color(0xFF18261E),
    surface = Color(0xFFDEE1CF),
    onSurface = Color(0xFF18261E),
    surfaceVariant = Color(0xFFB8BEA8),
    onSurfaceVariant = Color(0xFF536051),
    outline = Color(0xFF7F8775),
    inverseSurface = Color(0xFF18261E),
    inverseOnSurface = Color(0xFFE4E6D4),
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
