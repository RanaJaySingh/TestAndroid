package com.piplanner.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.piplanner.R
import com.piplanner.domain.GoalValidationService
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Inflation rate popup — design frame 7. Default 7% with live adjusted target.
 */
@Composable
fun InflationPopup(
    inflationRate: Double,
    targetPaisa: Long,
    startDate: LocalDate,
    endDate: LocalDate,
    formattedAdjustedTarget: String,
    onRateChange: (Double) -> Unit,
    onDone: () -> Unit,
) {
    val percent = (inflationRate * 100.0).roundToInt().coerceIn(0, 30)

    Dialog(onDismissRequest = onDone) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.inflation_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.inflation_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.inflation_rate_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.percent_value, percent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics {
                            contentDescription = "Inflation $percent percent"
                        },
                    )
                }
                Slider(
                    value = percent.toFloat(),
                    onValueChange = { onRateChange(it.roundToInt().coerceIn(0, 30) / 100.0) },
                    valueRange = 0f..30f,
                    steps = 29,
                    modifier = Modifier.semantics {
                        contentDescription = "Inflation rate"
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.inflation_adjusted_target),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formattedAdjustedTarget,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = "Adjusted target $formattedAdjustedTarget"
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Done" },
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }
}

/** Helper used by screens / tests for live adjusted target while the popup is open. */
fun liveAdjustedTargetPaisa(
    validation: GoalValidationService,
    targetPaisa: Long,
    inflationRate: Double,
    startDate: LocalDate,
    endDate: LocalDate,
): Long {
    return validation.adjustedTargetPaisa(
        targetPaisa = targetPaisa,
        inflationRate = inflationRate,
        startDate = startDate,
        endDate = endDate,
    )
}
