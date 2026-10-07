package com.piplanner.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * PIP-68 — token / theme unit smoke (Tech Spec §3.1, §6.2).
 * Asserts navy tokens, radii, spacing, and ColorScheme wiring without UI behaviour changes.
 */
class ThemeTokensTest {

    @Test
    fun navyPrimary_isApprovedRange_notForestGreen() {
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.NavyDeep).isEqualTo(Color(0xFF003A8C))
        assertThat(PiPlannerColors.NavyPrimary).isNotEqualTo(Color(0xFF0B3D2E))
    }

    @Test
    fun background_isNotCream_andCardSurfaceIsWhite() {
        assertThat(PiPlannerColors.BackgroundApp).isNotEqualTo(Color(0xFFF7F4EF))
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun chipPositiveBehindTokens_exist() {
        assertThat(PiPlannerColors.ChipLightBlue).isNotEqualTo(Color.Unspecified)
        assertThat(PiPlannerColors.PositiveGreen).isNotEqualTo(Color.Unspecified)
        assertThat(PiPlannerColors.Behind).isNotEqualTo(Color.Unspecified)
        assertThat(PiPlannerColors.Destructive).isNotEqualTo(Color.Unspecified)
    }

    @Test
    fun cardRadius_isInTwentyToTwentyFour_preferTwentyTwo() {
        assertThat(PiPlannerDimens.RadiusCard).isEqualTo(22.dp)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerDimens.RadiusSheetTop).isEqualTo(22.dp)
        assertThat(PiPlannerDimens.RadiusChip.value).isAtLeast(10f)
        assertThat(PiPlannerDimens.RadiusChip.value).isAtMost(12f)
    }

    @Test
    fun spacingScale_exposesEightThroughTwentyEight() {
        assertThat(PiPlannerDimens.Space8).isEqualTo(8.dp)
        assertThat(PiPlannerDimens.Space12).isEqualTo(12.dp)
        assertThat(PiPlannerDimens.Space16).isEqualTo(16.dp)
        assertThat(PiPlannerDimens.Space20).isEqualTo(20.dp)
        assertThat(PiPlannerDimens.Space24).isEqualTo(24.dp)
        assertThat(PiPlannerDimens.Space28).isEqualTo(28.dp)
    }

    @Test
    fun lightColorScheme_mapsPrimaryToNavy_notForestGreen() {
        val scheme = PiPlannerLightColorScheme
        assertThat(scheme.primary).isEqualTo(PiPlannerColors.NavyPrimary)
        assertThat(scheme.primary).isNotEqualTo(Color(0xFF0B3D2E))
        assertThat(scheme.background).isEqualTo(PiPlannerColors.BackgroundApp)
        assertThat(scheme.background).isNotEqualTo(Color(0xFFF7F4EF))
        assertThat(scheme.surface).isEqualTo(PiPlannerColors.SurfaceCard)
    }

    @Test
    fun typography_exposesAmountHeroTitleBodyCaption() {
        assertThat(PiPlannerTypography.amountHero.fontSize.value).isGreaterThan(0f)
        assertThat(PiPlannerTypography.title.fontSize.value).isGreaterThan(0f)
        assertThat(PiPlannerTypography.body.fontSize.value).isGreaterThan(0f)
        assertThat(PiPlannerTypography.caption.fontSize.value).isGreaterThan(0f)
        assertThat(PiPlannerTypography.amountHero.fontSize.value)
            .isGreaterThan(PiPlannerTypography.body.fontSize.value)
    }
}
