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
import androidx.compose.material3.Checkbox
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
import com.piplanner.domain.CreditEntryService
import com.piplanner.domain.OpeningSplitService

/**
 * Open / locked New credit History entry (frames 13 / 13a–13g / 13t).
 */
@Composable
fun CreditEntryScreen(
    viewModel: CreditEntryViewModel,
    onDone: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            viewModel.consumeNavigateBack()
            onDone()
        }
    }

    CreditEntryContent(
        uiState = uiState,
        formattedSavedSoFar = viewModel::formattedSavedSoFar,
        formattedAmount = viewModel::formattedAmount,
        onPercentChange = viewModel::setDisplayPercent,
        onUseThisSplitChange = viewModel::setUseThisSplitForStanding,
        onSave = viewModel::saveAndLock,
        onDone = onDone,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun CreditEntryContent(
    uiState: CreditEntryUiState,
    formattedSavedSoFar: (Goal) -> String,
    formattedAmount: (String) -> String,
    onPercentChange: (String, Int) -> Unit,
    onUseThisSplitChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
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
            .semantics { contentDescription = "Credit entry screen" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (uiState.isLocked) {
                        stringResource(R.string.credit_entry_title_locked)
                    } else {
                        stringResource(R.string.credit_entry_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (uiState.isTyped) {
                    Text(
                        text = stringResource(R.string.credit_entry_typed_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "Typed credit badge"
                        },
                    )
                }
            }
            Text(
                text = if (uiState.isLocked) {
                    OpeningSplitService.LOCKED_AMOUNTS_CAPTION
                } else {
                    CreditEntryService.LOCKED_ONCE_CAPTION
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!uiState.isTyped && uiState.formattedPrevious != null) {
                LabeledAmountRow(
                    label = stringResource(R.string.credit_entry_previous),
                    value = uiState.formattedPrevious,
                )
            }
            if (!uiState.isTyped && uiState.formattedNewBalance != null) {
                LabeledAmountRow(
                    label = stringResource(R.string.credit_entry_balance_now),
                    value = uiState.formattedNewBalance,
                )
            }
            LabeledAmountRow(
                label = stringResource(R.string.credit_entry_new_amount),
                value = uiState.formattedCreditAmount,
            )
        }

        Text(
            text = stringResource(R.string.credit_entry_already_saved),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        uiState.goals.forEach { goal ->
            CreditGoalSplitRow(
                goal = goal,
                savedSoFar = formattedSavedSoFar(goal),
                thisCreditAmount = formattedAmount(goal.id),
                percent = uiState.displayPercents[goal.id] ?: 0,
                isSingleGoal = uiState.isSingleGoal,
                isLocked = uiState.isLocked,
                onPercentChange = { onPercentChange(goal.id, it) },
            )
        }

        if (!uiState.isLocked && !uiState.isSingleGoal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Use this split checkbox" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = uiState.useThisSplitForStanding,
                    onCheckedChange = onUseThisSplitChange,
                )
                Text(
                    text = CreditEntryService.USE_THIS_SPLIT_CHECKBOX_TITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = "Credit entry status" },
        )

        if (!uiState.isLocked) {
            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Save and lock" },
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
                        text = stringResource(R.string.credit_entry_save_lock),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Credit entry done" },
            ) {
                Text(stringResource(R.string.done))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.credit_entry_save_failed)) },
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
private fun CreditGoalSplitRow(
    goal: Goal,
    savedSoFar: String,
    thisCreditAmount: String,
    percent: Int,
    isSingleGoal: Boolean,
    isLocked: Boolean,
    onPercentChange: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = "Credit split ${goal.name}" },
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
                text = savedSoFar,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.credit_entry_this_credit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = thisCreditAmount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        when {
            isSingleGoal -> {
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.semantics {
                        contentDescription = "${goal.name} automatically assigned 100 percent"
                    },
                )
            }
            isLocked -> {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.share_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = percent.toString(),
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }
                                onPercentChange(digits.toIntOrNull() ?: 0)
                            },
                            modifier = Modifier
                                .width(88.dp)
                                .semantics {
                                    contentDescription = "${goal.name} percentage"
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
