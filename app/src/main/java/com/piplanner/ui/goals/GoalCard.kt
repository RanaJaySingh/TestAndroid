package com.piplanner.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.piplanner.domain.GoalsTabService
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Individual goal card — saved/of target, status, monthly need, % of credits
 * (frames 9 / 11 · PIP-82 / Tech Spec §3.5 GoalCard).
 */
@Composable
fun GoalCard(
    name: String,
    formattedSavedOfTarget: String,
    statusLabel: String,
    monthlyNeedLabel: String,
    creditsPercentLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (statusLabel) {
        GoalsTabService.STATUS_ON_TRACK -> PiPlannerColors.PositiveGreen
        GoalsTabService.STATUS_BEHIND -> PiPlannerColors.Behind
        else -> PiPlannerColors.OnSurface.copy(alpha = 0.62f)
    }

    PiCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics {
                contentDescription =
                    "$name, $formattedSavedOfTarget, $statusLabel, $monthlyNeedLabel, " +
                        "$creditsPercentLabel. Opens goal detail"
            },
        contentPadding = PiPlannerDimens.Space16,
        contentDescription = "Goal card $name",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
            ) {
                Text(
                    text = name,
                    style = PiPlannerTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.OnSurface,
                )
                Text(
                    text = formattedSavedOfTarget,
                    style = PiPlannerTypography.title,
                    color = PiPlannerColors.OnSurface,
                )
                Text(
                    text = monthlyNeedLabel,
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.62f),
                )
                Text(
                    text = creditsPercentLabel,
                    style = PiPlannerTypography.caption,
                    fontWeight = FontWeight.Medium,
                    color = PiPlannerColors.NavyPrimary,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = statusLabel,
                        style = PiPlannerTypography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(
                            horizontal = PiPlannerDimens.Space12,
                            vertical = PiPlannerDimens.Space8,
                        ),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.55f),
                )
            }
        }
    }
}
