package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.piplanner.R
import com.piplanner.domain.CreditEntryService
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Goals open-entry banner (frame 9b / PRD R10 / Spec BR-6) — Assign now treatment.
 * Visual restyle only (PIP-82); Assign now still opens existing credit entry.
 */
@Composable
fun OpenEntryBanner(
    message: String,
    onAssignNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Open entry banner" },
        color = PiPlannerColors.ChipLightBlue,
        shape = RoundedCornerShape(PiPlannerDimens.RadiusCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PiPlannerDimens.Space16),
            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8 / 2),
            ) {
                Text(
                    text = CreditEntryService.OPEN_ENTRY_BANNER_PREFIX,
                    style = PiPlannerTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.NavyPrimary,
                )
                Text(
                    text = message,
                    style = PiPlannerTypography.caption,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.62f),
                )
            }
            Button(
                onClick = onAssignNow,
                modifier = Modifier.semantics {
                    contentDescription = CreditEntryService.ASSIGN_NOW_TITLE
                },
                shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PiPlannerColors.NavyPrimary,
                    contentColor = PiPlannerColors.OnNavy,
                ),
                contentPadding = PaddingValues(
                    horizontal = PiPlannerDimens.Space12,
                    vertical = PiPlannerDimens.Space8,
                ),
            ) {
                Text(
                    text = stringResource(R.string.credit_assign_now),
                    style = PiPlannerTypography.caption,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
