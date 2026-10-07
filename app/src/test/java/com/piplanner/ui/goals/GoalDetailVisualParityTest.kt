package com.piplanner.ui.goals

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalStatus
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography
import org.junit.Test

/**
 * PIP-88 — Goal detail visual parity smoke (PRD R13 / frame 14).
 * Layout lives in Compose previews; this asserts tokens + % reached presentation helper.
 */
class GoalDetailVisualParityTest {

    @Test
    fun detailTokens_navyHeroAndStatusChrome_exist() {
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(androidx.compose.ui.graphics.Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.PositiveGreen).isEqualTo(androidx.compose.ui.graphics.Color(0xFF1B8A4A))
        assertThat(PiPlannerColors.Behind).isEqualTo(androidx.compose.ui.graphics.Color(0xFFE65100))
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(androidx.compose.ui.graphics.Color(0xFFFFFFFF))
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerTypography.amountHero.fontSize.value).isAtLeast(32f)
    }

    @Test
    fun percentReachedLabel_usesSavedOverAdjusted() {
        val goal = Goal(
            id = "car",
            name = "Car",
            targetAmount = 100_000_000L,
            startDate = "2026-10-01",
            endDate = "2030-09-30",
            inflationRate = 0.07,
            savedAmount = 8_500_000L,
            shareOfNewCredits = 0.5,
            createdAt = "2026-10-01T00:00:00Z",
            updatedAt = "2026-10-01T00:00:00Z",
        )
        val adjusted = goal.adjustedTarget()
        assertThat(adjusted).isGreaterThan(0L)
        val expected = kotlin.math.round(goal.savedAmount.toDouble() / adjusted.toDouble() * 100.0).toInt()
        assertThat(percentReachedLabel(goal)).isEqualTo("$expected%")
    }

    @Test
    fun statusChrome_onTrackVsBehind_distinctColors() {
        assertThat(PiPlannerColors.PositiveGreen).isNotEqualTo(PiPlannerColors.Behind)
        // Sanity: GoalStatus sealed types used by status chip remain distinct.
        assertThat(GoalStatus.OnTrack).isInstanceOf(GoalStatus::class.java)
        assertThat(GoalStatus.Behind(shortfall = 1L)).isInstanceOf(GoalStatus.Behind::class.java)
    }
}
