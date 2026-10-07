package com.piplanner.ui.setup

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.piplanner.R
import com.piplanner.domain.GoalValidationService
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Goal form · create / edit — design frame 6 (PRD R5).
 */
@Composable
fun GoalFormScreen(
    draft: GoalFormDraft,
    validation: GoalValidationService,
    formatInr: (Long) -> String,
    onDraftChange: (GoalFormDraft) -> Unit,
    onOpenInflation: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    showInflationPopup: Boolean,
    onDismissInflation: () -> Unit,
) {
    val canSave = draft.canSave(validation)
    val targetPaisa = draft.targetPaisa(validation)
    val inflationPercent = draft.inflationPercentDisplay(validation)
    val sharePercent = draft.sharePercentDisplay(validation)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.goal_form_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.goal_form_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text(stringResource(R.string.goal_name_label)) },
            placeholder = { Text(stringResource(R.string.goal_name_placeholder)) },
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
                    onDraftChange(draft.copy(targetRupeeDigits = raw.filter { it.isDigit() }))
                },
                prefix = { Text("₹") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Target in rupees" },
            )
            Text(
                text = formatInr(targetPaisa),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = draft.startDate.toString(),
            onValueChange = { value ->
                parseLocalDateOrNull(value)?.let { onDraftChange(draft.copy(startDate = it)) }
            },
            label = { Text(stringResource(R.string.goal_start_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text(stringResource(R.string.goal_date_hint)) },
        )

        OutlinedTextField(
            value = draft.endDate.toString(),
            onValueChange = { value ->
                parseLocalDateOrNull(value)?.let { onDraftChange(draft.copy(endDate = it)) }
            },
            label = { Text(stringResource(R.string.goal_end_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text(stringResource(R.string.goal_date_hint)) },
        )

        TextButton(
            onClick = onOpenInflation,
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
                onValueChange = {
                    onDraftChange(
                        draft.copy(shareOfNewCredits = it.roundToInt().coerceIn(0, 100) / 100.0),
                    )
                },
                valueRange = 0f..100f,
                steps = 99,
                modifier = Modifier.semantics {
                    contentDescription = "Share of new credits"
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.goal_saved_so_far),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatInr(draft.savedAmount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.goal_saved_locked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricRow(
                title = stringResource(R.string.inflation_adjusted_target),
                value = formatInr(draft.adjustedTargetPaisa(validation)),
            )
            MetricRow(
                title = stringResource(R.string.goal_monthly_need),
                value = formatInr(draft.monthlyNeedPaisa(validation)),
            )
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Save goal" },
        ) {
            Text(stringResource(R.string.save))
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.cancel))
        }
    }

    if (showInflationPopup) {
        InflationPopup(
            inflationRate = draft.inflationRate,
            targetPaisa = targetPaisa,
            startDate = draft.startDate,
            endDate = draft.endDate,
            formattedAdjustedTarget = formatInr(
                liveAdjustedTargetPaisa(
                    validation = validation,
                    targetPaisa = targetPaisa,
                    inflationRate = draft.inflationRate,
                    startDate = draft.startDate,
                    endDate = draft.endDate,
                ),
            ),
            onRateChange = { rate -> onDraftChange(draft.copy(inflationRate = rate)) },
            onDone = onDismissInflation,
        )
    }
}

@Composable
private fun MetricRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
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

private fun parseLocalDateOrNull(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value) }.getOrNull()
}
