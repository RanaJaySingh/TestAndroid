package com.piplanner.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Soft light-blue chip (Ask suggestions / Transfer ₹ amounts).
 * Selected uses navy fill; unselected uses [PiPlannerColors.ChipLightBlue].
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
            borderColor = PiPlannerColors.OutlineMuted,
            selectedBorderColor = PiPlannerColors.NavyPrimary,
            disabledBorderColor = PiPlannerColors.OutlineMuted.copy(alpha = 0.38f),
            disabledSelectedBorderColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.38f),
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = PiPlannerColors.ChipLightBlue,
            labelColor = PiPlannerColors.OnChipLightBlue,
            selectedContainerColor = PiPlannerColors.NavyPrimary,
            selectedLabelColor = PiPlannerColors.OnNavy,
            disabledContainerColor = PiPlannerColors.ChipLightBlue.copy(alpha = 0.50f),
            disabledLabelColor = PiPlannerColors.OnChipLightBlue.copy(alpha = 0.50f),
            disabledSelectedContainerColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.38f),
        ),
    )
}
