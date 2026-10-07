package com.piplanner.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.piplanner.R
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Navy balance card on Goals tab — total savings + last-synced line + Sync / Update CTA
 * (frames 9 / 9b / 9c · PIP-82 / Tech Spec §3.5 NavyBalanceCard).
 */
@Composable
fun BalanceCard(
    formattedTotal: String,
    accountSubtitle: String?,
    lastActivityLine: String,
    actionTitle: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PiPlannerDimens.RadiusCard))
            .background(
                Brush.linearGradient(
                    colors = listOf(PiPlannerColors.NavyPrimary, PiPlannerColors.NavyDeep),
                ),
            )
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Goals balance card" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
    ) {
        Text(
            text = stringResource(R.string.goals_total_savings),
            style = PiPlannerTypography.caption,
            color = PiPlannerColors.OnNavy.copy(alpha = 0.78f),
        )
        Text(
            text = formattedTotal,
            style = PiPlannerTypography.amountHero,
            color = PiPlannerColors.OnNavy,
            modifier = Modifier.semantics {
                contentDescription = "Total savings $formattedTotal"
            },
        )
        if (accountSubtitle != null) {
            Text(
                text = accountSubtitle,
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnNavy.copy(alpha = 0.78f),
                modifier = Modifier.semantics {
                    contentDescription = "Dedicated account $accountSubtitle"
                },
            )
        }
        Text(
            text = lastActivityLine,
            style = PiPlannerTypography.caption,
            color = PiPlannerColors.OnNavy.copy(alpha = 0.72f),
            modifier = Modifier.semantics {
                contentDescription = "goals.balance.lastActivity"
            },
        )
        Button(
            onClick = onAction,
            enabled = actionEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = actionTitle },
            shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
            colors = ButtonDefaults.buttonColors(
                containerColor = PiPlannerColors.SurfaceCard,
                contentColor = PiPlannerColors.NavyPrimary,
                disabledContainerColor = PiPlannerColors.SurfaceCard.copy(alpha = 0.72f),
                disabledContentColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.55f),
            ),
        ) {
            Text(
                text = actionTitle,
                style = PiPlannerTypography.body,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
