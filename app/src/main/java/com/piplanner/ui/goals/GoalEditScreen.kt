package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.piplanner.domain.GoalValidationService
import com.piplanner.ui.setup.InflationPopup
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Goal edit · design frame 6e (PRD R10 / R11 / R24).
 * Saved amount is locked read-only; save shows toast 9c and holds changes to next credit.
 */
@Composable
fun GoalEditScreen(
    viewModel: GoalEditViewModel,
    validation: GoalValidationService,
    onBack: () -> Unit,
    onSavedNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Post-edit toast (9c) then return to detail so later view can show held info (13g).
    LaunchedEffect(uiState.showSavedToast, uiState.shouldNavigateBack, uiState.toastMessage) {
        if (uiState.showSavedToast) {
            snackbarHostState.showSnackbar(uiState.toastMessage)
            viewModel.consumeToast()
        }
        if (uiState.shouldNavigateBack) {
            viewModel.consumeNavigation()
            onSavedNavigateBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .semantics { contentDescription = "Goal edit" },
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }

            if (uiState.missing) {
                Text(
                    text = stringResource(R.string.goal_detail_missing),
                    style = MaterialTheme.typography.bodyLarge,
                )
                return@Column
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.goal_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.goal_edit_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val draft = uiState.draft
            val canSave = uiState.canSave(validation)
            val targetPaisa = draft.targetPaisa(validation)
            val inflationPercent = draft.inflationPercentDisplay(validation)
            val sharePercent = draft.sharePercentDisplay(validation)

            OutlinedTextField(
                value = draft.name,
                onValueChange = { value -> viewModel.updateDraft { it.copy(name = value) } },
                label = { Text(stringResource(R.string.goal_name_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Goal name" },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.goal_target_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = draft.targetRupeeDigits,
                    onValueChange = { raw ->
                        viewModel.updateDraft {
                            it.copy(targetRupeeDigits = raw.filter { ch -> ch.isDigit() })
                        }
                    },
                    prefix = { Text("₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Target in rupees" },
                )
                Text(
                    text = viewModel.formatInr(targetPaisa),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = draft.startDate.toString(),
                onValueChange = { value ->
                    parseLocalDateOrNull(value)?.let { date ->
                        viewModel.updateDraft { it.copy(startDate = date) }
                    }
                },
                label = { Text(stringResource(R.string.goal_start_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.goal_date_hint)) },
            )

            OutlinedTextField(
                value = draft.endDate.toString(),
                onValueChange = { value ->
                    parseLocalDateOrNull(value)?.let { date ->
                        viewModel.updateDraft { it.copy(endDate = date) }
                    }
                },
                label = { Text(stringResource(R.string.goal_end_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.goal_date_hint)) },
            )

            TextButton(
                onClick = { viewModel.setShowInflationPopup(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Inflation $inflationPercent percent"
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.inflation_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.percent_value, inflationPercent),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.edit),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.goal_share_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.percent_value, sharePercent),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Slider(
                    value = sharePercent.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateDraft {
                            it.copy(
                                shareOfNewCredits = value.roundToInt().coerceIn(0, 100) / 100.0,
                            )
                        }
                    },
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier.semantics {
                        contentDescription = "Share of new credits"
                    },
                )
            }

            // Frame 6e — saved amount locked read-only
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.goal_saved_amount_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = uiState.formattedSavedLocked,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Saved amount locked" },
                    supportingText = {
                        Text(stringResource(R.string.goal_saved_locked_edit_hint))
                    },
                )
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { viewModel.save() },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Save changes" },
            ) {
                Text(stringResource(R.string.save_changes))
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics { contentDescription = "Post-edit toast" },
        )
    }

    if (uiState.showInflationPopup) {
        val draft = uiState.draft
        InflationPopup(
            inflationRate = draft.inflationRate,
            targetPaisa = draft.targetPaisa(validation),
            startDate = draft.startDate,
            endDate = draft.endDate,
            formattedAdjustedTarget = viewModel.formatInr(
                validation.adjustedTargetPaisa(
                    targetPaisa = draft.targetPaisa(validation),
                    inflationRate = draft.inflationRate,
                    startDate = draft.startDate,
                    endDate = draft.endDate,
                ),
            ),
            onRateChange = { rate -> viewModel.updateDraft { it.copy(inflationRate = rate) } },
            onDone = { viewModel.setShowInflationPopup(false) },
        )
    }
}

private fun parseLocalDateOrNull(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value) }.getOrNull()
}
