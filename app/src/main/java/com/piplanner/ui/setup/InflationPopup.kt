package com.piplanner.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piplanner.R
import com.piplanner.domain.GoalValidationService
import com.piplanner.ui.components.LightBlueChip
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheet
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Inflation rate sheet — design frame 7. Default 5% with typed rate + live adjusted target.
 * PIP-110: editable numeric field is primary; −/+ steppers are optional extras.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InflationPopup(
    inflationRate: Double,
    @Suppress("UNUSED_PARAMETER") targetPaisa: Long,
    @Suppress("UNUSED_PARAMETER") startDate: LocalDate,
    @Suppress("UNUSED_PARAMETER") endDate: LocalDate,
    formattedAdjustedTarget: String,
    onRateChange: (Double) -> Unit,
    onDone: () -> Unit,
) {
    // targetPaisa / dates kept for call-site API parity; live ₹ is preformatted by the host.
    var rateText by remember {
        mutableStateOf(GoalValidationService.inflationPercentText(inflationRate))
    }
    var rateError by remember { mutableStateOf(false) }

    fun commitPercent(percent: Int) {
        val bounded = percent.coerceIn(
            GoalValidationService.MIN_INFLATION_PERCENT,
            GoalValidationService.MAX_INFLATION_PERCENT,
        )
        rateText = bounded.toString()
        rateError = false
        onRateChange(bounded / 100.0)
    }

    fun onTypedRate(raw: String) {
        // Whole-percent digits only (matches prior 0…30 stepper domain).
        val filtered = raw.filter { it.isDigit() }
        rateText = filtered
        val parsed = GoalValidationService.parseInflationPercentInput(filtered)
        if (parsed != null) {
            rateError = false
            onRateChange(parsed)
        } else {
            rateError = filtered.isNotEmpty()
        }
    }

    fun finish() {
        val parsed = GoalValidationService.parseInflationPercentInput(rateText)
        if (parsed == null) {
            onRateChange(GoalValidationService.DEFAULT_INFLATION_RATE)
        }
        onDone()
    }

    val displayPercent = (inflationRate * 100.0).roundToInt()
        .coerceIn(
            GoalValidationService.MIN_INFLATION_PERCENT,
            GoalValidationService.MAX_INFLATION_PERCENT,
        )

    PiSheet(
        onDismissRequest = finish,
        contentDescription = "Inflation sheet",
    ) {
        Text(
            text = stringResource(R.string.inflation_title),
            style = PiPlannerTypography.title,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface,
        )
        Text(
            text = stringResource(R.string.inflation_subtitle),
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            modifier = Modifier.semantics { contentDescription = "inflation.rate" },
        ) {
            Text(
                text = stringResource(R.string.inflation_rate_label),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
            ) {
                LightBlueChip(
                    label = "−",
                    selected = false,
                    onClick = { commitPercent(displayPercent - 1) },
                    contentDescription = "Decrease inflation",
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = ::onTypedRate,
                    singleLine = true,
                    isError = rateError,
                    suffix = {
                        Text(
                            text = "%",
                            style = PiPlannerTypography.title,
                            fontWeight = FontWeight.SemiBold,
                            color = PiPlannerColors.NavyPrimary,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = goalFormFieldColors(),
                    supportingText = if (rateError) {
                        {
                            Text(
                                text = stringResource(R.string.inflation_rate_error),
                                color = PiPlannerColors.Behind,
                            )
                        }
                    } else {
                        null
                    },
                    textStyle = PiPlannerTypography.amountHero.copy(
                        fontWeight = FontWeight.Bold,
                        color = PiPlannerColors.NavyPrimary,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 96.dp)
                        .semantics {
                            contentDescription = if (rateError) {
                                "Inflation rate invalid"
                            } else {
                                "Inflation $displayPercent percent"
                            }
                        },
                )
                LightBlueChip(
                    label = "+",
                    selected = false,
                    onClick = { commitPercent(displayPercent + 1) },
                    contentDescription = "Increase inflation",
                )
            }
        }

        PiCard(contentDescription = "Adjusted target") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                Text(
                    text = stringResource(R.string.inflation_adjusted_target_short),
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Text(
                    text = formattedAdjustedTarget,
                    style = PiPlannerTypography.amountHero,
                    fontWeight = FontWeight.Bold,
                    color = PiPlannerColors.NavyPrimary,
                    maxLines = 1,
                    modifier = Modifier.semantics {
                        contentDescription = "Adjusted target $formattedAdjustedTarget"
                    },
                )
                Text(
                    text = stringResource(R.string.inflation_live_hint),
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
            }
        }

        Spacer(modifier = Modifier.height(PiPlannerDimens.Space8))

        PrimaryCta(
            text = stringResource(R.string.use_this_rate),
            onClick = finish,
            contentDescription = "Use this rate",
        )
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
