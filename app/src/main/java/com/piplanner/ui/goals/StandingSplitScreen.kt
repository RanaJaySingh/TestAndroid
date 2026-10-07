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
import com.piplanner.domain.StandingSplitService

/**
 * Standing split screen — design frame 15 (PRD R12).
 * Multi-goal % editor for the next credit; one-goal path skips this UI.
 */
@Composable
fun StandingSplitScreen(
    viewModel: StandingSplitViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.shouldNavigateBack, uiState.shouldSkip) {
        if (uiState.shouldNavigateBack) {
            viewModel.consumeNavigation()
            onBack()
        }
    }

    StandingSplitContent(
        uiState = uiState,
        onPercentChange = viewModel::setDisplayPercent,
        onSave = viewModel::save,
        onBack = onBack,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun StandingSplitContent(
    uiState: StandingSplitUiState,
    onPercentChange: (String, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    // One-goal skip: minimal shell while auto-apply navigates away.
    if (uiState.shouldSkip) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .semantics { contentDescription = "Standing split skipped" },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = uiState.statusMessage.ifBlank {
                    stringResource(R.string.standing_split_skip_body)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .semantics { contentDescription = "Standing split" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.standing_split_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.standing_split_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = uiState.caption.ifBlank { StandingSplitService.SAVED_MONEY_STAYS_PUT },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics {
                    contentDescription = "Saved money stays put"
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            uiState.goals.forEach { goal ->
                StandingSplitGoalRow(
                    goal = goal,
                    percent = uiState.displayPercents[goal.id] ?: 0,
                    onPercentChange = { onPercentChange(goal.id, it) },
                )
            }
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
                contentDescription = "Standing split status ${uiState.statusMessage}"
            },
        )

        Button(
            onClick = onSave,
            enabled = uiState.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Save standing split" },
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
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.standing_split_save_failed_title)) },
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
private fun StandingSplitGoalRow(
    goal: Goal,
    percent: Int,
    onPercentChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = goal.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
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
