package com.piplanner.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Account
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme
import com.piplanner.ui.theme.PiPlannerTypography
import com.piplanner.util.DemoData

/**
 * Accounts screen — design frame 2 (Step 1 of 3). Pick exactly one Dedicated savings (BR-1 / R2 / visual R7).
 * Visual-only restyle (PIP-76): PiCard rows, navy step/toggle/CTA, gated Continue — no ViewModel changes.
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
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Accounts screen" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space24),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.accounts_step_label),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.NavyPrimary,
                modifier = Modifier.semantics { contentDescription = "accounts.step" },
            )
            Text(
                text = stringResource(R.string.accounts_title),
                style = PiPlannerTypography.title,
                color = PiPlannerColors.OnSurface,
                modifier = Modifier.semantics { contentDescription = "accounts.title" },
            )
            Text(
                text = stringResource(R.string.accounts_subtitle),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            modifier = Modifier.semantics { contentDescription = "accounts.list" },
        ) {
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
            style = PiPlannerTypography.caption,
            color = if (uiState.canContinue) {
                PiPlannerColors.OnSurface.copy(alpha = 0.72f)
            } else {
                PiPlannerColors.Behind
            },
            modifier = Modifier.semantics { contentDescription = "accounts.status" },
        )

        if (uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PiPlannerDimens.Space12),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .semantics { contentDescription = "Continue" },
                    color = PiPlannerColors.NavyPrimary,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            PrimaryCta(
                text = stringResource(R.string.continue_label),
                onClick = onContinue,
                enabled = uiState.canContinue,
                contentDescription = "Continue",
            )
        }

        Spacer(modifier = Modifier.height(PiPlannerDimens.Space8))
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
    val roleLabel = if (DemoData.isSpendingAccount(account)) {
        stringResource(R.string.accounts_role_spending)
    } else {
        stringResource(R.string.accounts_role_savings)
    }

    PiCard(contentDescription = "Account card $title") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
            ) {
                Text(
                    text = roleLabel,
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Text(
                    text = title,
                    style = PiPlannerTypography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = PiPlannerColors.OnSurface,
                )
                Text(
                    text = balanceLabel,
                    style = PiPlannerTypography.body,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                if (DemoData.isSpendingAccount(account)) {
                    Text(
                        text = stringResource(R.string.accounts_spending_note),
                        style = PiPlannerTypography.caption,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                        modifier = Modifier.semantics {
                            contentDescription = "accounts.spendingNote"
                        },
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.dedicated_savings_label),
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Switch(
                    checked = account.isDedicated,
                    onCheckedChange = onDedicatedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PiPlannerColors.OnNavy,
                        checkedTrackColor = PiPlannerColors.NavyPrimary,
                        checkedBorderColor = PiPlannerColors.NavyPrimary,
                        uncheckedThumbColor = PiPlannerColors.OnNavy,
                        uncheckedTrackColor = PiPlannerColors.OutlineMuted,
                        uncheckedBorderColor = PiPlannerColors.OutlineMuted,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Dedicated savings for $title"
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Accounts · none dedicated · Continue disabled")
@Composable
private fun AccountsNoneDedicatedPreview() {
    PiPlannerTheme {
        AccountsContent(
            uiState = AccountsUiState(
                accounts = DemoData.sampleAccounts(),
                canContinue = false,
                statusMessage = "Pick one Dedicated savings account to continue.",
            ),
            formattedBalance = { id ->
                when (id) {
                    DemoData.DEMO_SAVINGS_ACCOUNT_ID -> "₹1,00,000"
                    else -> "₹72,000"
                }
            },
            onDedicatedChange = { _, _ -> },
            onContinue = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true, name = "Accounts · one dedicated · Continue enabled")
@Composable
private fun AccountsOneDedicatedPreview() {
    PiPlannerTheme {
        AccountsContent(
            uiState = AccountsUiState(
                accounts = DemoData.seededPersonaAccounts(),
                canContinue = true,
                statusMessage = "Dedicated: HDFC ••4821.",
            ),
            formattedBalance = { id ->
                when (id) {
                    DemoData.DEMO_SAVINGS_ACCOUNT_ID -> "₹1,00,000"
                    else -> "₹72,000"
                }
            },
            onDedicatedChange = { _, _ -> },
            onContinue = {},
            onDismissError = {},
        )
    }
}
