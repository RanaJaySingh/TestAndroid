package com.piplanner.ui.goals

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.piplanner.domain.GoalsBalanceAction
import com.piplanner.domain.GoalsTabService
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import org.junit.Test

/**
 * PIP-82 — Goals home visual contract smoke (PRD R10 / R20 / Tech Spec §5.2 J2).
 * Asserts tokens and presentation contracts for navy card, quick actions, banner, header chrome.
 */
class GoalsHomeVisualTest {

    @Test
    fun navyBalanceCard_usesNavyPrimaryAndDeepTokens() {
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.NavyDeep).isEqualTo(Color(0xFF003A8C))
        assertThat(PiPlannerColors.OnNavy).isEqualTo(Color(0xFFFFFFFF))
    }

    @Test
    fun goalCardSurface_usesWhiteCardRadius22() {
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(Color(0xFFFFFFFF))
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerDimens.RadiusCard.value).isEqualTo(22f)
    }

    @Test
    fun statusChips_usePositiveGreenAndBehindTokens() {
        assertThat(PiPlannerColors.PositiveGreen).isEqualTo(Color(0xFF1B8A4A))
        assertThat(PiPlannerColors.Behind).isEqualTo(Color(0xFFE65100))
    }

    @Test
    fun openCreditBanner_usesLightBlueFill() {
        assertThat(PiPlannerColors.ChipLightBlue).isEqualTo(Color(0xFFD6E8FF))
    }

    @Test
    fun quickActions_coverSyncUpdateNewGoalTransferHistory() {
        assertThat(GoalsTabService.QUICK_ACTION_TITLES)
            .containsExactly("Sync", "New goal", "Transfer", "History")
            .inOrder()
        assertThat(GoalsTabService.quickBalanceActionTitle(GoalsBalanceAction.Sync))
            .isEqualTo("Sync")
        assertThat(GoalsTabService.quickBalanceActionTitle(GoalsBalanceAction.UpdateBalance))
            .isEqualTo("Update")
    }

    @Test
    fun headerChromeIcons_areCatalogued_searchNotificationsChart() {
        assertThat(PiIcons.Name.HEADER_SEARCH).isEqualTo("search")
        assertThat(PiIcons.Name.HEADER_NOTIFICATIONS).isEqualTo("notifications")
        assertThat(PiIcons.Name.HEADER_CHART).isEqualTo("bar_chart")
        assertThat(PiIcons.resolve(PiIcons.Name.HEADER_SEARCH)).isEqualTo(PiIcons.headerSearch)
        assertThat(PiIcons.resolve(PiIcons.Name.HEADER_NOTIFICATIONS))
            .isEqualTo(PiIcons.headerNotifications)
        assertThat(PiIcons.resolve(PiIcons.Name.HEADER_CHART)).isEqualTo(PiIcons.headerChart)
    }

    @Test
    fun appBackground_isNotCreamGreenTheme() {
        assertThat(PiPlannerColors.BackgroundApp).isEqualTo(Color(0xFFF5F7FB))
        assertThat(PiPlannerColors.BackgroundApp).isNotEqualTo(Color(0xFFF7F4EF))
    }
}
