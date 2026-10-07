package com.piplanner.ui.goals

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.domain.WithdrawalService
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheetHandle
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme

/**
 * Withdrawal screen — frames 18 / 18a (PRD R14 visual).
 * Goal reductions use design negative-amount treatment (−₹); Save and lock uses PrimaryCta.
 * Visual/layout only — allocation logic unchanged (PIP-58).
 */
@Composable
fun WithdrawalScreen(
    viewModel: WithdrawalViewModel,
    onDone: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            viewModel.consumeNavigateBack()
            onDone()
        }
    }

    WithdrawalContent(
        uiState = uiState,
        formattedSaved = viewModel::formattedSaved,
        formattedReduction = viewModel::formattedReduction,
        formattedRemaining = viewModel::formattedRemaining,
        onBeginEdit = viewModel::beginEdit,
        onFinishEdit = viewModel::finishEdit,
        onReductionChange = viewModel::setReductionRupees,
        onSave = viewModel::saveAndLock,
        onBack = onDone,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun WithdrawalContent(
    uiState: WithdrawalUiState,
    formattedSaved: (Goal) -> String,
    formattedReduction: (String) -> String,
    formattedRemaining: (Goal) -> String,
    onBeginEdit: () -> Unit,
    onFinishEdit: () -> Unit,
    onReductionChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PiPlannerColors.BackgroundApp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = PiPlannerColors.NavyPrimary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PiPlannerDimens.Space24)
            .padding(top = PiPlannerDimens.Space12, bottom = PiPlannerDimens.Space28)
            .semantics { contentDescription = "Withdrawal screen" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
    ) {
        PiSheetHandle()

        SecondaryCta(
            text = stringResource(R.string.back),
            onClick = onBack,
            style = SecondaryCtaStyle.Text,
            fillMaxWidth = false,
            contentDescription = "Back",
        )

        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.withdrawal_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.withdrawal_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Text(
                text = uiState.caption.ifBlank { WithdrawalService.PROPORTIONAL_CAPTION },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = PiPlannerColors.NavyPrimary,
                modifier = Modifier.semantics {
                    contentDescription = "Withdrawal caption"
                },
            )
        }

        PiCard(contentDescription = "Withdrawal balance summary") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                LabeledAmountRow(
                    label = stringResource(R.string.withdrawal_previous),
                    value = uiState.formattedPrevious,
                )
                LabeledAmountRow(
                    label = stringResource(R.string.withdrawal_balance_now),
                    value = uiState.formattedNewBalance,
                )
                LabeledAmountRow(
                    label = stringResource(R.string.withdrawal_shortfall),
                    value = negativeAmountLabel(uiState.formattedShortfall),
                    valueColor = PiPlannerColors.Destructive,
                )
                LabeledAmountRow(
                    label = stringResource(R.string.withdrawal_running_total),
                    value = negativeAmountLabel(uiState.formattedTotalReductions),
                    valueColor = PiPlannerColors.Destructive,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.withdrawal_reductions_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            when {
                uiState.canStartEdit -> {
                    SecondaryCta(
                        text = stringResource(R.string.edit),
                        onClick = onBeginEdit,
                        style = SecondaryCtaStyle.Text,
                        fillMaxWidth = false,
                        contentDescription = "Edit withdrawal reductions",
                    )
                }
                uiState.isEditing -> {
                    SecondaryCta(
                        text = stringResource(R.string.done),
                        onClick = onFinishEdit,
                        style = SecondaryCtaStyle.Text,
                        fillMaxWidth = false,
                        contentDescription = "Done editing withdrawal",
                    )
                }
            }
        }

        uiState.goals.forEach { goal ->
            WithdrawalGoalRow(
                goal = goal,
                savedLabel = formattedSaved(goal),
                reductionLabel = formattedReduction(goal.id),
                remainingLabel = formattedRemaining(goal),
                rupeeDigits = uiState.reductionRupeeDigits[goal.id].orEmpty(),
                editable = uiState.canEditAmounts,
                onReductionChange = { onReductionChange(goal.id, it) },
            )
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.canSave) {
                PiPlannerColors.OnSurface.copy(alpha = 0.72f)
            } else {
                PiPlannerColors.Behind
            },
            modifier = Modifier.semantics {
                contentDescription = "Withdrawal status ${uiState.statusMessage}"
            },
        )

        if (uiState.hasEditedOnce && !uiState.isLocked) {
            Text(
                text = stringResource(R.string.withdrawal_edit_locked_note),
                style = MaterialTheme.typography.bodySmall,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }

        if (uiState.isSaving) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(PiPlannerDimens.Space20)
                        .width(PiPlannerDimens.Space20),
                    strokeWidth = PiPlannerDimens.Space8 / 4,
                    color = PiPlannerColors.NavyPrimary,
                )
            }
        }

        if (!uiState.isLocked) {
            PrimaryCta(
                text = stringResource(R.string.withdrawal_save_lock),
                onClick = onSave,
                enabled = uiState.canSave && !uiState.isSaving,
                contentDescription = "Save and lock withdrawal",
            )
        } else {
            PrimaryCta(
                text = stringResource(R.string.done),
                onClick = onBack,
                contentDescription = "Withdrawal done",
            )
        }

        Spacer(modifier = Modifier.height(PiPlannerDimens.Space8))
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.withdrawal_save_failed)) },
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
private fun LabeledAmountRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = PiPlannerColors.OnSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

@Composable
private fun WithdrawalGoalRow(
    goal: Goal,
    savedLabel: String,
    reductionLabel: String,
    remainingLabel: String,
    rupeeDigits: String,
    editable: Boolean,
    onReductionChange: (String) -> Unit,
) {
    PiCard(
        contentDescription = "Withdrawal reduction ${goal.name}",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.OnSurface,
                )
                Text(
                    text = savedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.withdrawal_reduction_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                if (editable) {
                    OutlinedTextField(
                        value = rupeeDigits,
                        onValueChange = onReductionChange,
                        modifier = Modifier
                            .width(PiPlannerDimens.Space28 * 4)
                            .semantics { contentDescription = "${goal.name} reduction rupees" },
                        singleLine = true,
                        prefix = { Text("₹", color = PiPlannerColors.Destructive) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PiPlannerColors.NavyPrimary,
                            cursorColor = PiPlannerColors.NavyPrimary,
                        ),
                    )
                } else {
                    Text(
                        text = negativeAmountLabel(reductionLabel),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PiPlannerColors.Destructive,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.withdrawal_remaining_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Text(
                    text = remainingLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = PiPlannerColors.OnSurface,
                )
            }
        }
    }
}

/** Design negative treatment: unicode minus + INR (e.g. −₹8,000). */
internal fun negativeAmountLabel(formattedInr: String): String {
    val stripped = formattedInr.removePrefix("-").removePrefix("−")
    return "−$stripped"
}

@Preview(showBackground = true, name = "Withdrawal · negative amounts")
@Composable
private fun WithdrawalPreview() {
    val goals = withdrawalPreviewGoals()
    PiPlannerTheme {
        WithdrawalContent(
            uiState = WithdrawalUiState(
                goals = goals,
                reductionsPaisa = mapOf("g1" to 900_000L, "g2" to 600_000L),
                formattedPrevious = "₹1,00,000",
                formattedNewBalance = "₹85,000",
                formattedShortfall = "₹15,000",
                formattedTotalReductions = "₹15,000",
                statusMessage = WithdrawalService.READY_MESSAGE,
                canSave = true,
                caption = WithdrawalService.PROPORTIONAL_CAPTION,
            ),
            formattedSaved = { "₹${it.savedAmount / 100}" },
            formattedReduction = { id ->
                when (id) {
                    "g1" -> "₹9,000"
                    else -> "₹6,000"
                }
            },
            formattedRemaining = { goal ->
                when (goal.id) {
                    "g1" -> "₹31,000"
                    else -> "₹14,000"
                }
            },
            onBeginEdit = {},
            onFinishEdit = {},
            onReductionChange = { _, _ -> },
            onSave = {},
            onBack = {},
            onDismissError = {},
        )
    }
}

private fun withdrawalPreviewGoals(): List<Goal> {
    val now = "2026-01-01T00:00:00Z"
    return listOf(
        Goal(
            id = "g1",
            name = "Emergency",
            targetAmount = 20_000_000L,
            startDate = "2026-01-01",
            endDate = "2027-01-01",
            savedAmount = 4_000_000L,
            shareOfNewCredits = 0.6,
            createdAt = now,
            updatedAt = now,
        ),
        Goal(
            id = "g2",
            name = "Vacation",
            targetAmount = 10_000_000L,
            startDate = "2026-01-01",
            endDate = "2026-12-01",
            savedAmount = 2_000_000L,
            shareOfNewCredits = 0.4,
            createdAt = now,
            updatedAt = now,
        ),
    )
}
