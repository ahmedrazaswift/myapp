package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Raw Color Constants for Light Theme
val LightSlateDark = Color(0xFFF7F9F8)       // Light gray-green tinted clean canvas background
val LightSurfaceDark = Color(0xFFFFFFFF)     // Pure white minimalist cards/surfaces
val LightSurfaceDarkElevated = Color(0xFFEEF5F2) // Soft mint secondary elevated fields/containers
val LightMotorOrange = Color(0xFF006A6A)     // Sophisticated primary teal-green accent
val LightMotorOrangeVariant = Color(0xFF004F4F) // Deeper teal-green variant
val LightMotorAmber = Color(0xFFE65100)      // De-saturated rich orange warning/pending status

val LightTextPrimary = Color(0xFF191C1C)     // Clean, high-contrast almost-black/dark slate text
val LightTextSecondary = Color(0xFF707977)   // Soft, readable slate-grey descriptive text
val LightTextDisabled = Color(0xFFA0ABAA)    // Muted placeholder or disabled text

val LightBorderColor = Color(0xFFDDE4E1)     // Subtle light grey-teal structural borders

// Raw Color Constants for Dark Theme
val DarkSlateDark = Color(0xFF121414)        // Dark slate canvas background
val DarkSurfaceDark = Color(0xFF1C1E1E)      // Deep charcoal/slate minimalist surfaces
val DarkSurfaceDarkElevated = Color(0xFF262929) // Slightly lighter slate elevated fields/containers
val DarkMotorOrange = Color(0xFF4DB6AC)      // Vibrant bright teal accent for dark theme
val DarkMotorOrangeVariant = Color(0xFF00796B)
val DarkMotorAmber = Color(0xFFFFB74D)

val DarkTextPrimary = Color(0xFFE2E4E4)      // Crisp near-white/light slate text
val DarkTextSecondary = Color(0xFFA2AAAB)    // Muted, readable soft-grey text
val DarkTextDisabled = Color(0xFF6E7475)     // Heavily muted placeholder text

val DarkBorderColor = Color(0xFF333A3A)      // Dark slate structural borders

// Dynamic Theme-Aware Color Getters
val SlateDark: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceDark: Color @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceDarkElevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val MotorOrange: Color @Composable get() = MaterialTheme.colorScheme.primary
val MotorOrangeVariant: Color @Composable get() = if (MaterialTheme.colorScheme.background == LightSlateDark) LightMotorOrangeVariant else DarkMotorOrangeVariant
val MotorAmber: Color @Composable get() = MaterialTheme.colorScheme.tertiary

val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextDisabled: Color @Composable get() = if (MaterialTheme.colorScheme.background == LightSlateDark) LightTextDisabled else DarkTextDisabled

val BorderColor: Color @Composable get() = MaterialTheme.colorScheme.outline

// Service type coloring
val ColorServiceWithParts = Color(0xFF006A6A)    // Teal-green for parts service
val ColorOilChange = Color(0xFFE65100)           // Rich orange/amber for oil change
val ColorServiceWithoutParts = Color(0xFF7B1FA2) // Elegant purple for labor only service

