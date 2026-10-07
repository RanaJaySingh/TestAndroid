package com.piplanner.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material3 light [ColorScheme] remapped to navy design tokens (PIP-68 / Tech Spec §5.1).
 * Extra tokens (chip, positive, behind) live on [PiPlannerColors] for later screen tickets.
 */
val PiPlannerLightColorScheme: ColorScheme = lightColorScheme(
    primary = PiPlannerColors.NavyPrimary,
    onPrimary = PiPlannerColors.OnNavy,
    primaryContainer = PiPlannerColors.NavyDeep,
    onPrimaryContainer = PiPlannerColors.OnNavy,
    secondary = PiPlannerColors.ChipLightBlue,
    onSecondary = PiPlannerColors.OnChipLightBlue,
    secondaryContainer = PiPlannerColors.ChipLightBlue,
    onSecondaryContainer = PiPlannerColors.OnChipLightBlue,
    tertiary = PiPlannerColors.PositiveGreen,
    onTertiary = PiPlannerColors.OnNavy,
    tertiaryContainer = PiPlannerColors.PositiveGreen.copy(alpha = 0.16f),
    onTertiaryContainer = PiPlannerColors.PositiveGreen,
    error = PiPlannerColors.Destructive,
    onError = PiPlannerColors.OnNavy,
    errorContainer = PiPlannerColors.Behind.copy(alpha = 0.16f),
    onErrorContainer = PiPlannerColors.Behind,
    background = PiPlannerColors.BackgroundApp,
    onBackground = PiPlannerColors.OnBackground,
    surface = PiPlannerColors.SurfaceCard,
    onSurface = PiPlannerColors.OnSurface,
    surfaceVariant = PiPlannerColors.BackgroundApp,
    onSurfaceVariant = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
    outline = PiPlannerColors.OutlineMuted,
)

@Composable
fun PiPlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PiPlannerLightColorScheme,
        typography = PiPlannerTypography.material,
        content = content,
    )
}
