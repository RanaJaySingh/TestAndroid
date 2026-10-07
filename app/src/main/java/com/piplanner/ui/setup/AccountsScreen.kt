package com.piplanner.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.piplanner.data.model.Account
import com.piplanner.util.DemoData

/**
 * Accounts screen — design frame 2 (Step 1 of 3). Pick exactly one Dedicated savings (BR-1 / R2).
 */
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    accounts: List<Account>,
    onNavigateToConsent: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(accounts.map { it.id to it.isDedicated }) {
        viewModel.configure(accounts)
    }

    LaunchedEffect(uiState.shouldNavigateToConsent) {
        if (uiState.shouldNavigateToConsent) {
            viewModel.consumeNavigation()
            onNavigateToConsent()
        }
    }

    AccountsContent(
        uiState = uiState,
        formattedBalance = viewModel::formattedBalance,
        onDedicatedChange = viewModel::setDedicated,
        onContinue = viewModel::onContinue,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun AccountsContent(
    uiState: AccountsUiState,
    formattedBalance: (String) -> String,
    onDedicatedChange: (String, Boolean) -> Unit,
    onContinue: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.accounts_step_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.accounts_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.accounts_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            uiState.accounts.forEach { account ->
                AccountDedicatedRow(
                    account = account,
                    balanceLabel = formattedBalance(account.id),
                    onDedicatedChange = { dedicated ->
                        onDedicatedChange(account.id, dedicated)
                    },
                )
            }
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.canContinue) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        )

        Button(
            onClick = onContinue,
            enabled = uiState.canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Continue"
                },
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(20.dp)
                        .width(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.continue_label),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.accounts_save_failed_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun AccountDedicatedRow(
    account: Account,
    balanceLabel: String,
    onDedicatedChange: (Boolean) -> Unit,
) {
    val title = "${account.bankName} ${account.maskedNumber}"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = balanceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (DemoData.isSpendingAccount(account)) {
                    Text(
                        text = stringResource(R.string.accounts_spending_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "accounts.spendingNote"
                        },
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.dedicated_savings_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = account.isDedicated,
                    onCheckedChange = onDedicatedChange,
                    modifier = Modifier.semantics {
                        contentDescription = "Dedicated savings for $title"
                    },
                )
            }
        }
    }
}
