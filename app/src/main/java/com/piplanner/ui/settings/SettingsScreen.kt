package com.piplanner.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/**
 * Settings — design frames 20 / 20a / 20b / 20c (PRD R17).
 * Reachable from Goals header gear (assumption A4 / Spec §5.3).
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenConsent: () -> Unit,
    onResetToWelcome: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.navigateToConsent) {
        if (uiState.navigateToConsent) {
            viewModel.consumeConsentNavigation()
            onOpenConsent()
        }
    }

    LaunchedEffect(uiState.shouldNavigateToWelcome) {
        if (uiState.shouldNavigateToWelcome) {
            viewModel.consumeWelcomeNavigation()
            onResetToWelcome()
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onConsentToggle = viewModel::onConsentToggle,
        onRequestReset = viewModel::requestResetDemo,
        onConfirmReset = viewModel::confirmResetDemo,
        onDismissReset = viewModel::dismissResetConfirmation,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onConsentToggle: (Boolean) -> Unit,
    onRequestReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onDismissReset: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Settings screen" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Done" },
            ) {
                Text(stringResource(R.string.done))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            LinkedAccountsSection(accounts = uiState.linkedAccounts)

            AutomaticBalanceUpdatesSection(
                consentOn = uiState.consentAutoUpdate,
                dedicatedTitle = uiState.dedicatedAccountTitle,
                enabled = !uiState.isWorking,
                onToggle = onConsentToggle,
            )

            ResetDemoSection(
                enabled = !uiState.isWorking,
                onReset = onRequestReset,
            )
        }
    }

    if (uiState.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissReset,
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmReset,
                    modifier = Modifier.semantics {
                        contentDescription = "Confirm reset demo"
                    },
                ) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissReset,
                    modifier = Modifier.semantics {
                        contentDescription = "Cancel reset demo"
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.settings_error_title)) },
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
private fun LinkedAccountsSection(accounts: List<LinkedAccountRow>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics { contentDescription = "Linked accounts" },
    ) {
        Text(
            text = stringResource(R.string.settings_linked_accounts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.settings_linked_accounts_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (accounts.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_no_accounts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            accounts.forEach { account ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Linked account ${account.title}"
                        },
                ) {
                    Text(
                        text = account.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = account.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = account.formattedBalance,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AutomaticBalanceUpdatesSection(
    consentOn: Boolean,
    dedicatedTitle: String?,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics {
            contentDescription = "Automatic balance updates"
        },
    ) {
        Text(
            text = stringResource(R.string.settings_auto_updates_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (consentOn) {
                stringResource(R.string.settings_auto_updates_on_body)
            } else {
                stringResource(R.string.settings_auto_updates_off_body)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        dedicatedTitle?.let { title ->
            Text(
                text = stringResource(R.string.settings_auto_updates_account, title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (consentOn) {
                    stringResource(R.string.settings_consent_on)
                } else {
                    stringResource(R.string.settings_consent_off)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Switch(
                checked = consentOn,
                onCheckedChange = onToggle,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = "Automatic balance updates switch"
                },
            )
        }
    }
}

@Composable
private fun ResetDemoSection(
    enabled: Boolean,
    onReset: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = "Reset demo section" },
    ) {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.settings_reset_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.settings_reset_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onReset,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Reset demo" },
        ) {
            Text(
                text = stringResource(R.string.settings_reset_cta),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
