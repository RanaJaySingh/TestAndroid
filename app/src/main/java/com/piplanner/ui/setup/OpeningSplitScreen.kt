package com.piplanner.ui.setup

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
import com.piplanner.data.model.HistoryEntry
import com.piplanner.domain.OpeningSplitService

/**
 * Opening split screen — design frames 8 (multi-goal) and 8b (single-goal).
 */
@Composable
fun OpeningSplitScreen(
    viewModel: OpeningSplitViewModel,
    goals: List<Goal>,
    openingBalance: Long,
    lockedEntry: HistoryEntry? = null,
    onNavigateToGoals: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(goals, openingBalance, lockedEntry?.id) {
        if (lockedEntry != null) {
            viewModel.configureReadOnly(lockedEntry, goals)
        } else {
            viewModel.configure(goals = goals, openingBalance = openingBalance)
        }
    }

    LaunchedEffect(uiState.shouldNavigateToGoals) {
        if (uiState.shouldNavigateToGoals) {
            viewModel.consumeNavigation()
            onNavigateToGoals()
        }
    }

    OpeningSplitContent(
        uiState = uiState,
        formattedAmount = viewModel::formattedAmount,
        onPercentChange = viewModel::setDisplayPercent,
        onLockClick = viewModel::requestLock,
        onConfirmLock = viewModel::confirmLock,
        onDismissConfirm = viewModel::dismissConfirmLock,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun OpeningSplitContent(
    uiState: OpeningSplitUiState,
    formattedAmount: (String) -> String,
    onPercentChange: (String, Int) -> Unit,
    onLockClick: () -> Unit,
    onConfirmLock: () -> Unit,
    onDismissConfirm: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (uiState.isReadOnly) {
                    stringResource(R.string.opening_split_title_readonly)
                } else {
                    stringResource(R.string.opening_split_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (uiState.isReadOnly) {
                    OpeningSplitService.LOCKED_AMOUNTS_CAPTION
                } else {
                    stringResource(R.string.opening_split_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.opening_balance_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = uiState.formattedOpeningBalance,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Opening balance ${uiState.formattedOpeningBalance}"
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            uiState.goals.forEach { goal ->
                GoalSplitRow(
                    goal = goal,
                    amountLabel = formattedAmount(goal.id),
                    percent = uiState.displayPercents[goal.id] ?: 0,
                    isSingleGoal = uiState.isSingleGoal,
                    isReadOnly = uiState.isReadOnly,
                    onPercentChange = { onPercentChange(goal.id, it) },
                )
            }
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.canLock || uiState.isReadOnly) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        )

        if (!uiState.isReadOnly) {
            Button(
                onClick = onLockClick,
                enabled = uiState.canLock,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Lock this split"
                    },
            ) {
                if (uiState.isLocking) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.lock_this_split),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (uiState.showConfirmLock) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { Text(stringResource(R.string.lock_this_split_confirm_title)) },
            text = { Text(stringResource(R.string.lock_this_split_confirm_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmLock) {
                    Text(stringResource(R.string.lock_this_split))
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
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.lock_failed_title)) },
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
private fun GoalSplitRow(
    goal: Goal,
    amountLabel: String,
    percent: Int,
    isSingleGoal: Boolean,
    isReadOnly: Boolean,
    onPercentChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = amountLabel,
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
            isReadOnly -> {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.semantics {
                        contentDescription = "${goal.name} $percent percent, locked"
                    },
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
