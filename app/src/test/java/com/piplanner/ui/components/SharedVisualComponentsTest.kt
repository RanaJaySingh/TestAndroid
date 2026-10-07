package com.piplanner.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.piplanner.domain.StubGrokService
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import org.junit.Test

/**
 * PIP-70 — shared visual component contract smoke (Tech Spec §3.5 / §6.2).
 * Asserts tokens consumed by PiCard / CTAs / chips / sheet / ProposalCard shell.
 * Compose previews live in [SharedVisualComponentsPreview] for visual review.
 */
class SharedVisualComponentsTest {

    @Test
    fun piCardTokens_whiteSurface_radiusInTwentyToTwentyFour_softElevation() {
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(Color(0xFFFFFFFF))
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerDimens.ElevationCard).isEqualTo(2.dp)
        assertThat(PiPlannerDimens.ElevationCard.value).isAtMost(4f)
    }

    @Test
    fun primaryCta_usesNavyPrimary_notForestGreen() {
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.NavyPrimary).isNotEqualTo(Color(0xFF0B3D2E))
        assertThat(PiPlannerColors.OnNavy).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun lightBlueChip_fillToken_matchesDesignSoftBlue() {
        assertThat(PiPlannerColors.ChipLightBlue).isEqualTo(Color(0xFFD6E8FF))
        assertThat(PiPlannerColors.OnChipLightBlue).isEqualTo(PiPlannerColors.NavyPrimary)
        assertThat(PiPlannerDimens.RadiusChip.value).isAtLeast(10f)
        assertThat(PiPlannerDimens.RadiusChip.value).isAtMost(12f)
    }

    /**
     * iOS PIP-69 / Reviewer r1: selected keeps ChipLightBlue fill + navy stroke + navy label;
     * unselected is fill-only (no navy stroke). Must not invert to navy-fill/white-label.
     */
    @Test
    fun lightBlueChip_selectedContract_keepsLightBlueFill_navyStrokeAndLabel() {
        // Selected + unselected share the soft-blue fill (not NavyPrimary invert).
        assertThat(PiPlannerColors.ChipLightBlue).isNotEqualTo(PiPlannerColors.NavyPrimary)
        assertThat(PiPlannerColors.OnChipLightBlue).isEqualTo(PiPlannerColors.NavyPrimary)
        assertThat(PiPlannerColors.OnChipLightBlue).isNotEqualTo(PiPlannerColors.OnNavy)
        // Selected stroke ~1–1.5dp; unselected uses 0dp (asserted in LightBlueChip source contract).
        assertThat(PiPlannerDimens.ChipSelectedStroke.value).isAtLeast(1f)
        assertThat(PiPlannerDimens.ChipSelectedStroke.value).isAtMost(1.5f)
    }

    @Test
    fun piSheetChrome_usesSheetTopRadiusAndHandleTokens() {
        assertThat(PiPlannerDimens.RadiusSheetTop).isEqualTo(22.dp)
        assertThat(PiPlannerDimens.SheetHandleWidth).isEqualTo(36.dp)
        assertThat(PiPlannerDimens.SheetHandleHeight).isEqualTo(4.dp)
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun proposalCardShell_checkedByCopy_matchesDesign() {
        assertThat(StubGrokService.CHECKED_BY_LABEL)
            .isEqualTo("Checked by PiPlanner. Estimate.")
    }

    @Test
    fun secondaryCtaStyles_coverOutlineAndText() {
        assertThat(SecondaryCtaStyle.entries.toList())
            .containsExactly(SecondaryCtaStyle.Outline, SecondaryCtaStyle.Text)
            .inOrder()
    }
}
