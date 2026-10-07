package com.piplanner.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
 * Inflation rate sheet — design frame 7. Default 7% with live adjusted target.
 * Visual parity (PIP-80): PiSheet chrome, ± stepper, live targets, “Use this rate” CTA.
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
    val percent = (inflationRate * 100.0).roundToInt().coerceIn(0, 30)

    PiSheet(
        onDismissRequest = onDone,
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
            modifier = Modifier.semantics { contentDescription = "inflation.stepper" },
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
                    onClick = {
                        val next = (percent - 1).coerceIn(0, 30)
                        onRateChange(next / 100.0)
                    },
                    contentDescription = "Decrease inflation",
                )
                Text(
                    text = stringResource(R.string.percent_value, percent),
                    style = PiPlannerTypography.amountHero,
                    fontWeight = FontWeight.Bold,
                    color = PiPlannerColors.NavyPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(min = 72.dp)
                        .semantics { contentDescription = "Inflation $percent percent" },
                )
                LightBlueChip(
                    label = "+",
                    selected = false,
                    onClick = {
                        val next = (percent + 1).coerceIn(0, 30)
                        onRateChange(next / 100.0)
                    },
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
            onClick = onDone,
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
