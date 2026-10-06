package com.piplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B3D2E),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F4EF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFF7F4EF),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun PiPlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
