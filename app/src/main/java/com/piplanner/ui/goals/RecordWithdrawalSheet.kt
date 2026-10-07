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
import com.piplanner.domain.WithdrawalService

/**
 * Manual "Record a withdrawal" entry (frame 18c) — enter new lower balance, then open 18.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordWithdrawalSheet(
    currentFormatted: String,
    currentBalancePaisa: Long,
    onContinue: (newBalancePaisa: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rupeeDigits by remember { mutableStateOf("") }
    val newBalancePaisa = (rupeeDigits.toLongOrNull() ?: 0L) * FormattingService.PAISA_PER_RUPEE
    val canContinue = newBalancePaisa < currentBalancePaisa && newBalancePaisa >= 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .semantics { contentDescription = "Record a withdrawal sheet" },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = WithdrawalService.RECORD_WITHDRAWAL_TITLE,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.withdrawal_record_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.goals_update_current, currentFormatted),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = rupeeDigits,
                onValueChange = { raw ->
                    rupeeDigits = raw.filter { it.isDigit() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "New balance after withdrawal" },
                label = { Text(stringResource(R.string.withdrawal_record_new_balance)) },
                prefix = { Text("₹") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if (rupeeDigits.isNotEmpty() && !canContinue) {
                Text(
                    text = stringResource(R.string.withdrawal_record_must_be_lower),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { onContinue(newBalancePaisa) },
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Continue to withdrawal from record" },
            ) {
                Text(stringResource(R.string.goals_sync_withdrawal_cta))
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
