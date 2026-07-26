package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightMotorOrange,
    onPrimary = Color.White,
    secondary = LightSurfaceDarkElevated,
    onSecondary = LightTextPrimary,
    tertiary = LightMotorAmber,
    onTertiary = Color.White,
    background = LightSlateDark,
    onBackground = LightTextPrimary,
    surface = LightSurfaceDark,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceDarkElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderColor,
    error = Color(0xFFEF4444)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkMotorOrange,
    onPrimary = Color.Black,
    secondary = DarkSurfaceDarkElevated,
    onSecondary = DarkTextPrimary,
    tertiary = DarkMotorAmber,
    onTertiary = Color.Black,
    background = DarkSlateDark,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceDark,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceDarkElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderColor,
    error = Color(0xFFEF4444)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

