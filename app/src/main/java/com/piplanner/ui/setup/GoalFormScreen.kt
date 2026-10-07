package com.piplanner.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Goal form · create / edit — design frame 6 (PRD R5 / R9).
 * Visual parity (PIP-80): field stack, inflation row, live targets, valid/invalid chrome.
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
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space20),
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.goal_form_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnBackground,
            )
            Text(
                text = stringResource(R.string.goal_form_subtitle),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (canSave) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 1.5.dp,
                            color = PiPlannerColors.Behind.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(PiPlannerDimens.RadiusCard),
                        )
                    },
                )
                .semantics {
                    contentDescription = if (canSave) "goalForm.valid" else "goalForm.invalid"
                },
        ) {
            PiCard(contentDescription = "Goal form fields") {
                Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16)) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { onDraftChange(draft.copy(name = it)) },
                        label = {
                            Text(
                                text = stringResource(R.string.goal_name_label),
                                style = PiPlannerTypography.caption,
                            )
                        },
                        placeholder = { Text(stringResource(R.string.goal_name_placeholder)) },
                        singleLine = true,
                        colors = goalFormFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Goal name" },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                        Text(
                            text = stringResource(R.string.goal_target_label),
                            style = PiPlannerTypography.caption,
                            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                        )
                        OutlinedTextField(
                            value = draft.targetRupeeDigits,
                            onValueChange = { raw ->
                                onDraftChange(draft.copy(targetRupeeDigits = raw.filter { it.isDigit() }))
                            },
                            prefix = {
                                Text(
                                    text = "₹",
                                    style = PiPlannerTypography.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PiPlannerColors.NavyPrimary,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = goalFormFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Target in rupees" },
                        )
                        Text(
                            text = formatInr(targetPaisa),
                            style = PiPlannerTypography.caption,
                            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                        )
                    }

                    OutlinedTextField(
                        value = draft.startDate.toString(),
                        onValueChange = { value ->
                            parseLocalDateOrNull(value)?.let { onDraftChange(draft.copy(startDate = it)) }
                        },
                        label = { Text(stringResource(R.string.goal_start_label)) },
                        singleLine = true,
                        colors = goalFormFieldColors(),
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
                        colors = goalFormFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text(stringResource(R.string.goal_date_hint)) },
                    )

                    if (!canSave) {
                        Text(
                            text = stringResource(R.string.goal_form_incomplete),
                            style = PiPlannerTypography.caption,
                            color = PiPlannerColors.Behind,
                            modifier = Modifier.semantics {
                                contentDescription = "Form incomplete"
                            },
                        )
                    }
                }
            }
        }

        PiCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenInflation)
                .semantics {
                    contentDescription = "Inflation $inflationPercent percent"
                },
            contentDescription = "Inflation row",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8 / 2)) {
                    Text(
                        text = stringResource(R.string.inflation_title),
                        style = PiPlannerTypography.caption,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    )
                    Text(
                        text = stringResource(R.string.percent_value, inflationPercent),
                        style = PiPlannerTypography.body,
                        fontWeight = FontWeight.SemiBold,
                        color = PiPlannerColors.NavyPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.edit),
                    style = PiPlannerTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.NavyPrimary,
                )
            }
        }

        PiCard(contentDescription = "Share of new credits") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.goal_share_label),
                        style = PiPlannerTypography.caption,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    )
                    Text(
                        text = stringResource(R.string.percent_value, sharePercent),
                        style = PiPlannerTypography.body,
                        fontWeight = FontWeight.SemiBold,
                        color = PiPlannerColors.NavyPrimary,
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
                    colors = SliderDefaults.colors(
                        thumbColor = PiPlannerColors.NavyPrimary,
                        activeTrackColor = PiPlannerColors.NavyPrimary,
                        inactiveTrackColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.20f),
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Share of new credits"
                    },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8 / 2)) {
            Text(
                text = stringResource(R.string.goal_saved_so_far),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Text(
                text = formatInr(draft.savedAmount),
                style = PiPlannerTypography.body,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Text(
                text = stringResource(R.string.goal_saved_locked_hint),
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }

        PiCard(contentDescription = "Goal metrics") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
                MetricRow(
                    title = stringResource(R.string.inflation_adjusted_target),
                    value = formatInr(draft.adjustedTargetPaisa(validation)),
                )
                MetricRow(
                    title = stringResource(R.string.goal_monthly_need),
                    value = formatInr(draft.monthlyNeedPaisa(validation)),
                )
            }
        }

        PrimaryCta(
            text = stringResource(R.string.save),
            onClick = onSave,
            enabled = canSave,
            contentDescription = "Save goal",
        )

        SecondaryCta(
            text = stringResource(R.string.cancel),
            onClick = onCancel,
            style = SecondaryCtaStyle.Text,
            contentDescription = "Cancel",
        )
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
            style = PiPlannerTypography.caption,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
        )
        Text(
            text = value,
            style = PiPlannerTypography.body,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnSurface,
        )
    }
}

private fun parseLocalDateOrNull(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value) }.getOrNull()
}
