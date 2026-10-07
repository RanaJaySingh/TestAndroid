package com.piplanner.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Soft light-blue chip (Ask suggestions / Transfer ₹ amounts).
 *
 * iOS PIP-69 / Reviewer contract:
 * - selected: [PiPlannerColors.ChipLightBlue] fill + navy stroke (~1–1.5dp) + navy semibold label
 * - unselected: fill-only [PiPlannerColors.ChipLightBlue] (no navy stroke)
 */
@Composable
fun LightBlueChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = label,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        },
        shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            // Unselected: fill-only — no stroke.
            borderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            borderWidth = 0.dp,
            // Selected: navy stroke on ChipLightBlue fill.
            selectedBorderColor = PiPlannerColors.NavyPrimary,
            disabledSelectedBorderColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.38f),
            selectedBorderWidth = PiPlannerDimens.ChipSelectedStroke,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = PiPlannerColors.ChipLightBlue,
            labelColor = PiPlannerColors.OnChipLightBlue,
            selectedContainerColor = PiPlannerColors.ChipLightBlue,
            selectedLabelColor = PiPlannerColors.OnChipLightBlue,
            disabledContainerColor = PiPlannerColors.ChipLightBlue.copy(alpha = 0.50f),
            disabledLabelColor = PiPlannerColors.OnChipLightBlue.copy(alpha = 0.50f),
            disabledSelectedContainerColor = PiPlannerColors.ChipLightBlue.copy(alpha = 0.50f),
        ),
    )
}
