package com.piplanner.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * UPI PIN (Demo) — design frame 4b (PRD R4 / R8). Demo PIN `"1234"`. PIP-78 chrome.
 */
@Composable
fun UPIPinScreen(
    viewModel: ConsentViewModel,
    onSuccess: () -> Unit,
    onWrongPin: () -> Unit,
    onCancel: () -> Unit,
    onOtherApp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldShowFetchedBalance) {
        if (uiState.shouldShowFetchedBalance) {
            viewModel.consumeNavigation()
            onSuccess()
        }
    }

    LaunchedEffect(uiState.shouldShowWrongPin) {
        if (uiState.shouldShowWrongPin) {
            viewModel.consumeNavigation()
            onWrongPin()
        }
    }

    LaunchedEffect(uiState.shouldShowOtherApp) {
        if (uiState.shouldShowOtherApp) {
            viewModel.consumeNavigation()
            onOtherApp()
        }
    }

    UPIPinContent(
        uiState = uiState,
        bankTitle = uiState.dedicatedAccountTitle,
        onDigit = viewModel::appendPinDigit,
        onDelete = viewModel::deletePinDigit,
        onCheckBalance = viewModel::checkBalanceWithPin,
        onCancel = {
            viewModel.cancelPin()
            onCancel()
        },
        onOtherApp = viewModel::accountOnOtherUpiApp,
    )
}

@Composable
fun UPIPinContent(
    uiState: ConsentUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onCheckBalance: () -> Unit,
    onCancel: () -> Unit,
    onOtherApp: () -> Unit,
    bankTitle: String? = uiState.dedicatedAccountTitle,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "UPI PIN screen" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space24),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        ) {
            UPIDemoBadge()
            if (bankTitle != null) {
                UPIBankMaskedLine(title = bankTitle)
            }
            Text(
                text = stringResource(R.string.upi_pin_subtitle),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.upi_pin_demo_hint),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.upi_pin_no_debit),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
        }

        UPIPinDots(filledCount = uiState.pinDigits.length)

        UPIMockPad(
            onDigit = onDigit,
            onDelete = onDelete,
            enabled = !uiState.isWorking,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        ) {
            if (uiState.isWorking) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = PiPlannerDimens.Space12),
                    color = PiPlannerColors.NavyPrimary,
                )
            } else {
                PrimaryCta(
                    text = stringResource(R.string.upi_pin_check_balance),
                    onClick = onCheckBalance,
                    enabled = uiState.canCheckPin,
                    contentDescription = "Check balance",
                )
            }
            SecondaryCta(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                enabled = !uiState.isWorking,
                style = SecondaryCtaStyle.Outline,
                contentDescription = "Cancel",
            )
        }

        SecondaryCta(
            text = stringResource(R.string.upi_pin_other_app),
            onClick = onOtherApp,
            enabled = !uiState.isWorking,
            style = SecondaryCtaStyle.Text,
            contentDescription = "Account on another UPI app",
        )
    }
}
