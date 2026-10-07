package com.piplanner.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheetHandle
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme

/**
 * Delete goal screen — design frame 17 (PRD R14 + delete).
 * Release amount, destination split, destructive confirm layout.
 * Visual/layout only — reassignment logic unchanged (PIP-54).
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
        onBeginEdit = viewModel::beginEditPercents,
        onFinishEdit = viewModel::finishEditPercents,
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
    onBeginEdit: () -> Unit,
    onFinishEdit: () -> Unit,
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
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PiPlannerDimens.Space24)
            .padding(top = PiPlannerDimens.Space12, bottom = PiPlannerDimens.Space28)
            .semantics {
                contentDescription = "Delete goal. Reassign saved money to remaining goals."
            },
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

        when (uiState.phase) {
            DeleteGoalPhase.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = PiPlannerColors.NavyPrimary,
                )
            }
            DeleteGoalPhase.MissingGoal -> {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.delete_goal_missing),
                    style = MaterialTheme.typography.bodyLarge,
                    color = PiPlannerColors.OnSurface,
                )
            }
            else -> {
                val deletedName = uiState.deletedGoal?.name.orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                    Text(
                        text = stringResource(R.string.delete_goal_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PiPlannerColors.OnSurface,
                    )
                    Text(
                        text = stringResource(R.string.delete_goal_subtitle, deletedName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    )
                }

                PiCard(contentDescription = "Released amount ${uiState.formattedReleasedAmount}") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.delete_goal_released_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                        )
                        Text(
                            text = uiState.formattedReleasedAmount,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PiPlannerColors.NavyPrimary,
                            modifier = Modifier.semantics {
                                contentDescription =
                                    "Released amount ${uiState.formattedReleasedAmount}"
                            },
                        )
                    }
                }

                if (uiState.isOnlyGoalGate || uiState.showCreateForm) {
                    PiCard(contentDescription = "Replacement goal form") {
                        ReplacementGoalForm(
                            draft = uiState.replacementDraft,
                            onDraftChange = onDraftChange,
                            onSave = onSaveReplacement,
                            onCancel = if (uiState.isOnlyGoalGate) null else onHideCreate,
                            gateCopy = uiState.isOnlyGoalGate,
                        )
                    }
                }

                if (uiState.destinationGoals.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.delete_goal_reassign_heading),
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
                                    contentDescription = "Edit reassignment",
                                )
                            }
                            uiState.isEditingPercents -> {
                                SecondaryCta(
                                    text = stringResource(R.string.done),
                                    onClick = onFinishEdit,
                                    style = SecondaryCtaStyle.Text,
                                    fillMaxWidth = false,
                                    contentDescription = "Done editing reassignment",
                                )
                            }
                        }
                    }
                    uiState.destinationGoals.forEach { goal ->
                        ReassignmentRow(
                            goalName = goal.name,
                            percent = uiState.displayPercents[goal.id] ?: 0,
                            amountLabel = formattedAmount(goal.id),
                            editable = uiState.canEditPercents,
                            onPercentChange = { onPercentChange(goal.id, it) },
                        )
                    }
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.canConfirm) {
                            PiPlannerColors.OnSurface.copy(alpha = 0.72f)
                        } else {
                            PiPlannerColors.Behind
                        },
                    )
                    if (uiState.hasEditedOnce) {
                        Text(
                            text = stringResource(R.string.delete_goal_edit_locked_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                        )
                    }
                    if (uiState.resetStandingToEqual) {
                        Text(
                            text = stringResource(R.string.delete_goal_standing_reset_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = PiPlannerColors.NavyPrimary,
                        )
                    }
                }

                if (!uiState.isOnlyGoalGate && !uiState.showCreateForm) {
                    SecondaryCta(
                        text = stringResource(R.string.delete_goal_add_goal),
                        onClick = onShowCreate,
                        style = SecondaryCtaStyle.Text,
                        fillMaxWidth = false,
                    )
                }

                Spacer(modifier = Modifier.height(PiPlannerDimens.Space8))

                if (uiState.isConfirming) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(PiPlannerDimens.Space20)
                                .width(PiPlannerDimens.Space20),
                            strokeWidth = PiPlannerDimens.Space8 / 4,
                            color = PiPlannerColors.Destructive,
                        )
                    }
                }

                DestructiveCta(
                    text = stringResource(R.string.delete_goal_confirm),
                    onClick = onConfirmClick,
                    enabled = uiState.canConfirm && !uiState.isConfirming,
                    contentDescription = "Confirm delete goal",
                )
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
                    Text(
                        text = stringResource(R.string.delete_goal_confirm),
                        color = PiPlannerColors.Destructive,
                        fontWeight = FontWeight.SemiBold,
                    )
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

/**
 * Filled destructive CTA for Delete confirm (frame 17).
 * Mirrors [PrimaryCta] shape/padding with [PiPlannerColors.Destructive].
 */
@Composable
private fun DestructiveCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = text,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
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
        Text(text = text, fontWeight = FontWeight.SemiBold)
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
    PiCard(contentDescription = "$goalName reassignment") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
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
                    color = PiPlannerColors.OnSurface,
                )
                Text(
                    text = amountLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PiPlannerColors.OnSurface,
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PiPlannerColors.NavyPrimary,
                        cursorColor = PiPlannerColors.NavyPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(R.string.percent_value, percent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
        Text(
            text = if (gateCopy) {
                stringResource(R.string.delete_goal_only_gate_title)
            } else {
                stringResource(R.string.delete_goal_add_goal_title)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface,
        )
        if (gateCopy) {
            Text(
                text = stringResource(R.string.delete_goal_only_gate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PiPlannerColors.NavyPrimary,
            cursorColor = PiPlannerColors.NavyPrimary,
        )
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text(stringResource(R.string.goal_name_label)) },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.targetRupeesText,
            onValueChange = {
                onDraftChange(draft.copy(targetRupeesText = it.filter { ch -> ch.isDigit() }))
            },
            label = { Text(stringResource(R.string.goal_target_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.startDate,
            onValueChange = { onDraftChange(draft.copy(startDate = it)) },
            label = { Text(stringResource(R.string.goal_start_label)) },
            supportingText = { Text(stringResource(R.string.goal_date_hint)) },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.endDate,
            onValueChange = { onDraftChange(draft.copy(endDate = it)) },
            label = { Text(stringResource(R.string.goal_end_label)) },
            supportingText = { Text(stringResource(R.string.goal_date_hint)) },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            PrimaryCta(
                text = stringResource(R.string.save),
                onClick = onSave,
                fillMaxWidth = false,
            )
            if (onCancel != null) {
                SecondaryCta(
                    text = stringResource(R.string.cancel),
                    onClick = onCancel,
                    style = SecondaryCtaStyle.Text,
                    fillMaxWidth = false,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Delete goal · release + destinations")
@Composable
private fun DeleteGoalPreview() {
    val now = "2026-01-01T00:00:00Z"
    val deleted = Goal(
        id = "g1",
        name = "Vacation",
        targetAmount = 10_000_000L,
        startDate = "2026-01-01",
        endDate = "2026-12-01",
        savedAmount = 2_000_000L,
        shareOfNewCredits = 0.4,
        createdAt = now,
        updatedAt = now,
    )
    val remaining = Goal(
        id = "g2",
        name = "Emergency",
        targetAmount = 20_000_000L,
        startDate = "2026-01-01",
        endDate = "2027-01-01",
        savedAmount = 4_000_000L,
        shareOfNewCredits = 0.6,
        createdAt = now,
        updatedAt = now,
    )
    PiPlannerTheme {
        DeleteGoalContent(
            uiState = DeleteGoalUiState(
                phase = DeleteGoalPhase.ReassignDefault,
                deletedGoal = deleted,
                destinationGoals = listOf(remaining),
                formattedReleasedAmount = "₹20,000",
                displayPercents = mapOf("g2" to 100),
                statusMessage = "Total 100% — ready to delete",
                canConfirm = true,
                resetStandingToEqual = true,
            ),
            formattedAmount = { "₹20,000" },
            onPercentChange = { _, _ -> },
            onBeginEdit = {},
            onFinishEdit = {},
            onConfirmClick = {},
            onConfirmDelete = {},
            onDismissConfirm = {},
            onDismissError = {},
            onShowCreate = {},
            onHideCreate = {},
            onDraftChange = {},
            onSaveReplacement = {},
            onBack = {},
        )
    }
}
