package com.piplanner.ui.setup

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import org.junit.Test

/**
 * PIP-78 — Update balance / UPI Demo chrome token contract smoke (PRD R8).
 * Compose previews live on [UpdateBalanceUPIChrome] for visual review.
 */
class UpdateBalanceUPIChromeTest {

    @Test
    fun choiceRowTokens_whiteCard_radiusAndSoftElevation() {
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(Color(0xFFFFFFFF))
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerDimens.ElevationCard).isEqualTo(2.dp)
    }

    @Test
    fun demoBadgeTokens_lightBlueChipFill_navyLabel() {
        assertThat(PiPlannerColors.ChipLightBlue).isEqualTo(Color(0xFFD6E8FF))
        assertThat(PiPlannerColors.OnChipLightBlue).isEqualTo(PiPlannerColors.NavyPrimary)
        assertThat(PiPlannerDimens.RadiusChip.value).isAtLeast(10f)
        assertThat(PiPlannerDimens.RadiusChip.value).isAtMost(12f)
    }

    @Test
    fun pinErrorAndPrimaryCtaTokens_destructiveAndNavy() {
        assertThat(PiPlannerColors.Destructive).isEqualTo(Color(0xFFC62828))
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.OnNavy).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun choiceIcons_manuallyAndBalanceSync_resolve() {
        assertThat(UpdateBalanceChoiceIcons.Manually).isNotNull()
        assertThat(UpdateBalanceChoiceIcons.BalanceSync).isNotNull()
    }
}
