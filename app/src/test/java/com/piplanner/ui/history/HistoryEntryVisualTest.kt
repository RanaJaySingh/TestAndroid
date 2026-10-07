package com.piplanner.ui.history

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.piplanner.domain.CreditEntryService
import com.piplanner.domain.HistoryService
import com.piplanner.domain.OpeningSplitService
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import org.junit.Test

/**
 * PIP-86 — History credit entry open / locked / typed visual contract smoke (PRD R12).
 * Compose screens consume these tokens/copy; behaviour stays in CreditEntryService tests.
 */
class HistoryEntryVisualTest {

    @Test
    fun openEntry_assignNowCopy_matchesDesign() {
        assertThat(CreditEntryService.ASSIGN_NOW_TITLE).isEqualTo("Assign now")
        assertThat(CreditEntryService.LOCKED_ONCE_CAPTION)
            .isEqualTo("You can change this split once.")
    }

    @Test
    fun lockedEntry_captionAndTokens_matchDesign() {
        assertThat(OpeningSplitService.LOCKED_AMOUNTS_CAPTION)
            .isEqualTo("Locked amounts never change")
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.ChipLightBlue).isEqualTo(Color(0xFFD6E8FF))
        assertThat(PiPlannerColors.OnChipLightBlue).isEqualTo(PiPlannerColors.NavyPrimary)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerDimens.RadiusChip).isEqualTo(12.dp)
        assertThat(PiPlannerColors.BackgroundApp).isEqualTo(Color(0xFFF5F7FB))
    }

    @Test
    fun historyDetail_originalAmountsCaption_unchanged() {
        assertThat(HistoryService.ORIGINAL_AMOUNTS_CAPTION)
            .isEqualTo("Original amounts never change")
    }
}
