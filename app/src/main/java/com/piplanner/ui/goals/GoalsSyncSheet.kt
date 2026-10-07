package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piplanner.R

/**
 * Sync sheet for Consent On — frames 10 / 10a / 10b (PIP-48).
 * Also available as [SyncSheet] for ticket naming.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsSyncSheet(
    phase: SyncSheetPhase,
    formattedPrevious: String,
    formattedFetched: String?,
    formattedNewAmount: String?,
    infoMessage: String?,
    errorMessage: String?,
    isBlockedByOpenEntry: Boolean,
    canContinueToCreditEntry: Boolean,
    canContinueToWithdrawal: Boolean,
    onSync: () -> Unit,
    onContinueToCreditEntry: () -> Unit,
    onContinueToWithdrawal: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.semantics { contentDescription = "Goals sync sheet" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.goals_sync_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.goals_sync_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SyncAmountRow(
                    label = stringResource(R.string.credit_entry_previous),
                    value = formattedPrevious,
                )
                if (formattedFetched != null) {
                    SyncAmountRow(
                        label = stringResource(R.string.goals_sync_fetched),
                        value = formattedFetched,
                    )
                }
                if (formattedNewAmount != null) {
                    SyncAmountRow(
                        label = stringResource(R.string.credit_entry_new_amount),
                        value = formattedNewAmount,
                    )
                }
            }

            if (infoMessage != null) {
                Text(
                    text = infoMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { contentDescription = "Sync info" },
                )
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            when {
                phase == SyncSheetPhase.Syncing -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.goals_syncing))
                }
                canContinueToCreditEntry -> {
                    Button(
                        onClick = onContinueToCreditEntry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Continue to credit entry" },
                    ) {
                        Text(stringResource(R.string.continue_label))
                    }
                }
                canContinueToWithdrawal -> {
                    Button(
                        onClick = onContinueToWithdrawal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Continue to withdrawal" },
                    ) {
                        Text(stringResource(R.string.goals_sync_withdrawal_cta))
                    }
                }
                else -> {
                    Button(
                        onClick = onSync,
                        enabled = !isBlockedByOpenEntry && phase != SyncSheetPhase.Syncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Sync now" },
                    ) {
                        Text(stringResource(R.string.goals_sync_now))
                    }
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

/** Ticket-facing alias for [GoalsSyncSheet]. */
@Composable
fun SyncSheet(
    phase: SyncSheetPhase,
    formattedPrevious: String,
    formattedFetched: String?,
    formattedNewAmount: String?,
    infoMessage: String?,
    errorMessage: String?,
    isBlockedByOpenEntry: Boolean,
    canContinueToCreditEntry: Boolean,
    canContinueToWithdrawal: Boolean,
    onSync: () -> Unit,
    onContinueToCreditEntry: () -> Unit,
    onContinueToWithdrawal: () -> Unit,
    onDismiss: () -> Unit,
) {
    GoalsSyncSheet(
        phase = phase,
        formattedPrevious = formattedPrevious,
        formattedFetched = formattedFetched,
        formattedNewAmount = formattedNewAmount,
        infoMessage = infoMessage,
        errorMessage = errorMessage,
        isBlockedByOpenEntry = isBlockedByOpenEntry,
        canContinueToCreditEntry = canContinueToCreditEntry,
        canContinueToWithdrawal = canContinueToWithdrawal,
        onSync = onSync,
        onContinueToCreditEntry = onContinueToCreditEntry,
        onContinueToWithdrawal = onContinueToWithdrawal,
        onDismiss = onDismiss,
    )
}

enum class SyncSheetPhase {
    Idle,
    Syncing,
    ShowingResult,
}

@Composable
private fun SyncAmountRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
