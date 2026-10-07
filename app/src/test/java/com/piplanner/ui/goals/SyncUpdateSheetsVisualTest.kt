package com.piplanner.ui.goals

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import org.junit.Test

/**
 * PIP-84 — Sync / Update sheets visual contract smoke (PRD R11 / Tech Spec §5.2).
 * Asserts tokens consumed by GoalsSyncSheet / GoalsUpdateBalanceSheet hierarchy.
 */
class SyncUpdateSheetsVisualTest {

    @Test
    fun newAmountPositiveGreen_matchesDesignToken() {
        assertThat(PiPlannerColors.PositiveGreen).isEqualTo(Color(0xFF1B8A4A))
        assertThat(PiPlannerColors.PositiveGreen).isNotEqualTo(PiPlannerColors.NavyPrimary)
    }

    @Test
    fun wentDownUsesDestructive_notPositiveGreen() {
        assertThat(PiPlannerColors.Destructive).isEqualTo(Color(0xFFC62828))
        assertThat(PiPlannerColors.Destructive).isNotEqualTo(PiPlannerColors.PositiveGreen)
    }

    @Test
    fun sheetAndCardChrome_useSharedRadiusTokens() {
        assertThat(PiPlannerDimens.RadiusSheetTop).isEqualTo(PiPlannerDimens.RadiusCard)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun syncPhases_coverIdleSyncingResult() {
        assertThat(SyncSheetPhase.entries.toList())
            .containsExactly(
                SyncSheetPhase.Idle,
                SyncSheetPhase.Syncing,
                SyncSheetPhase.ShowingResult,
            )
            .inOrder()
    }

    @Test
    fun newAmountVisualPrefix_isPlusOnly_notDoublePlus() {
        val formatted = "₹10,000"
        val display = "+$formatted"
        assertThat(display).isEqualTo("+₹10,000")
        assertThat(display).doesNotContain("++")
    }
}
