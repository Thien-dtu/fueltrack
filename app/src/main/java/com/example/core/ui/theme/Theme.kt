package com.example.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FuelTrackColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color(0xFF1E1E1E),
    primaryContainer = AmberContainer,
    onPrimaryContainer = OnAmberContainer,
    secondary = CyanAccent,
    onSecondary = Color(0xFF032B42),
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun FuelTrackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FuelTrackColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
