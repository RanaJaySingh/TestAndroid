package com.piplanner.ui.goals

import com.google.common.truth.Truth.assertThat
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import org.junit.Test

/**
 * Presentation smoke for PIP-90 sheet visuals (frames 15–18a).
 * Behaviour stays in ViewModel/Service tests; this guards design tokens + negative INR chrome.
 */
class SheetsVisualPresentationTest {

    @Test
    fun negativeAmountLabel_usesUnicodeMinusAndKeepsRupee() {
        assertThat(negativeAmountLabel("₹8,000")).isEqualTo("−₹8,000")
        assertThat(negativeAmountLabel("-₹8,000")).isEqualTo("−₹8,000")
        assertThat(negativeAmountLabel("−₹15,000")).isEqualTo("−₹15,000")
    }

    @Test
    fun sheetVisualTokens_matchSharedContract() {
        assertThat(PiPlannerColors.ChipLightBlue).isEqualTo(androidx.compose.ui.graphics.Color(0xFFD6E8FF))
        assertThat(PiPlannerColors.Destructive).isEqualTo(androidx.compose.ui.graphics.Color(0xFFC62828))
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(androidx.compose.ui.graphics.Color(0xFF0A2A6B))
        assertThat(PiPlannerDimens.RadiusSheetTop.value).isEqualTo(22f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
    }
}
