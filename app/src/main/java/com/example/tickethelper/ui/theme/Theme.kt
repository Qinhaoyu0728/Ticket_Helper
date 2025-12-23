package com.example.tickethelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF121212), // 深色背景
    surface = Color(0xFF1E1E1E),
    primary = androidx.compose.ui.graphics.Color(0xFF3B82F6),
    secondary = androidx.compose.ui.graphics.Color(0xFF10B981),
    tertiary = androidx.compose.ui.graphics.Color(0xFFF59E0B)
)

private val LightColorScheme = lightColorScheme(
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    primary = androidx.compose.ui.graphics.Color(0xFF3B82F6),
    secondary = androidx.compose.ui.graphics.Color(0xFF10B981),
    tertiary = androidx.compose.ui.graphics.Color(0xFFF59E0B)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun PersonalChestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && darkTheme -> DarkColorScheme
        dynamicColor && !darkTheme -> LightColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}