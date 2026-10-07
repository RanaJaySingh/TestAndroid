package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.piplanner.R
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheet
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Sync sheet for Consent On — frames 10 / 10a / 10b (PIP-48 behaviour, PIP-84 visuals).
 * PiSheet + PiCard Previous / Fetched / New amount (+green) or Went down by (destructive).
 * Also available as [SyncSheet] for ticket naming.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsSyncSheet(
    phase: SyncSheetPhase,
    formattedPrevious: String,
    formattedFetched: String?,
    formattedNewAmount: String?,
    formattedWentDownBy: String? = null,
    infoMessage: String?,
    errorMessage: String?,
    isBlockedByOpenEntry: Boolean,
    canContinueToCreditEntry: Boolean,
    canContinueToWithdrawal: Boolean,
    onSync: () -> Unit,
    onContinueToCreditEntry: () -> Unit,
    onContinueToWithdrawal: () -> Unit,
    onDismiss: () -> Unit,
) {
    val helper = when {
        phase == SyncSheetPhase.Idle &&
            formattedFetched == null &&
            !isBlockedByOpenEntry -> stringResource(R.string.goals_sync_helper)
        else -> null
    }

    PiSheet(
        onDismissRequest = onDismiss,
        contentDescription = "Goals sync sheet",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.goals_sync_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            if (helper != null) {
                Text(
                    text = helper,
                    style = PiPlannerTypography.body,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
            }
        }

        PiCard(contentDescription = "Sync balance rows") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
                SyncAmountRow(
                    label = stringResource(R.string.credit_entry_previous),
                    value = formattedPrevious,
                    emphasis = SyncRowEmphasis.Standard,
                )
                if (formattedFetched != null) {
                    SyncAmountRow(
                        label = stringResource(R.string.goals_sync_fetched),
                        value = formattedFetched,
                        emphasis = SyncRowEmphasis.Standard,
                    )
                }
                if (formattedNewAmount != null) {
                    // Design frame 10: New amount +₹… in positive green (prefix is visual only).
                    SyncAmountRow(
                        label = stringResource(R.string.credit_entry_new_amount),
                        value = "+$formattedNewAmount",
                        emphasis = SyncRowEmphasis.Positive,
                    )
                } else if (formattedWentDownBy != null) {
                    // Design frame 10b: Went down by −₹… (presentation only).
                    SyncAmountRow(
                        label = stringResource(R.string.goals_sync_went_down_by),
                        value = formattedWentDownBy,
                        emphasis = SyncRowEmphasis.Negative,
                    )
                }
            }
        }

        if (infoMessage != null) {
            Text(
                text = infoMessage,
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                textAlign = TextAlign.Start,
                modifier = Modifier.semantics { contentDescription = "Sync info" },
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = PiPlannerTypography.body,
                color = PiPlannerColors.Destructive,
                textAlign = TextAlign.Start,
            )
        }

        when {
            phase == SyncSheetPhase.Syncing -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
                ) {
                    CircularProgressIndicator(color = PiPlannerColors.NavyPrimary)
                    Text(
                        text = stringResource(R.string.goals_syncing),
                        style = PiPlannerTypography.body,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    )
                }
            }
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
            else -> {
                PrimaryCta(
                    text = stringResource(R.string.goals_sync_now),
                    onClick = onSync,
                    enabled = !isBlockedByOpenEntry && phase != SyncSheetPhase.Syncing,
                    contentDescription = "Sync now",
                )
            }
        }
        SecondaryCta(
            text = stringResource(R.string.close),
            onClick = onDismiss,
            style = SecondaryCtaStyle.Text,
            contentDescription = "Close sync sheet",
        )
    }
}

/** Ticket-facing alias for [GoalsSyncSheet]. */
@Composable
fun SyncSheet(
    phase: SyncSheetPhase,
    formattedPrevious: String,
    formattedFetched: String?,
    formattedNewAmount: String?,
    formattedWentDownBy: String? = null,
    infoMessage: String?,
    errorMessage: String?,
    isBlockedByOpenEntry: Boolean,
    canContinueToCreditEntry: Boolean,
    canContinueToWithdrawal: Boolean,
    onSync: () -> Unit,
    onContinueToCreditEntry: () -> Unit,
    onContinueToWithdrawal: () -> Unit,
    onDismiss: () -> Unit,
) {
    GoalsSyncSheet(
        phase = phase,
        formattedPrevious = formattedPrevious,
        formattedFetched = formattedFetched,
        formattedNewAmount = formattedNewAmount,
        formattedWentDownBy = formattedWentDownBy,
        infoMessage = infoMessage,
        errorMessage = errorMessage,
        isBlockedByOpenEntry = isBlockedByOpenEntry,
        canContinueToCreditEntry = canContinueToCreditEntry,
        canContinueToWithdrawal = canContinueToWithdrawal,
        onSync = onSync,
        onContinueToCreditEntry = onContinueToCreditEntry,
        onContinueToWithdrawal = onContinueToWithdrawal,
        onDismiss = onDismiss,
    )
}

enum class SyncSheetPhase {
    Idle,
    Syncing,
    ShowingResult,
}

private enum class SyncRowEmphasis {
    Standard,
    Positive,
    Negative,
}

@Composable
private fun SyncAmountRow(
    label: String,
    value: String,
    emphasis: SyncRowEmphasis,
) {
    val valueColor = when (emphasis) {
        SyncRowEmphasis.Standard -> PiPlannerColors.OnSurface
        SyncRowEmphasis.Positive -> PiPlannerColors.PositiveGreen
        SyncRowEmphasis.Negative -> PiPlannerColors.Destructive
    }
    val valueStyle = when (emphasis) {
        SyncRowEmphasis.Positive -> PiPlannerTypography.title
        SyncRowEmphasis.Standard, SyncRowEmphasis.Negative -> PiPlannerTypography.body
    }
    val valueWeight = when (emphasis) {
        SyncRowEmphasis.Standard -> FontWeight.SemiBold
        SyncRowEmphasis.Positive, SyncRowEmphasis.Negative -> FontWeight.Bold
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
        )
        Text(
            text = value,
            style = valueStyle,
            fontWeight = valueWeight,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}
