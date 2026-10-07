package com.piplanner.ui.goals

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.domain.TransferPhase
import com.piplanner.domain.TransferService

/**
 * Transfer between goals — design frames 16 / 16a / 16b / 16c (PRD R14).
 * From/To selectors, amount + chips, after-transfer preview, Move gating.
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
                .semantics { contentDescription = "Transfer loading" },
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
            .semantics { contentDescription = "Transfer screen" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.transfer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.transfer_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        modifier = Modifier.semantics {
                            contentDescription = "Over amount message"
                        },
                    )
                }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Transfer amount field" },
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics { contentDescription = "Amount chips" },
        ) {
            uiState.chipAmountsPaisa.forEach { chipPaisa ->
                val label = formattedAmount(chipPaisa)
                FilterChip(
                    selected = uiState.amountPaisa == chipPaisa,
                    onClick = { onChip(chipPaisa) },
                    enabled = uiState.phase != TransferPhase.Complete && !uiState.isMoving,
                    label = { Text(label) },
                    modifier = Modifier.semantics {
                        contentDescription = "Amount chip $label"
                    },
                )
            }
        }

        if (uiState.showPreview) {
            val from = uiState.fromGoal
            val to = uiState.toGoal
            val afterFrom = uiState.afterFromSavedPaisa
            val afterTo = uiState.afterToSavedPaisa
            if (from != null && to != null && afterFrom != null && afterTo != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.semantics {
                        contentDescription = "After transfer preview"
                    },
                ) {
                    Text(
                        text = stringResource(R.string.transfer_after_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.transfer_after_from,
                            from.name,
                            formattedAmount(afterFrom),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
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
                        modifier = Modifier.semantics {
                            contentDescription = "After to ${to.name}"
                        },
                    )
                }
            }
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.isOverAmount) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.semantics {
                contentDescription = "Transfer status ${uiState.statusMessage}"
            },
        )

        Button(
            onClick = onMove,
            enabled = uiState.canMove && !uiState.isMoving,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Move transfer" },
        ) {
            if (uiState.isMoving) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(stringResource(R.string.transfer_move))
        }
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
            modifier = Modifier
                .menuAnchor()
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
