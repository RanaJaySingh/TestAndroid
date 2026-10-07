package com.piplanner.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.domain.HistoryService
import com.piplanner.ui.components.EntryBadge
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Read-only History entry detail — Transfer / Withdrawal / Goal deleted / locked New credit.
 *
 * PIP-86: locked New credit uses Saved and locked + Typed/Custom badges and a
 * this-credit allocations card (visual only; no behaviour change).
 * Prefer main shared chrome: PIP-72 [PiIcons], PIP-70/74 [PiCard].
 */
@Composable
fun HistoryEntryDetailScreen(
    viewModel: HistoryDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryEntryDetailContent(
        uiState = uiState,
        onBack = onBack,
    )
}

@Composable
fun HistoryEntryDetailContent(
    uiState: HistoryDetailUiState,
    onBack: () -> Unit,
) {
    val entry = uiState.entry
    val isNewCredit = entry?.type == HistoryEntryType.NewCredit
    val isTyped = entry?.isTyped == true
    val showsCustomBadge = isNewCredit && entry?.isLocked == true && !isTyped

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space16)
            .semantics { contentDescription = "History entry detail read-only" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = "Back from history detail" },
        ) {
            Text(
                text = stringResource(R.string.back),
                color = PiPlannerColors.NavyPrimary,
            )
        }

        if (uiState.missing) {
            Text(
                text = stringResource(R.string.history_detail_missing),
                style = MaterialTheme.typography.bodyLarge,
                color = PiPlannerColors.OnSurface,
            )
            return
        }

        if (isNewCredit) {
            NewCreditLockedHeader(
                isTyped = isTyped,
                showsCustomBadge = showsCustomBadge,
            )
        } else {
            GenericHistoryHeader(uiState = uiState)
        }

        Text(
            text = uiState.caption.ifBlank { HistoryService.ORIGINAL_AMOUNTS_CAPTION },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = HistoryService.ORIGINAL_AMOUNTS_CAPTION
            },
        )

        PiCard(contentDescription = "History detail summary") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                HistoryLabeledRow(
                    label = stringResource(R.string.history_detail_amount),
                    value = uiState.formattedAmount,
                    emphasize = true,
                    valueDescription = "History detail amount ${uiState.formattedAmount}",
                )
                // Typed New credit (13t): omit Previous / Balance — match CreditEntryScreen.
                val showBalanceLines = !(isNewCredit && isTyped)
                if (showBalanceLines) {
                    uiState.detailLines.firstOrNull { it.startsWith("Previous") }?.let { line ->
                        HistoryLabeledRow(
                            label = if (isNewCredit) {
                                stringResource(R.string.history_detail_previous_balance)
                            } else {
                                stringResource(R.string.credit_entry_previous)
                            },
                            value = line.removePrefix("Previous ").trim(),
                        )
                    }
                    uiState.detailLines.firstOrNull { it.startsWith("Balance now") }?.let { line ->
                        HistoryLabeledRow(
                            label = if (isNewCredit) {
                                stringResource(R.string.history_detail_balance_after)
                            } else {
                                stringResource(R.string.credit_entry_balance_now)
                            },
                            value = line.removePrefix("Balance now ").trim(),
                        )
                    }
                }
                if (!isNewCredit) {
                    uiState.detailLines
                        .filterNot {
                            it.startsWith("Previous") ||
                                it.startsWith("Balance now") ||
                                it == "Typed"
                        }
                        .forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PiPlannerColors.OnSurface,
                            )
                        }
                }
            }
        }

        if (uiState.allocationRows.isNotEmpty()) {
            PiCard(contentDescription = "History detail allocations") {
                Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
                    Text(
                        text = if (isNewCredit) {
                            entry?.creditAmount?.let {
                                stringResource(
                                    R.string.credit_entry_this_credit_split,
                                    uiState.formattedAmount,
                                )
                            } ?: stringResource(R.string.history_detail_this_credit_only)
                        } else {
                            stringResource(R.string.history_detail_allocations)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isNewCredit) {
                            PiPlannerColors.NavyPrimary
                        } else {
                            PiPlannerColors.OnSurface
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (isNewCredit) {
                                "This credit header"
                            } else {
                                "Allocations header"
                            }
                        },
                    )
                    uiState.allocationRows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription =
                                        "Allocation ${row.goalName} ${row.formattedAmount} ${row.percentLabel}"
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = row.goalName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = PiPlannerColors.OnSurface,
                                )
                                Text(
                                    text = row.percentLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = row.formattedAmount,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = PiPlannerColors.NavyPrimary,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = HistoryService.ORIGINAL_AMOUNTS_CAPTION,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = "history detail original caption"
            },
        )
    }
}

@Composable
private fun NewCreditLockedHeader(
    isTyped: Boolean,
    showsCustomBadge: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
        Text(
            text = stringResource(R.string.credit_entry_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            color = PiPlannerColors.NavyPrimary.copy(alpha = 0.72f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Icon(
                    imageVector = PiIcons.lock,
                    contentDescription = stringResource(R.string.credit_entry_locked_a11y),
                    tint = PiPlannerColors.NavyPrimary,
                    modifier = Modifier
                        .size(22.dp)
                        .semantics { contentDescription = "History detail lock icon" },
                )
                Text(
                    text = stringResource(R.string.credit_entry_saved_and_locked),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.NavyPrimary,
                    modifier = Modifier.semantics {
                        contentDescription = "Saved and locked"
                    },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                if (isTyped) {
                    EntryBadge(
                        label = stringResource(R.string.credit_entry_typed_badge),
                        contentDescription = "Typed credit badge",
                    )
                }
                if (showsCustomBadge) {
                    EntryBadge(
                        label = stringResource(R.string.credit_entry_custom_badge),
                        contentDescription = "Custom credit badge",
                    )
                }
            }
        }
    }
}

@Composable
private fun GenericHistoryHeader(uiState: HistoryDetailUiState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
    ) {
        Icon(
            imageVector = PiIcons.resolve(uiState.typeIcon),
            contentDescription = uiState.typeLabel,
            tint = PiPlannerColors.NavyPrimary.copy(alpha = 0.72f),
            modifier = Modifier.size(32.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
            ) {
                Text(
                    text = uiState.typeLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.NavyPrimary,
                )
                if (uiState.isReadOnly) {
                    Icon(
                        imageVector = PiIcons.lock,
                        contentDescription = stringResource(R.string.credit_entry_locked_a11y),
                        tint = PiPlannerColors.NavyPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = uiState.dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryLabeledRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    valueDescription: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) PiPlannerColors.NavyPrimary else PiPlannerColors.OnSurface,
            modifier = if (valueDescription != null) {
                Modifier.semantics { contentDescription = valueDescription }
            } else {
                Modifier
            },
        )
    }
}
