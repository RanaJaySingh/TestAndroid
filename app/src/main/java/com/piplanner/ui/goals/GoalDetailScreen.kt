package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType

/**
 * Goal detail · design frame 14 (PRD R10).
 * Shows metrics, From History, Transfer / Edit / Delete, and held-edit info (13g).
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .semantics { contentDescription = "Goal detail" },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }

        val goal = uiState.goal
        if (uiState.missing || goal == null) {
            Text(
                text = stringResource(R.string.goal_detail_missing),
                style = MaterialTheme.typography.bodyLarge,
            )
            return@Column
        }

        Text(
            text = goal.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "Goal name ${goal.name}" },
        )

        if (uiState.showHeldEditInfo) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Edit held info" },
            ) {
                Text(
                    text = uiState.heldEditInfoMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        MetricBlock(
            title = stringResource(R.string.goal_saved_amount_label),
            value = uiState.formattedSaved,
            contentDescription = "Saved amount ${uiState.formattedSaved}",
        )
        MetricBlock(
            title = stringResource(R.string.goal_status_label),
            value = uiState.statusLabel,
            contentDescription = "Status ${uiState.statusLabel}",
        )
        MetricBlock(
            title = stringResource(R.string.inflation_adjusted_target),
            value = uiState.formattedAdjustedTarget,
            contentDescription = "Adjusted target ${uiState.formattedAdjustedTarget}",
        )
        MetricBlock(
            title = stringResource(R.string.goal_monthly_need),
            value = uiState.formattedMonthlyNeed,
            contentDescription = "Monthly need ${uiState.formattedMonthlyNeed}",
        )
        MetricBlock(
            title = stringResource(R.string.goal_dates_label),
            value = stringResource(
                R.string.goal_dates_value,
                goal.startDate,
                goal.endDate,
            ),
            contentDescription = "Dates ${goal.startDate} to ${goal.endDate}",
        )
        MetricBlock(
            title = stringResource(R.string.inflation_title),
            value = uiState.inflationPercentLabel,
            contentDescription = "Inflation ${uiState.inflationPercentLabel}",
        )
        MetricBlock(
            title = stringResource(R.string.goal_share_label),
            value = uiState.sharePercentLabel,
            contentDescription = "Share ${uiState.sharePercentLabel}",
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.goal_from_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (uiState.relatedHistory.isEmpty()) {
                Text(
                    text = stringResource(R.string.goal_from_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.relatedHistory.forEach { entry ->
                    HistoryRow(
                        entry = entry,
                        amountLabel = viewModel.formatHistoryAmount(entry),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onTransfer(goal.id) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Transfer" },
            ) {
                Text(stringResource(R.string.transfer))
            }
            Button(
                onClick = { onEdit(goal.id) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Edit goal" },
            ) {
                Text(stringResource(R.string.edit))
            }
            OutlinedButton(
                onClick = { onDelete(goal.id) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Delete goal" },
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun MetricBlock(
    title: String,
    value: String,
    contentDescription: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
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
            .semantics {
                contentDescription = "History ${entry.type.name} $amountLabel"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = historyTypeLabel(entry.type),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = entry.createdAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = amountLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
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
        HistoryEntryType.GoalDeleted -> stringResource(R.string.history_type_deleted)
    }
}
