package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SonicLocalColorScheme = darkColorScheme(
    primary = ElectricAmber,
    onPrimary = Color(0xFF1A1200),
    primaryContainer = Color(0xFF3A2800),
    onPrimaryContainer = ElectricAmberLight,
    secondary = VinylCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF42141B),
    onSecondaryContainer = Color(0xFFFFCDD2),
    tertiary = StageViolet,
    onTertiary = Color(0xFF1B093B),
    background = ObsidianBackground,
    onBackground = CreamTextPrimary,
    surface = ObsidianSurface,
    onSurface = CreamTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = WarmGrayTextSecondary,
    outline = MutedTextTertiary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SonicLocalColorScheme,
        typography = Typography,
        content = content
    )
}
