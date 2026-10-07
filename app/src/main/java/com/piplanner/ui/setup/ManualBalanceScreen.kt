package com.piplanner.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Manual amount · setup — design frame 4a (PRD R4 / R8). Continue disabled at ₹0 (PIP-78 chrome).
 */
@Composable
fun ManualBalanceScreen(
    viewModel: ConsentViewModel,
    onContinue: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldContinueAfterBalance) {
        if (uiState.shouldContinueAfterBalance) {
            viewModel.consumeNavigation()
            onContinue()
        }
    }

    ManualBalanceContent(
        uiState = uiState,
        formattedAmount = viewModel.formattedBalance(uiState.manualAmountPaisa),
        onDigitsChange = viewModel::setManualRupeeDigits,
        onContinue = viewModel::continueManual,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun ManualBalanceContent(
    uiState: ConsentUiState,
    formattedAmount: String,
    onDigitsChange: (String) -> Unit,
    onContinue: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Manual balance screen" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.manual_balance_title),
                style = PiPlannerTypography.title,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.manual_balance_subtitle),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }

        PiCard(contentDescription = "Amount card") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                Text(
                    text = stringResource(R.string.amount_label),
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "₹",
                        style = PiPlannerTypography.amountHero,
                        fontWeight = FontWeight.Bold,
                        color = PiPlannerColors.NavyPrimary,
                    )
                    BasicTextField(
                        value = uiState.manualRupeeDigits,
                        onValueChange = onDigitsChange,
                        singleLine = true,
                        textStyle = PiPlannerTypography.amountHero.copy(
                            color = PiPlannerColors.NavyPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                        cursorBrush = SolidColor(PiPlannerColors.NavyPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { inner ->
                            if (uiState.manualRupeeDigits.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = PiPlannerTypography.amountHero,
                                    color = PiPlannerColors.NavyPrimary.copy(alpha = 0.35f),
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Opening balance in rupees" },
                    )
                }
                Text(
                    text = formattedAmount,
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    modifier = Modifier.semantics {
                        contentDescription = "Formatted amount $formattedAmount"
                    },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (uiState.isWorking) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = PiPlannerDimens.Space12),
                color = PiPlannerColors.NavyPrimary,
            )
        } else {
            PrimaryCta(
                text = stringResource(R.string.continue_label),
                onClick = onContinue,
                enabled = uiState.canContinueManual,
                contentDescription = "Continue",
            )
        }
    }

    uiState.errorMessage?.let { message ->
        if (!uiState.isWorking) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(stringResource(R.string.manual_balance_save_failed_title)) },
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
