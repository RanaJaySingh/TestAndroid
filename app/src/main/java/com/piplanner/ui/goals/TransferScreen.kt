package com.piplanner.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.domain.TransferPhase
import com.piplanner.domain.TransferService
import com.piplanner.ui.components.LightBlueChip
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheetHandle
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme

/**
 * Transfer between goals — design frames 16 / 16b (PRD R14 visual).
 * From/To selectors, amount + light-blue chips, after-transfer preview, Move gating.
 * Visual/layout only — validation/ledger unchanged (PIP-56).
 */
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            viewModel.consumeNavigateBack()
            onBack()
        }
    }

    TransferContent(
        uiState = uiState,
        formattedSaved = viewModel::formattedSaved,
        formattedAmount = viewModel::formattedAmount,
        onFromSelected = viewModel::selectFromGoal,
        onToSelected = viewModel::selectToGoal,
        onAmountDigitsChange = viewModel::setAmountDigits,
        onChip = viewModel::applyChip,
        onMove = viewModel::move,
        onBack = onBack,
        onDismissError = viewModel::clearError,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransferContent(
    uiState: TransferUiState,
    formattedSaved: (Goal) -> String,
    formattedAmount: (Long) -> String,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onAmountDigitsChange: (String) -> Unit,
    onChip: (Long) -> Unit,
    onMove: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PiPlannerColors.BackgroundApp)
                .semantics { contentDescription = "Transfer loading" },
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
            .semantics { contentDescription = "Transfer screen" },
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
                text = stringResource(R.string.transfer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.transfer_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }

        GoalSelectorDropdown(
            label = stringResource(R.string.transfer_from_label),
            contentDescription = "From goal selector",
            goals = uiState.goals,
            selectedGoalId = uiState.fromGoalId,
            formattedSaved = formattedSaved,
            enabled = uiState.phase != TransferPhase.Complete && !uiState.isMoving,
            onSelected = onFromSelected,
        )

        GoalSelectorDropdown(
            label = stringResource(R.string.transfer_to_label),
            contentDescription = "To goal selector",
            goals = uiState.goals,
            selectedGoalId = uiState.toGoalId,
            formattedSaved = formattedSaved,
            enabled = uiState.phase != TransferPhase.Complete && !uiState.isMoving,
            onSelected = onToSelected,
        )

        OutlinedTextField(
            value = uiState.amountRupeeDigits,
            onValueChange = onAmountDigitsChange,
            label = { Text(stringResource(R.string.transfer_amount_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = uiState.phase != TransferPhase.Complete && !uiState.isMoving,
            isError = uiState.isOverAmount,
            supportingText = if (uiState.isOverAmount) {
                {
                    Text(
                        text = TransferService.OVER_AMOUNT_MESSAGE,
                        color = PiPlannerColors.Destructive,
                        modifier = Modifier.semantics {
                            contentDescription = "Over amount message"
                        },
                    )
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PiPlannerColors.NavyPrimary,
                cursorColor = PiPlannerColors.NavyPrimary,
                errorBorderColor = PiPlannerColors.Destructive,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Transfer amount field" },
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
            modifier = Modifier.semantics { contentDescription = "Amount chips" },
        ) {
            uiState.chipAmountsPaisa.forEach { chipPaisa ->
                val label = formattedAmount(chipPaisa)
                LightBlueChip(
                    label = label,
                    selected = uiState.amountPaisa == chipPaisa,
                    onClick = { onChip(chipPaisa) },
                    enabled = uiState.phase != TransferPhase.Complete && !uiState.isMoving,
                    contentDescription = "Amount chip $label",
                )
            }
        }

        if (uiState.showPreview) {
            val from = uiState.fromGoal
            val to = uiState.toGoal
            val afterFrom = uiState.afterFromSavedPaisa
            val afterTo = uiState.afterToSavedPaisa
            if (from != null && to != null && afterFrom != null && afterTo != null) {
                PiCard(contentDescription = "After transfer preview") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                    ) {
                        Text(
                            text = stringResource(R.string.transfer_after_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PiPlannerColors.OnSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.transfer_after_from,
                                from.name,
                                formattedAmount(afterFrom),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = PiPlannerColors.OnSurface,
                            modifier = Modifier.semantics {
                                contentDescription = "After from ${from.name}"
                            },
                        )
                        Text(
                            text = stringResource(
                                R.string.transfer_after_to,
                                to.name,
                                formattedAmount(afterTo),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = PiPlannerColors.OnSurface,
                            modifier = Modifier.semantics {
                                contentDescription = "After to ${to.name}"
                            },
                        )
                    }
                }
            }
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.isOverAmount) {
                PiPlannerColors.Destructive
            } else {
                PiPlannerColors.OnSurface.copy(alpha = 0.72f)
            },
            modifier = Modifier.semantics {
                contentDescription = "Transfer status ${uiState.statusMessage}"
            },
        )

        if (uiState.isMoving) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    color = PiPlannerColors.NavyPrimary,
                    strokeWidth = PiPlannerDimens.Space8 / 4,
                )
            }
        }

        PrimaryCta(
            text = stringResource(R.string.transfer_move),
            onClick = onMove,
            enabled = uiState.canMove && !uiState.isMoving,
            contentDescription = "Move transfer",
        )
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.transfer_error_title)) },
            text = { Text(uiState.errorMessage) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalSelectorDropdown(
    label: String,
    contentDescription: String,
    goals: List<Goal>,
    selectedGoalId: String?,
    formattedSaved: (Goal) -> String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = goals.firstOrNull { it.id == selectedGoalId }
    val display = selected?.let { "${it.name} · ${formattedSaved(it)}" }
        ?: stringResource(R.string.transfer_select_goal)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PiPlannerColors.NavyPrimary,
                cursorColor = PiPlannerColors.NavyPrimary,
            ),
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            goals.forEach { goal ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(goal.name)
                            Text(
                                text = formattedSaved(goal),
                                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                            )
                        }
                    },
                    onClick = {
                        onSelected(goal.id)
                        expanded = false
                    },
                    modifier = Modifier.semantics {
                        this.contentDescription = "Select goal ${goal.name}"
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Transfer · chips + preview")
@Composable
private fun TransferPreview() {
    val goals = transferPreviewGoals()
    PiPlannerTheme {
        TransferContent(
            uiState = TransferUiState(
                goals = goals,
                fromGoalId = "g1",
                toGoalId = "g2",
                amountRupeeDigits = "5000",
                amountPaisa = 500_000L,
                chipAmountsPaisa = listOf(100_000L, 500_000L, 1_000_000L),
                afterFromSavedPaisa = 3_500_000L,
                afterToSavedPaisa = 2_500_000L,
                canMove = true,
                statusMessage = "Ready to move",
                phase = TransferPhase.Preview,
            ),
            formattedSaved = { "₹${it.savedAmount / 100}" },
            formattedAmount = { paisa ->
                when (paisa) {
                    100_000L -> "₹1,000"
                    500_000L -> "₹5,000"
                    1_000_000L -> "₹10,000"
                    else -> "₹${paisa / 100}"
                }
            },
            onFromSelected = {},
            onToSelected = {},
            onAmountDigitsChange = {},
            onChip = {},
            onMove = {},
            onBack = {},
            onDismissError = {},
        )
    }
}

private fun transferPreviewGoals(): List<Goal> {
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
