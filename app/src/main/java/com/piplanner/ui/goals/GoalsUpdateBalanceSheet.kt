package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.piplanner.R
import com.piplanner.domain.FormattingService
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheet
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Goals-tab Update balance sheet (Consent Off, frames 11 / 11a–11b).
 * Distinct from setup [com.piplanner.ui.setup.UpdateBalanceSheet] (frame 4).
 * Visual parity (PIP-84 / PRD R11): Paytm-like PiSheet choice + amount chrome.
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
    onBalanceSync: () -> Unit = {},
    onContinueToCreditEntry: () -> Unit,
    onContinueToWithdrawal: () -> Unit,
    onRecordWithdrawal: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(UpdateBalanceStep.Choice) }
    var digits by remember { mutableStateOf("") }
    val paisa = (digits.filter { it.isDigit() }.toLongOrNull() ?: 0L) *
        FormattingService.PAISA_PER_RUPEE
    val canApply = paisa > 0L && !isBlockedByOpenEntry

    // Result CTAs (credit / withdrawal) take over after apply / sync — keep existing behaviour.
    val showingResult = canContinueToCreditEntry || canContinueToWithdrawal

    PiSheet(
        onDismissRequest = onDismiss,
        contentDescription = "Goals update balance sheet",
    ) {
        when {
            showingResult -> {
                UpdateResultContent(
                    currentFormatted = currentFormatted,
                    infoMessage = infoMessage,
                    errorMessage = errorMessage,
                    canContinueToCreditEntry = canContinueToCreditEntry,
                    canContinueToWithdrawal = canContinueToWithdrawal,
                    onContinueToCreditEntry = onContinueToCreditEntry,
                    onContinueToWithdrawal = onContinueToWithdrawal,
                    onDismiss = onDismiss,
                )
            }
            step == UpdateBalanceStep.Choice -> {
                UpdateChoiceContent(
                    currentFormatted = currentFormatted,
                    infoMessage = infoMessage,
                    errorMessage = errorMessage,
                    isBlockedByOpenEntry = isBlockedByOpenEntry,
                    onManually = { step = UpdateBalanceStep.Manual },
                    onBalanceSync = onBalanceSync,
                    onRecordWithdrawal = onRecordWithdrawal,
                    onDismiss = onDismiss,
                )
            }
            else -> {
                UpdateManualContent(
                    currentFormatted = currentFormatted,
                    digits = digits,
                    onDigitsChange = { value -> digits = value.filter { it.isDigit() } },
                    infoMessage = infoMessage,
                    errorMessage = errorMessage,
                    canApply = canApply,
                    isBlockedByOpenEntry = isBlockedByOpenEntry,
                    onApply = { onApply(paisa) },
                    onBack = { step = UpdateBalanceStep.Choice },
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

private enum class UpdateBalanceStep {
    Choice,
    Manual,
}

@Composable
private fun UpdateChoiceContent(
    currentFormatted: String,
    infoMessage: String?,
    errorMessage: String?,
    isBlockedByOpenEntry: Boolean,
    onManually: () -> Unit,
    onBalanceSync: () -> Unit,
    onRecordWithdrawal: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
        Text(
            text = stringResource(R.string.update_balance_title),
            style = PiPlannerTypography.title,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface,
        )
        Text(
            text = stringResource(R.string.goals_update_choice_helper),
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
        )
    }

    PiCard(contentDescription = "Current balance card") {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.goals_update_current_label),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Text(
                text = currentFormatted,
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
        }
    }

    UpdateMessages(infoMessage = infoMessage, errorMessage = errorMessage)

    PrimaryCta(
        text = stringResource(R.string.update_balance_manually),
        onClick = onManually,
        enabled = !isBlockedByOpenEntry,
        contentDescription = "Manually",
    )
    SecondaryCta(
        text = stringResource(R.string.update_balance_sync),
        onClick = onBalanceSync,
        enabled = !isBlockedByOpenEntry,
        style = SecondaryCtaStyle.Outline,
        contentDescription = "Balance sync",
    )
    if (onRecordWithdrawal != null) {
        SecondaryCta(
            text = stringResource(R.string.withdrawal_record_cta),
            onClick = onRecordWithdrawal,
            style = SecondaryCtaStyle.Text,
            contentDescription = "Record a withdrawal",
        )
    }
    SecondaryCta(
        text = stringResource(R.string.close),
        onClick = onDismiss,
        style = SecondaryCtaStyle.Text,
        contentDescription = "Close update balance sheet",
    )
}

@Composable
private fun UpdateManualContent(
    currentFormatted: String,
    digits: String,
    onDigitsChange: (String) -> Unit,
    infoMessage: String?,
    errorMessage: String?,
    canApply: Boolean,
    isBlockedByOpenEntry: Boolean,
    onApply: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
        Text(
            text = stringResource(R.string.goals_update_manual_title),
            style = PiPlannerTypography.title,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface,
        )
        Text(
            text = stringResource(R.string.goals_update_footer),
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
        )
    }

    PiCard(contentDescription = "Manual amount card") {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16)) {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                Text(
                    text = stringResource(R.string.goals_update_current_label),
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Text(
                    text = currentFormatted,
                    style = PiPlannerTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.OnSurface,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                Text(
                    text = stringResource(R.string.goals_update_new_balance_label),
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                ) {
                    Text(
                        text = FormattingService.RUPEE_SYMBOL,
                        style = PiPlannerTypography.title,
                        fontWeight = FontWeight.SemiBold,
                        color = PiPlannerColors.NavyPrimary,
                    )
                    BasicTextField(
                        value = digits,
                        onValueChange = onDigitsChange,
                        enabled = !isBlockedByOpenEntry,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = PiPlannerTypography.title.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PiPlannerColors.OnSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "New balance digits" },
                        decorationBox = { inner ->
                            if (digits.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = PiPlannerTypography.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PiPlannerColors.OnSurface.copy(alpha = 0.38f),
                                )
                            }
                            inner()
                        },
                    )
                }
            }
        }
    }

    UpdateMessages(infoMessage = infoMessage, errorMessage = errorMessage)

    PrimaryCta(
        text = stringResource(R.string.apply),
        onClick = onApply,
        enabled = canApply,
        contentDescription = "Apply balance",
    )
    SecondaryCta(
        text = stringResource(R.string.back),
        onClick = onBack,
        style = SecondaryCtaStyle.Text,
        contentDescription = "Back to update choice",
    )
    SecondaryCta(
        text = stringResource(R.string.close),
        onClick = onDismiss,
        style = SecondaryCtaStyle.Text,
        contentDescription = "Close update balance sheet",
    )
}

@Composable
private fun UpdateResultContent(
    currentFormatted: String,
    infoMessage: String?,
    errorMessage: String?,
    canContinueToCreditEntry: Boolean,
    canContinueToWithdrawal: Boolean,
    onContinueToCreditEntry: () -> Unit,
    onContinueToWithdrawal: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
        Text(
            text = stringResource(R.string.update_balance_title),
            style = PiPlannerTypography.title,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface,
        )
    }

    PiCard(contentDescription = "Current balance card") {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.goals_update_current_label),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Text(
                text = currentFormatted,
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
        }
    }

    UpdateMessages(infoMessage = infoMessage, errorMessage = errorMessage)

    when {
        canContinueToCreditEntry -> {
            PrimaryCta(
                text = stringResource(R.string.continue_label),
                onClick = onContinueToCreditEntry,
                contentDescription = "Continue to credit entry",
            )
        }
        canContinueToWithdrawal -> {
            PrimaryCta(
                text = stringResource(R.string.goals_sync_withdrawal_cta),
                onClick = onContinueToWithdrawal,
                contentDescription = "Continue to withdrawal",
            )
        }
    }
    SecondaryCta(
        text = stringResource(R.string.close),
        onClick = onDismiss,
        style = SecondaryCtaStyle.Text,
        contentDescription = "Close update balance sheet",
    )
}

@Composable
private fun UpdateMessages(
    infoMessage: String?,
    errorMessage: String?,
) {
    if (infoMessage != null) {
        Text(
            text = infoMessage,
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            modifier = Modifier.semantics { contentDescription = "Update balance info" },
        )
    }
    if (errorMessage != null) {
        Text(
            text = errorMessage,
            style = PiPlannerTypography.body,
            color = PiPlannerColors.Destructive,
        )
    }
}
