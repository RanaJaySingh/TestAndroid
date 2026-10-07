package com.piplanner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.piplanner.R
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheet
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme
import com.piplanner.ui.theme.PiPlannerTypography
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Settings — design frames 20 / 20a / 20b / 20c (PRD R16).
 * Reachable from Goals header gear (assumption A6 / Spec §5.3).
 * Visual parity (PIP-94): tokens + PiCard / PiSheet grouping — no behaviour change.
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

@OptIn(ExperimentalMaterial3Api::class)
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
            .background(PiPlannerColors.BackgroundApp)
            .semantics { contentDescription = "Settings screen" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PiPlannerDimens.Space8,
                    vertical = PiPlannerDimens.Space8,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Done" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PiPlannerColors.NavyPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.done),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnBackground,
            )
            // Balance Done so the title stays visually centered.
            Box(modifier = Modifier.padding(horizontal = PiPlannerDimens.Space16)) {
                Text(
                    text = stringResource(R.string.done),
                    color = PiPlannerColors.BackgroundApp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PiPlannerDimens.Space20)
                .padding(
                    top = PiPlannerDimens.Space8,
                    bottom = PiPlannerDimens.Space28,
                ),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space28),
        ) {
            LinkedAccountsSection(accounts = uiState.linkedAccounts)

            AutomaticBalanceUpdatesSection(
                consentOn = uiState.consentAutoUpdate,
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
        PiSheet(
            onDismissRequest = onDismissReset,
            contentDescription = "Reset demo confirmation",
        ) {
            Text(
                text = stringResource(R.string.settings_reset_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.settings_reset_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Button(
                onClick = onConfirmReset,
                enabled = !uiState.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Confirm reset demo" },
                shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PiPlannerColors.Destructive,
                    contentColor = PiPlannerColors.OnNavy,
                    disabledContainerColor = PiPlannerColors.Destructive.copy(alpha = 0.38f),
                    disabledContentColor = PiPlannerColors.OnNavy.copy(alpha = 0.70f),
                ),
                contentPadding = PaddingValues(
                    horizontal = PiPlannerDimens.Space20,
                    vertical = PiPlannerDimens.Space12,
                ),
            ) {
                Text(
                    text = stringResource(R.string.settings_reset_cta),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            SecondaryCta(
                text = stringResource(R.string.cancel),
                onClick = onDismissReset,
                enabled = !uiState.isWorking,
                style = SecondaryCtaStyle.Outline,
                contentDescription = "Cancel reset demo",
            )
        }
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
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title.uppercase(),
            style = PiPlannerTypography.caption,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.60f),
        )
        PiCard(contentDescription = title) {
            content()
        }
    }
}

@Composable
private fun LinkedAccountsSection(accounts: List<LinkedAccountRow>) {
    SettingsGroup(title = stringResource(R.string.settings_linked_accounts)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
            modifier = Modifier.semantics { contentDescription = "Linked accounts" },
        ) {
            if (accounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_accounts),
                    style = PiPlannerTypography.body,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
            } else {
                accounts.forEachIndexed { index, account ->
                    if (index > 0) {
                        HorizontalDivider(color = PiPlannerColors.OutlineMuted.copy(alpha = 0.5f))
                    }
                    LinkedAccountRowView(account = account)
                }
            }
        }
    }
}

@Composable
private fun LinkedAccountRowView(account: LinkedAccountRow) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Linked account ${account.title}"
            },
    ) {
        Text(
            text = account.roleLabel,
            style = PiPlannerTypography.caption,
            fontWeight = FontWeight.Medium,
            color = PiPlannerColors.NavyPrimary,
        )
        Text(
            text = account.title,
            style = PiPlannerTypography.body,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface,
        )
        Text(
            text = account.formattedBalance,
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
        )
        Text(
            text = account.linkSubtitle,
            style = PiPlannerTypography.caption,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun AutomaticBalanceUpdatesSection(
    consentOn: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_auto_updates_title)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            modifier = Modifier.semantics {
                contentDescription = "Automatic balance updates"
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_auto_updates_title),
                            style = PiPlannerTypography.body,
                            fontWeight = FontWeight.SemiBold,
                            color = PiPlannerColors.OnSurface,
                        )
                        ConsentStateChip(consentOn = consentOn)
                    }
                    Text(
                        text = if (consentOn) {
                            stringResource(R.string.settings_auto_updates_on_body)
                        } else {
                            stringResource(R.string.settings_auto_updates_off_body)
                        },
                        style = PiPlannerTypography.caption,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    )
                }
                Switch(
                    checked = consentOn,
                    onCheckedChange = onToggle,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PiPlannerColors.OnNavy,
                        checkedTrackColor = PiPlannerColors.NavyPrimary,
                        uncheckedThumbColor = PiPlannerColors.OnNavy,
                        uncheckedTrackColor = PiPlannerColors.OutlineMuted,
                        uncheckedBorderColor = PiPlannerColors.OutlineMuted,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Automatic balance updates switch"
                    },
                )
            }
            if (!consentOn) {
                Text(
                    text = stringResource(R.string.settings_untyped_gap_hint),
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    modifier = Modifier
                        .padding(top = PiPlannerDimens.Space8)
                        .semantics { contentDescription = "Untyped gap hint" },
                )
            }
        }
    }
}

/** On / Off chip — frames 20 / 20a (light-blue when On, muted when Off). */
@Composable
private fun ConsentStateChip(consentOn: Boolean) {
    val label = if (consentOn) {
        stringResource(R.string.settings_consent_on)
    } else {
        stringResource(R.string.settings_consent_off)
    }
    Text(
        text = label,
        style = PiPlannerTypography.caption,
        fontWeight = FontWeight.SemiBold,
        color = if (consentOn) {
            PiPlannerColors.OnChipLightBlue
        } else {
            PiPlannerColors.OnSurface.copy(alpha = 0.60f)
        },
        modifier = Modifier
            .clip(RoundedCornerShape(PiPlannerDimens.RadiusChip))
            .background(
                if (consentOn) {
                    PiPlannerColors.ChipLightBlue
                } else {
                    PiPlannerColors.OutlineMuted.copy(alpha = 0.28f)
                },
            )
            .padding(
                horizontal = PiPlannerDimens.Space8,
                vertical = 4.dp,
            )
            .semantics { contentDescription = "Consent state $label" },
    )
}

@Composable
private fun ResetDemoSection(
    enabled: Boolean,
    onReset: () -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_demo_group)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) {
                        Modifier.clickable(onClick = onReset)
                    } else {
                        Modifier
                    },
                )
                .semantics { contentDescription = "Reset demo" },
        ) {
            Text(
                text = stringResource(R.string.settings_reset_cta),
                style = PiPlannerTypography.body,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.Destructive,
            )
            Text(
                text = stringResource(R.string.settings_reset_body),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Preview(name = "Settings · consent On", showBackground = true)
@Composable
private fun SettingsConsentOnPreview() {
    PiPlannerTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                linkedAccounts = previewAccounts(),
                consentAutoUpdate = true,
                dedicatedAccountTitle = "HDFC ••4821",
            ),
            onBack = {},
            onConsentToggle = {},
            onRequestReset = {},
            onConfirmReset = {},
            onDismissReset = {},
            onDismissError = {},
        )
    }
}

@Preview(name = "Settings · consent Off", showBackground = true)
@Composable
private fun SettingsConsentOffPreview() {
    PiPlannerTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                linkedAccounts = previewAccounts(),
                consentAutoUpdate = false,
                dedicatedAccountTitle = "HDFC ••4821",
            ),
            onBack = {},
            onConsentToggle = {},
            onRequestReset = {},
            onConfirmReset = {},
            onDismissReset = {},
            onDismissError = {},
        )
    }
}

private fun previewAccounts(): List<LinkedAccountRow> = listOf(
    LinkedAccountRow(
        id = "dedicated",
        title = "HDFC ••4821",
        roleLabel = "Dedicated savings",
        linkSubtitle = "Linked in Paytm",
        subtitle = "Dedicated savings · Paytm linked",
        isDedicated = true,
        formattedBalance = "₹1,00,000",
    ),
    LinkedAccountRow(
        id = "spending",
        title = "SBI ••7730",
        roleLabel = "Spending",
        linkSubtitle = "Linked in Paytm",
        subtitle = "Spending · not tracked in PiPlanner · Paytm linked",
        isDedicated = false,
        formattedBalance = "₹72,000",
    ),
)
