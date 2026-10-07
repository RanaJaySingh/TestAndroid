package com.piplanner.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piplanner.domain.GoalsTabService
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Goals home quick-action chip row — Sync/Update · New goal · Transfer · History
 * (PIP-82 / Tech Spec §3.5 QuickActionRow). Wires existing destinations only.
 */
@Composable
fun QuickActionRow(
    balanceActionTitle: String,
    onBalanceAction: () -> Unit,
    onNewGoal: () -> Unit,
    onTransfer: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier,
    balanceActionEnabled: Boolean = true,
    transferEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "goals.quickActions" },
        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
    ) {
        QuickActionCell(
            title = balanceActionTitle,
            icon = PiIcons.sync,
            enabled = balanceActionEnabled,
            contentDescription = "goals.quickAction.balance",
            onClick = onBalanceAction,
            modifier = Modifier.weight(1f),
        )
        QuickActionCell(
            title = GoalsTabService.QUICK_NEW_GOAL,
            icon = PiIcons.newCredit,
            contentDescription = "goals.quickAction.newGoal",
            onClick = onNewGoal,
            modifier = Modifier.weight(1f),
        )
        QuickActionCell(
            title = GoalsTabService.QUICK_TRANSFER,
            icon = PiIcons.transfer,
            enabled = transferEnabled,
            contentDescription = "goals.quickAction.transfer",
            onClick = onTransfer,
            modifier = Modifier.weight(1f),
        )
        QuickActionCell(
            title = GoalsTabService.QUICK_HISTORY,
            icon = PiIcons.historyTab,
            contentDescription = "goals.quickAction.history",
            onClick = onHistory,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun QuickActionCell(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.45f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PiPlannerDimens.RadiusChip))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(vertical = PiPlannerDimens.Space8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PiPlannerColors.NavyPrimary.copy(alpha = alpha),
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(PiPlannerDimens.RadiusChip))
                .background(PiPlannerColors.ChipLightBlue.copy(alpha = if (enabled) 1f else 0.55f))
                .padding(PiPlannerDimens.Space12),
        )
        Text(
            text = title,
            style = PiPlannerTypography.caption,
            fontWeight = FontWeight.Medium,
            color = PiPlannerColors.NavyPrimary.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
