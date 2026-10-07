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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.domain.ConsentService

/**
 * Consent sheet — design frame 3 (PRD R3 / R4).
 * Hosts Yes/No; navigation to fetched balance (3a) or Update balance (4).
 */
@Composable
fun ConsentSheet(
    viewModel: ConsentViewModel,
    onYesFetched: () -> Unit,
    onNo: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldShowFetchedBalance) {
        if (uiState.shouldShowFetchedBalance) {
            viewModel.consumeNavigation()
            onYesFetched()
        }
    }

    LaunchedEffect(uiState.shouldShowUpdateBalance) {
        if (uiState.shouldShowUpdateBalance) {
            viewModel.consumeNavigation()
            onNo()
        }
    }

    ConsentSheetContent(
        uiState = uiState,
        onYes = viewModel::chooseConsentYes,
        onNo = viewModel::chooseConsentNo,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun ConsentSheetContent(
    uiState: ConsentUiState,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .semantics { contentDescription = "Consent sheet" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.consent_step_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.consent_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = uiState.dedicatedAccountTitle?.let { title ->
                    stringResource(R.string.consent_subtitle_named, title)
                } ?: stringResource(R.string.consent_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ConsentService.consentBullets.forEach { bullet ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyLarge)
                    Text(bullet, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onYes,
                enabled = !uiState.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Yes, update automatically" },
            ) {
                if (uiState.isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.consent_yes),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            OutlinedButton(
                onClick = onNo,
                enabled = !uiState.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "No, I’ll update it myself" },
            ) {
                Text(
                    text = stringResource(R.string.consent_no),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    uiState.errorMessage?.let { message ->
        if (!uiState.isWorking) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(stringResource(R.string.consent_fetch_failed_title)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }
    }
}

/** Opening balance fetched — design frame 3a (Consent Yes / PIN success). */
@Composable
fun FetchedBalanceScreen(
    viewModel: ConsentViewModel,
    onContinue: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val balanceLabel = viewModel.formattedBalance(uiState.resolvedBalancePaisa ?: 0L)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = "Fetched balance screen" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.fetched_balance_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.fetched_balance_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.balance_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Balance $balanceLabel"
                },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Continue" },
        ) {
            Text(
                text = stringResource(R.string.continue_label),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Update balance sheet (setup) — design frame 4 (Consent No). */
@Composable
fun UpdateBalanceSheet(
    onManually: () -> Unit,
    onBalanceSync: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = "Update balance sheet" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.update_balance_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.update_balance_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onManually,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Manually" },
        ) {
            Text(
                text = stringResource(R.string.update_balance_manually),
                fontWeight = FontWeight.SemiBold,
            )
        }
        OutlinedButton(
            onClick = onBalanceSync,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Balance sync" },
        ) {
            Text(
                text = stringResource(R.string.update_balance_sync),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

/** Account on another UPI app — design frame 4c (forces manual). */
@Composable
fun OtherAppScreen(
    onContinueManual: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = "Other UPI app screen" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.other_app_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.other_app_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onContinueManual,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Enter balance manually" },
        ) {
            Text(
                text = stringResource(R.string.other_app_cta),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Wrong PIN — design frames 4d / 4e (retry or manual). */
@Composable
fun WrongPinScreen(
    viewModel: ConsentViewModel,
    onRetry: () -> Unit,
    onManual: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = "Wrong PIN screen" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.wrong_pin_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = uiState.errorMessage
                ?: stringResource(R.string.wrong_pin_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                viewModel.retryPin()
                onRetry()
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Try again" },
        ) {
            Text(
                text = stringResource(R.string.wrong_pin_retry),
                fontWeight = FontWeight.SemiBold,
            )
        }
        OutlinedButton(
            onClick = {
                viewModel.clearPin()
                onManual()
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Enter manually" },
        ) {
            Text(
                text = stringResource(R.string.wrong_pin_manual),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
