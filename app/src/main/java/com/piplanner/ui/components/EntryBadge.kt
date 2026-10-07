package com.piplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Light-blue Typed / Custom badge for History credit entry chrome (PIP-86 / PRD R12).
 * Fill-only chip look (not interactive FilterChip).
 */
@Composable
fun EntryBadge(
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = PiPlannerColors.OnChipLightBlue,
        modifier = modifier
            .clip(RoundedCornerShape(PiPlannerDimens.RadiusChip))
            .background(PiPlannerColors.ChipLightBlue)
            .padding(
                horizontal = PiPlannerDimens.Space12,
                vertical = PiPlannerDimens.Space8,
            )
            .semantics { this.contentDescription = contentDescription },
    )
}
