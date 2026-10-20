package com.harness.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Design tokens. Kept in `:core:ui` so every screen shares one source of truth and screenshot
 * goldens stay stable across modules.
 */

private val Ink = Color(0xFF16181D)
private val Paper = Color(0xFFFDFCF9)
private val Slate = Color(0xFF5B616E)
private val Signal = Color(0xFF2F6FED)
private val Alert = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = Signal,
    onPrimary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    onSurfaceVariant = Slate,
    error = Alert,
)

private val DarkColors = darkColorScheme(
    primary = Signal,
    onPrimary = Color.White,
    background = Ink,
    onBackground = Paper,
    surface = Color(0xFF22252C),
    onSurface = Paper,
    onSurfaceVariant = Color(0xFFB9BFCC),
    error = Alert,
)

@Composable
fun HarnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
