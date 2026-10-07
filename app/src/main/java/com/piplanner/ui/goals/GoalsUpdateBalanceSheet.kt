package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.piplanner.R
import com.piplanner.domain.FormattingService

/**
 * Goals-tab Update balance sheet (Consent Off, frames 11 / 11a–11c).
 * Distinct from setup [com.piplanner.ui.setup.UpdateBalanceSheet] (frame 4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsUpdateBalanceSheet(
    currentFormatted: String,
    infoMessage: String?,
    errorMessage: String?,
    isBlockedByOpenEntry: Boolean,
    canContinueToCreditEntry: Boolean,
    canContinueToWithdrawal: Boolean,
    onApply: (Long) -> Unit,
    onContinueToCreditEntry: () -> Unit,
    onContinueToWithdrawal: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var digits by remember { mutableStateOf("") }
    val paisa = (digits.filter { it.isDigit() }.toLongOrNull() ?: 0L) *
        FormattingService.PAISA_PER_RUPEE
    val canApply = paisa > 0L && !isBlockedByOpenEntry

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.semantics { contentDescription = "Goals update balance sheet" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.update_balance_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.goals_update_current, currentFormatted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!canContinueToCreditEntry && !canContinueToWithdrawal) {
                OutlinedTextField(
                    value = digits,
                    onValueChange = { value -> digits = value.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.goals_update_new_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isBlockedByOpenEntry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "New balance digits" },
                )
                Text(
                    text = stringResource(R.string.goals_update_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (infoMessage != null) {
                Text(
                    text = infoMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = "Update balance info" },
                )
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            when {
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
                        onClick = { onApply(paisa) },
                        enabled = canApply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Apply balance" },
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.close))
            }
        }
    }
}
