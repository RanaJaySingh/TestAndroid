package com.piplanner.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.piplanner.domain.ConsentService
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheetChrome
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme
import com.piplanner.ui.theme.PiPlannerTypography
import com.piplanner.util.DemoData

/**
 * Consent sheet — design frame 3 (PRD R3 / R4 / visual R7).
 * Paytm-like sheet chrome via [PiSheetChrome]; Yes/No CTAs; navigation to 3a or Update balance (4).
 */
@Composable
fun ConsentSheet(
    viewModel: ConsentViewModel,
    onYesFetched: () -> Unit,
    onNo: () -> Unit,
    /** Setup shows “Step 2 of 3”; Settings (20b) hides the step label. */
    showsSetupStep: Boolean = true,
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
        showsSetupStep = showsSetupStep,
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
    showsSetupStep: Boolean = true,
) {
    val helper = uiState.dedicatedAccountTitle?.let { title ->
        stringResource(R.string.consent_subtitle_named, title)
    } ?: stringResource(R.string.consent_subtitle)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .semantics { contentDescription = "Consent sheet" },
    ) {
        PiSheetChrome(
            title = stringResource(R.string.consent_title),
            helper = helper,
            contentDescription = "Consent sheet chrome",
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space24),
            ) {
                if (showsSetupStep) {
                    Text(
                        text = stringResource(R.string.consent_step_label),
                        style = PiPlannerTypography.caption,
                        color = PiPlannerColors.NavyPrimary,
                        modifier = Modifier.semantics { contentDescription = "consent.step" },
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
                    modifier = Modifier.semantics { contentDescription = "consent.bullets" },
                ) {
                    ConsentService.consentBullets.forEachIndexed { index, bullet ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
                            modifier = Modifier.semantics {
                                contentDescription = "consent.bullet.$index"
                            },
                        ) {
                            Text(
                                text = "✓",
                                style = PiPlannerTypography.body.copy(fontWeight = FontWeight.SemiBold),
                                color = PiPlannerColors.NavyPrimary,
                            )
                            Text(
                                text = bullet,
                                style = PiPlannerTypography.body,
                                color = PiPlannerColors.OnSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
                    if (uiState.isWorking) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = PiPlannerDimens.Space12),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .semantics {
                                        contentDescription = "Yes, update automatically"
                                    },
                                color = PiPlannerColors.NavyPrimary,
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        PrimaryCta(
                            text = stringResource(R.string.consent_yes),
                            onClick = onYes,
                            enabled = true,
                            contentDescription = "Yes, update automatically",
                        )
                    }

                    SecondaryCta(
                        text = stringResource(R.string.consent_no),
                        onClick = onNo,
                        enabled = !uiState.isWorking,
                        style = SecondaryCtaStyle.Outline,
                        contentDescription = "No, I’ll update it myself",
                    )
                }
            }
        }
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

/** Opening balance fetched — design frame 3a (Consent Yes / PIN success); visual R7. */
@Composable
fun FetchedBalanceScreen(
    viewModel: ConsentViewModel,
    onContinue: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val balanceLabel = viewModel.formattedBalance(uiState.resolvedBalancePaisa ?: 0L)

    FetchedBalanceContent(
        balanceLabel = balanceLabel,
        onContinue = onContinue,
    )
}

@Composable
fun FetchedBalanceContent(
    balanceLabel: String,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Fetched balance screen" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space24),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.consent_step_label),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.NavyPrimary,
                modifier = Modifier.semantics { contentDescription = "fetchedBalance.step" },
            )
            Text(
                text = stringResource(R.string.fetched_balance_title),
                style = PiPlannerTypography.title,
                color = PiPlannerColors.OnSurface,
                modifier = Modifier.semantics { contentDescription = "fetchedBalance.title" },
            )
            Text(
                text = stringResource(R.string.fetched_balance_subtitle),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }

        PiCard(contentDescription = "Fetched balance amount card") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                Text(
                    text = stringResource(R.string.balance_label),
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Text(
                    text = balanceLabel,
                    style = PiPlannerTypography.amountHero,
                    color = PiPlannerColors.NavyPrimary,
                    modifier = Modifier.semantics {
                        contentDescription = "Balance $balanceLabel"
                    },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryCta(
            text = stringResource(R.string.continue_label),
            onClick = onContinue,
            contentDescription = "Continue",
        )
    }
}


/** Update balance sheet (setup) — design frame 4 (Consent No). Paytm-like choice rows (PIP-78). */
@Composable
fun UpdateBalanceSheet(
    onManually: () -> Unit,
    onBalanceSync: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Update balance sheet" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.update_balance_title),
                style = PiPlannerTypography.title,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.update_balance_subtitle),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
            UpdateBalanceChoiceRow(
                title = stringResource(R.string.update_balance_manually),
                subtitle = stringResource(R.string.update_balance_manually_subtitle),
                leadingIcon = UpdateBalanceChoiceIcons.Manually,
                onClick = onManually,
                contentDescription = "Manually",
            )
            UpdateBalanceChoiceRow(
                title = stringResource(R.string.update_balance_sync),
                subtitle = stringResource(R.string.update_balance_sync_subtitle),
                leadingIcon = UpdateBalanceChoiceIcons.BalanceSync,
                onClick = onBalanceSync,
                contentDescription = "Balance sync",
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

/** Account on another UPI app — design frame 4c (forces manual). PIP-78 chrome. */
@Composable
fun OtherAppScreen(
    onContinueManual: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Other UPI app screen" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.other_app_title),
                style = PiPlannerTypography.title,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.other_app_body),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        PrimaryCta(
            text = stringResource(R.string.other_app_cta),
            onClick = onContinueManual,
            contentDescription = "Enter balance manually",
        )
    }
}

/** Wrong PIN — design frames 4d / 4e (retry or manual). PIP-78 chrome (no logic change). */
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
            .background(PiPlannerColors.BackgroundApp)
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Wrong PIN screen" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.wrong_pin_title),
                style = PiPlannerTypography.title,
                color = PiPlannerColors.Destructive,
            )
            Text(
                text = uiState.errorMessage
                    ?: stringResource(R.string.wrong_pin_body),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            UPIPinDots(filledCount = 4, showsError = true)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
            PrimaryCta(
                text = stringResource(R.string.wrong_pin_retry),
                onClick = {
                    viewModel.retryPin()
                    onRetry()
                },
                contentDescription = "Try again",
            )
            SecondaryCta(
                text = stringResource(R.string.wrong_pin_manual),
                onClick = {
                    viewModel.clearPin()
                    onManual()
                },
                style = SecondaryCtaStyle.Outline,
                contentDescription = "Enter manually",
            )
        }
    }
}

@Preview(showBackground = true, name = "Consent · Yes / No")
@Composable
private fun ConsentSheetPreview() {
    PiPlannerTheme {
        ConsentSheetContent(
            uiState = ConsentUiState(accounts = DemoData.seededPersonaAccounts()),
            showsSetupStep = true,
            onYes = {},
            onNo = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true, name = "Consent · Settings 20b")
@Composable
private fun ConsentSheetSettingsPreview() {
    PiPlannerTheme {
        ConsentSheetContent(
            uiState = ConsentUiState(accounts = DemoData.seededPersonaAccounts()),
            showsSetupStep = false,
            onYes = {},
            onNo = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true, name = "Fetched balance 3a · ₹1,00,000")
@Composable
private fun FetchedBalancePreview() {
    PiPlannerTheme {
        FetchedBalanceContent(
            balanceLabel = "₹1,00,000",
            onContinue = {},
        )
    }
}
