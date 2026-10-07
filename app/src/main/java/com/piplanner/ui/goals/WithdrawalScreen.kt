package com.piplanner.ui.goals

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.domain.WithdrawalService

/**
 * Withdrawal screen — frames 18 / 18a / 18b (PRD R15, Spec BR-8).
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
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .semantics { contentDescription = "Withdrawal screen" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.withdrawal_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.withdrawal_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = uiState.caption.ifBlank { WithdrawalService.PROPORTIONAL_CAPTION },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics {
                    contentDescription = "Withdrawal caption"
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                value = uiState.formattedShortfall,
            )
            LabeledAmountRow(
                label = stringResource(R.string.withdrawal_running_total),
                value = uiState.formattedTotalReductions,
            )
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
            )
            when {
                uiState.canStartEdit -> {
                    TextButton(
                        onClick = onBeginEdit,
                        modifier = Modifier.semantics {
                            contentDescription = "Edit withdrawal reductions"
                        },
                    ) {
                        Text(stringResource(R.string.edit))
                    }
                }
                uiState.isEditing -> {
                    TextButton(
                        onClick = onFinishEdit,
                        modifier = Modifier.semantics {
                            contentDescription = "Done editing withdrawal"
                        },
                    ) {
                        Text(stringResource(R.string.done))
                    }
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
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.semantics {
                contentDescription = "Withdrawal status ${uiState.statusMessage}"
            },
        )

        if (uiState.hasEditedOnce && !uiState.isLocked) {
            Text(
                text = stringResource(R.string.withdrawal_edit_locked_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!uiState.isLocked) {
            Button(
                onClick = onSave,
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Save and lock withdrawal" },
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.withdrawal_save_lock),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Withdrawal done" },
            ) {
                Text(stringResource(R.string.done))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
private fun LabeledAmountRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
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
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = "Withdrawal reduction ${goal.name}" },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = savedLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.withdrawal_reduction_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (editable) {
                OutlinedTextField(
                    value = rupeeDigits,
                    onValueChange = onReductionChange,
                    modifier = Modifier
                        .width(120.dp)
                        .semantics { contentDescription = "${goal.name} reduction rupees" },
                    singleLine = true,
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            } else {
                Text(
                    text = reductionLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = remainingLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
