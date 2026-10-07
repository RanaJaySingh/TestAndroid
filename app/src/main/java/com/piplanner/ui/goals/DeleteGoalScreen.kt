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

/**
 * Delete goal screen — design frames 17, 17a–17e (PRD R13, Spec BR-9).
 */
@Composable
fun DeleteGoalScreen(
    viewModel: DeleteGoalViewModel,
    goalId: String,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(goalId) {
        viewModel.configure(goalId)
    }

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.consumeNavigation()
            onNavigateBack()
        }
    }

    DeleteGoalContent(
        uiState = uiState,
        formattedAmount = viewModel::formattedAmount,
        onPercentChange = viewModel::setDisplayPercent,
        onConfirmClick = viewModel::requestConfirm,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissConfirm = viewModel::dismissConfirm,
        onDismissError = viewModel::clearError,
        onShowCreate = viewModel::showCreateReplacement,
        onHideCreate = viewModel::hideCreateReplacement,
        onDraftChange = viewModel::updateReplacementDraft,
        onSaveReplacement = viewModel::saveReplacementGoal,
        onBack = onNavigateBack,
    )
}

@Composable
fun DeleteGoalContent(
    uiState: DeleteGoalUiState,
    formattedAmount: (String) -> String,
    onPercentChange: (String, Int) -> Unit,
    onConfirmClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissConfirm: () -> Unit,
    onDismissError: () -> Unit,
    onShowCreate: () -> Unit,
    onHideCreate: () -> Unit,
    onDraftChange: (ReplacementGoalDraft) -> Unit,
    onSaveReplacement: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .semantics {
                contentDescription = "Delete goal. Reassign saved money to remaining goals."
            },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text(stringResource(R.string.back))
        }

        when (uiState.phase) {
            DeleteGoalPhase.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            DeleteGoalPhase.MissingGoal -> {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.delete_goal_missing),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> {
                val deletedName = uiState.deletedGoal?.name.orEmpty()
                Text(
                    text = stringResource(R.string.delete_goal_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.delete_goal_subtitle, deletedName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.delete_goal_released_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = uiState.formattedReleasedAmount,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics {
                            contentDescription = "Released amount ${uiState.formattedReleasedAmount}"
                        },
                    )
                }

                if (uiState.isOnlyGoalGate || uiState.showCreateForm) {
                    ReplacementGoalForm(
                        draft = uiState.replacementDraft,
                        onDraftChange = onDraftChange,
                        onSave = onSaveReplacement,
                        onCancel = if (uiState.isOnlyGoalGate) null else onHideCreate,
                        gateCopy = uiState.isOnlyGoalGate,
                    )
                }

                if (uiState.destinationGoals.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.delete_goal_reassign_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    uiState.destinationGoals.forEach { goal ->
                        ReassignmentRow(
                            goalName = goal.name,
                            percent = uiState.displayPercents[goal.id] ?: 0,
                            amountLabel = formattedAmount(goal.id),
                            editable = uiState.canEditPercents && uiState.destinationGoals.size > 1,
                            onPercentChange = { onPercentChange(goal.id, it) },
                        )
                    }
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (uiState.resetStandingToEqual) {
                        Text(
                            text = stringResource(R.string.delete_goal_standing_reset_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (!uiState.isOnlyGoalGate && !uiState.showCreateForm) {
                    TextButton(onClick = onShowCreate) {
                        Text(stringResource(R.string.delete_goal_add_goal))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onConfirmClick,
                    enabled = uiState.canConfirm && !uiState.isConfirming,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Confirm delete goal" },
                ) {
                    if (uiState.isConfirming) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .width(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.delete_goal_confirm))
                    }
                }
            }
        }
    }

    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { Text(stringResource(R.string.delete_goal_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_goal_confirm_body,
                        uiState.deletedGoal?.name.orEmpty(),
                        uiState.formattedReleasedAmount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text(stringResource(R.string.delete_goal_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirm) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    uiState.errorMessage?.let { message ->
        if (uiState.phase != DeleteGoalPhase.MissingGoal) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(stringResource(R.string.delete_goal_failed_title)) },
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

@Composable
private fun ReassignmentRow(
    goalName: String,
    percent: Int,
    amountLabel: String,
    editable: Boolean,
    onPercentChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = goalName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = amountLabel,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (editable) {
            OutlinedTextField(
                value = percent.toString(),
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }
                    onPercentChange(digits.toIntOrNull() ?: 0)
                },
                label = { Text(stringResource(R.string.share_label)) },
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = stringResource(R.string.percent_value, percent),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ReplacementGoalForm(
    draft: ReplacementGoalDraft,
    onDraftChange: (ReplacementGoalDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: (() -> Unit)?,
    gateCopy: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (gateCopy) {
                stringResource(R.string.delete_goal_only_gate_title)
            } else {
                stringResource(R.string.delete_goal_add_goal_title)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (gateCopy) {
            Text(
                text = stringResource(R.string.delete_goal_only_gate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text(stringResource(R.string.goal_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.targetRupeesText,
            onValueChange = { onDraftChange(draft.copy(targetRupeesText = it.filter { ch -> ch.isDigit() })) },
            label = { Text(stringResource(R.string.goal_target_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.startDate,
            onValueChange = { onDraftChange(draft.copy(startDate = it)) },
            label = { Text(stringResource(R.string.goal_start_label)) },
            supportingText = { Text(stringResource(R.string.goal_date_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.endDate,
            onValueChange = { onDraftChange(draft.copy(endDate = it)) },
            label = { Text(stringResource(R.string.goal_end_label)) },
            supportingText = { Text(stringResource(R.string.goal_date_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) {
                Text(stringResource(R.string.save))
            }
            if (onCancel != null) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
