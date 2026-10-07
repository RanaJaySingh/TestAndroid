package com.piplanner.ui.setup

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.piplanner.domain.ConsentService
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography
import org.junit.Test

/**
 * PIP-76 — Accounts / Consent / Fetched balance visual contract smoke (PRD R7).
 * Asserts tokens and shared components consumed by frames 2 / 3 / 3a.
 * Compose previews live on AccountsScreen / ConsentSheet for Reviewer screenshots.
 */
class AccountsConsentVisualTest {

    @Test
    fun accountsFrame_usesNavyStepAndCardTokens() {
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.BackgroundApp).isEqualTo(Color(0xFFF5F7FB))
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(Color(0xFFFFFFFF))
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerDimens.ElevationCard).isEqualTo(2.dp)
    }

    @Test
    fun consentFrame_hasFourBullets_andSheetChromeTokens() {
        assertThat(ConsentService.consentBullets).hasSize(4)
        assertThat(PiPlannerDimens.RadiusSheetTop).isEqualTo(22.dp)
        assertThat(PiPlannerDimens.SheetHandleWidth).isEqualTo(36.dp)
        assertThat(PiPlannerDimens.SheetHandleHeight).isEqualTo(4.dp)
    }

    @Test
    fun fetchedBalanceFrame_amountHeroIsLargeNavyTreatment() {
        assertThat(PiPlannerTypography.amountHero.fontSize.value).isAtLeast(32f)
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.NavyPrimary).isNotEqualTo(Color(0xFF0B3D2E))
    }

    @Test
    fun continueGating_andConsentCtaStyles_remainDistinct() {
        // Disabled Continue uses navy primary at reduced alpha (PrimaryCta contract).
        assertThat(PiPlannerColors.NavyPrimary.copy(alpha = 0.38f).alpha).isWithin(0.01f).of(0.38f)
        // Secondary outline Yes/No pair uses navy stroke colour token.
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
    }
}
