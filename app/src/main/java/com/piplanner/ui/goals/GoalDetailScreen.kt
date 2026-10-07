package com.piplanner.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalStatus
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.domain.DeleteGoalService
import com.piplanner.domain.GoalsTabService
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme
import com.piplanner.ui.theme.PiPlannerTypography
import kotlin.math.round

/**
 * Goal detail · design frame 14 (PRD R13).
 * Visual parity via PIP-68 tokens + PIP-70 PiCard. Behaviour (CRUD / transfer / delete / held edits)
 * unchanged — chrome only.
 */
@Composable
fun GoalDetailScreen(
    viewModel: GoalDetailViewModel,
    onBack: () -> Unit,
    onTransfer: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GoalDetailContent(
        uiState = uiState,
        historyAmountLabel = viewModel::formatHistoryAmount,
        onBack = onBack,
        onTransfer = onTransfer,
        onEdit = onEdit,
        onDelete = onDelete,
    )
}

@Composable
internal fun GoalDetailContent(
    uiState: GoalDetailUiState,
    historyAmountLabel: (HistoryEntry) -> String,
    onBack: () -> Unit,
    onTransfer: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Goal detail" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
    ) {
        Text(
            text = stringResource(R.string.back),
            style = PiPlannerTypography.body,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.NavyPrimary,
            modifier = Modifier
                .clickable(onClick = onBack)
                .semantics { contentDescription = "Back" }
                .padding(vertical = PiPlannerDimens.Space8),
        )

        val goal = uiState.goal
        if (uiState.missing || goal == null) {
            Text(
                text = stringResource(R.string.goal_detail_missing),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface,
            )
            return@Column
        }

        Text(
            text = goal.name,
            style = PiPlannerTypography.title,
            color = PiPlannerColors.NavyPrimary,
            modifier = Modifier.semantics { contentDescription = "Goal name ${goal.name}" },
        )

        if (uiState.showHeldEditInfo) {
            HeldEditBanner(message = uiState.heldEditInfoMessage)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
            modifier = Modifier.semantics { contentDescription = "Goal detail metrics" },
        ) {
            HeroSavedCard(
                formattedSaved = uiState.formattedSaved,
                statusLabel = uiState.statusLabel,
                status = goal.status(),
            )
            MetricsCard(
                formattedTarget = uiState.formattedTarget,
                formattedAdjustedTarget = uiState.formattedAdjustedTarget,
                percentReachedLabel = percentReachedLabel(goal),
                formattedMonthlyNeed = uiState.formattedMonthlyNeed,
                startDateLabel = uiState.startDateLabel,
                endDateLabel = uiState.endDateLabel,
                inflationPercentLabel = uiState.inflationPercentLabel,
                sharePercentLabel = uiState.sharePercentLabel,
            )
        }

        ActionsRow(
            onTransfer = { onTransfer(goal.id) },
            onEdit = { onEdit(goal.id) },
            onDelete = { onDelete(goal.id) },
        )

        FromHistorySection(
            entries = uiState.relatedHistory,
            amountLabel = historyAmountLabel,
        )
    }
}

@Composable
private fun HeldEditBanner(message: String) {
    Surface(
        color = PiPlannerColors.ChipLightBlue.copy(alpha = 0.72f),
        shape = RoundedCornerShape(PiPlannerDimens.RadiusCard),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Edit held info" },
    ) {
        Text(
            text = message,
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnChipLightBlue,
            modifier = Modifier.padding(PiPlannerDimens.Space16),
        )
    }
}

@Composable
private fun HeroSavedCard(
    formattedSaved: String,
    statusLabel: String,
    status: GoalStatus,
) {
    PiCard(contentDescription = "Saved amount $formattedSaved") {
        Text(
            text = stringResource(R.string.goal_detail_saved_caption),
            style = PiPlannerTypography.caption,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.64f),
        )
        Spacer(modifier = Modifier.height(PiPlannerDimens.Space12))
        Text(
            text = formattedSaved,
            style = PiPlannerTypography.amountHero,
            color = PiPlannerColors.NavyPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(PiPlannerDimens.Space12))
        StatusChip(statusLabel = statusLabel, status = status)
    }
}

@Composable
private fun StatusChip(
    statusLabel: String,
    status: GoalStatus,
) {
    val foreground = when (status) {
        is GoalStatus.OnTrack -> PiPlannerColors.PositiveGreen
        is GoalStatus.Behind -> PiPlannerColors.Behind
    }
    val statusDescription = when (status) {
        is GoalStatus.OnTrack -> "Status On track"
        is GoalStatus.Behind -> "Status Behind"
    }
    Surface(
        color = foreground.copy(alpha = 0.12f),
        shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
        modifier = Modifier.semantics {
            contentDescription = "Status $statusLabel"
        },
    ) {
        Text(
            text = statusLabel.ifBlank {
                when (status) {
                    is GoalStatus.OnTrack -> GoalsTabService.STATUS_ON_TRACK
                    is GoalStatus.Behind -> GoalsTabService.STATUS_BEHIND
                }
            },
            style = PiPlannerTypography.caption,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
            modifier = Modifier
                .padding(
                    horizontal = PiPlannerDimens.Space12,
                    vertical = PiPlannerDimens.Space8,
                )
                .semantics { contentDescription = statusDescription },
        )
    }
}

@Composable
private fun MetricsCard(
    formattedTarget: String,
    formattedAdjustedTarget: String,
    percentReachedLabel: String,
    formattedMonthlyNeed: String,
    startDateLabel: String,
    endDateLabel: String,
    inflationPercentLabel: String,
    sharePercentLabel: String,
) {
    PiCard(contentDescription = "Goal metrics") {
        MetricRow(
            title = stringResource(R.string.goal_target_label),
            value = formattedTarget,
            contentDescription = "Target $formattedTarget",
        )
        Spacer(modifier = Modifier.height(PiPlannerDimens.Space12))
        MetricRow(
            title = stringResource(R.string.goal_detail_adjusted_target),
            value = formattedAdjustedTarget,
            contentDescription = "Adjusted target $formattedAdjustedTarget",
        )
        Spacer(modifier = Modifier.height(PiPlannerDimens.Space12))
        MetricRow(
            title = stringResource(R.string.goal_detail_percent_reached),
            value = percentReachedLabel,
            contentDescription = "Percent reached $percentReachedLabel",
        )
        Spacer(modifier = Modifier.height(PiPlannerDimens.Space12))
        MetricRow(
            title = stringResource(R.string.goal_monthly_need),
            value = formattedMonthlyNeed,
            contentDescription = "Monthly need $formattedMonthlyNeed",
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = PiPlannerDimens.Space12),
            color = PiPlannerColors.OutlineMuted.copy(alpha = 0.55f),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            modifier = Modifier.semantics {
                contentDescription = "Dates $startDateLabel to $endDateLabel"
            },
        ) {
            MetricRow(
                title = stringResource(R.string.goal_detail_start),
                value = startDateLabel,
                contentDescription = "Start $startDateLabel",
            )
            MetricRow(
                title = stringResource(R.string.goal_detail_end),
                value = endDateLabel,
                contentDescription = "End $endDateLabel",
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = PiPlannerDimens.Space12),
            color = PiPlannerColors.OutlineMuted.copy(alpha = 0.55f),
        )

        MetricRow(
            title = stringResource(R.string.inflation_title),
            value = inflationPercentLabel,
            contentDescription = "Inflation $inflationPercentLabel",
        )
        Spacer(modifier = Modifier.height(PiPlannerDimens.Space12))
        MetricRow(
            title = stringResource(R.string.goal_share_label),
            value = sharePercentLabel,
            contentDescription = "Share $sharePercentLabel",
        )
    }
}

@Composable
private fun MetricRow(
    title: String,
    value: String,
    contentDescription: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.64f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = PiPlannerTypography.body,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ActionsRow(
    onTransfer: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
    ) {
        DetailActionButton(
            title = stringResource(R.string.transfer),
            icon = PiIcons.transfer,
            onClick = onTransfer,
            emphasis = DetailActionEmphasis.Secondary,
            contentDescription = "Transfer",
            modifier = Modifier.weight(1f),
        )
        DetailActionButton(
            title = stringResource(R.string.edit),
            icon = Icons.Outlined.Edit,
            onClick = onEdit,
            emphasis = DetailActionEmphasis.Secondary,
            contentDescription = "Edit goal",
            modifier = Modifier.weight(1f),
        )
        DetailActionButton(
            title = stringResource(R.string.delete),
            icon = PiIcons.goalDeleted,
            onClick = onDelete,
            emphasis = DetailActionEmphasis.Destructive,
            contentDescription = "Delete goal",
            modifier = Modifier.weight(1f),
        )
    }
}

private enum class DetailActionEmphasis {
    Secondary,
    Destructive,
}

@Composable
private fun DetailActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    emphasis: DetailActionEmphasis,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val foreground = when (emphasis) {
        DetailActionEmphasis.Secondary -> PiPlannerColors.NavyPrimary
        DetailActionEmphasis.Destructive -> PiPlannerColors.Destructive
    }
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
        color = PiPlannerColors.SurfaceCard,
        border = BorderStroke(1.5.dp, foreground.copy(alpha = 0.55f)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PiPlannerDimens.Space8,
                    vertical = PiPlannerDimens.Space12,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = PiPlannerTypography.caption,
                fontWeight = FontWeight.SemiBold,
                color = foreground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FromHistorySection(
    entries: List<HistoryEntry>,
    amountLabel: (HistoryEntry) -> String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        modifier = Modifier.semantics { contentDescription = "From History" },
    ) {
        Text(
            text = stringResource(R.string.goal_from_history_title),
            style = PiPlannerTypography.title,
            color = PiPlannerColors.NavyPrimary,
        )
        if (entries.isEmpty()) {
            PiCard(contentDescription = "From History empty") {
                Text(
                    text = stringResource(R.string.goal_from_history_empty),
                    style = PiPlannerTypography.body,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.64f),
                )
            }
        } else {
            PiCard(contentDescription = "From History list") {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = PiPlannerDimens.Space8),
                            color = PiPlannerColors.OutlineMuted.copy(alpha = 0.55f),
                        )
                    }
                    HistoryRow(
                        entry = entry,
                        amountLabel = amountLabel(entry),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    amountLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PiPlannerDimens.Space8)
            .semantics {
                contentDescription = "History ${entry.type.name} $amountLabel"
            },
        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = historyTypeLabel(entry.type),
                    style = PiPlannerTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.OnSurface,
                )
                if (entry.isLocked) {
                    Icon(
                        imageVector = PiIcons.lock,
                        contentDescription = "Locked history entry",
                        tint = PiPlannerColors.OnSurface.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(16.dp)
                            .semantics { contentDescription = "Locked history entry" },
                    )
                }
            }
            Text(
                text = entry.createdAt.take(10),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.64f),
            )
        }
        Text(
            text = amountLabel,
            style = PiPlannerTypography.body,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.NavyPrimary,
        )
    }
}

@Composable
private fun historyTypeLabel(type: HistoryEntryType): String {
    return when (type) {
        HistoryEntryType.OpeningBalance -> stringResource(R.string.history_type_opening)
        HistoryEntryType.NewCredit -> stringResource(R.string.history_type_credit)
        HistoryEntryType.Transfer -> stringResource(R.string.history_type_transfer)
        HistoryEntryType.Withdrawal -> stringResource(R.string.history_type_withdrawal)
        HistoryEntryType.GoalDeleted -> DeleteGoalService.historyTitle
    }
}

/** Presentation-only % of adjusted target reached (saved ÷ adjusted). No ViewModel change. */
internal fun percentReachedLabel(goal: Goal): String {
    val adjusted = goal.adjustedTarget()
    if (adjusted <= 0L) return "—"
    val percent = round(goal.savedAmount.toDouble() / adjusted.toDouble() * 100.0).toInt()
    return "$percent%"
}

@Preview(showBackground = true, name = "Goal detail On track")
@Composable
private fun GoalDetailOnTrackPreview() {
    val goal = previewGoal(savedAmount = 6_000_000L, share = 0.6)
    PiPlannerTheme {
        GoalDetailContent(
            uiState = GoalDetailUiState(
                goalId = goal.id,
                goal = goal,
                formattedSaved = "₹60,000",
                statusLabel = GoalsTabService.STATUS_ON_TRACK,
                formattedTarget = "₹10,00,000",
                formattedAdjustedTarget = "₹13,10,796",
                formattedMonthlyNeed = "₹26,058",
                inflationPercentLabel = "7%",
                sharePercentLabel = "60%",
                startDateLabel = "Oct 2026",
                endDateLabel = "Sep 2030",
            ),
            historyAmountLabel = { "₹60,000" },
            onBack = {},
            onTransfer = {},
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, name = "Goal detail Behind")
@Composable
private fun GoalDetailBehindPreview() {
    val goal = previewGoal(savedAmount = 4_000_000L, share = 0.4)
    PiPlannerTheme {
        GoalDetailContent(
            uiState = GoalDetailUiState(
                goalId = goal.id,
                goal = goal,
                formattedSaved = "₹40,000",
                statusLabel = "Behind · ₹1,058",
                formattedTarget = "₹2,00,000",
                formattedAdjustedTarget = "₹2,14,000",
                formattedMonthlyNeed = "₹14,500",
                inflationPercentLabel = "7%",
                sharePercentLabel = "40%",
                startDateLabel = "Oct 2026",
                endDateLabel = "Sep 2027",
            ),
            historyAmountLabel = { "₹0" },
            onBack = {},
            onTransfer = {},
            onEdit = {},
            onDelete = {},
        )
    }
}

private fun previewGoal(savedAmount: Long, share: Double): Goal {
    return Goal(
        id = "preview-goal",
        name = "Car",
        targetAmount = 100_000_000L,
        startDate = "2026-10-01",
        endDate = "2030-09-30",
        inflationRate = 0.07,
        savedAmount = savedAmount,
        shareOfNewCredits = share,
        createdAt = "2026-10-01T00:00:00Z",
        updatedAt = "2026-10-01T00:00:00Z",
    )
}
