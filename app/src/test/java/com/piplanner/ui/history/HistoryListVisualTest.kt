package com.piplanner.ui.history

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.domain.HistoryService
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import org.junit.Test

/**
 * PIP-92 visual contract smoke for History list (frames 12 / 12a).
 * Behaviour coverage remains in HistoryViewModelTest / HistoryServiceTest.
 */
class HistoryListVisualTest {

    @Test
    fun listChrome_usesAppBackgroundAndCardTokens() {
        assertThat(PiPlannerColors.BackgroundApp).isEqualTo(Color(0xFFF5F7FB))
        assertThat(PiPlannerColors.SurfaceCard).isEqualTo(Color(0xFFFFFFFF))
        assertThat(PiPlannerDimens.RadiusCard.value).isAtLeast(20f)
        assertThat(PiPlannerDimens.RadiusCard.value).isAtMost(24f)
        assertThat(PiPlannerDimens.ElevationCard).isEqualTo(2.dp)
    }

    @Test
    fun openVsLockedChrome_flagsMatchRowUiContract() {
        val open = HistoryRowUi(
            id = "open",
            type = HistoryEntryType.NewCredit,
            typeLabel = HistoryService.TYPE_NEW_CREDIT,
            typeIcon = HistoryService.TYPE_ICON_NEW_CREDIT,
            subtitle = HistoryService.ASSIGN_NOW_SUBTITLE,
            formattedAmount = "₹10,000",
            isLocked = false,
            isOpenAssignable = true,
            showLockIcon = false,
        )
        val locked = open.copy(
            id = "locked",
            subtitle = "2024-02-01",
            isLocked = true,
            isOpenAssignable = false,
            showLockIcon = true,
        )

        assertThat(open.isOpenAssignable).isTrue()
        assertThat(open.showLockIcon).isFalse()
        assertThat(open.subtitle).isEqualTo(HistoryService.ASSIGN_NOW_SUBTITLE)

        assertThat(locked.isOpenAssignable).isFalse()
        assertThat(locked.showLockIcon).isTrue()
        assertThat(locked.isLocked).isTrue()
    }

    @Test
    fun typeAndLockIcons_resolveFromPiIconsCatalog() {
        assertThat(PiIcons.resolve(HistoryService.TYPE_ICON_NEW_CREDIT)).isEqualTo(PiIcons.newCredit)
        assertThat(PiIcons.resolve(HistoryService.TYPE_ICON_TRANSFER)).isEqualTo(PiIcons.transfer)
        assertThat(PiIcons.resolve(HistoryService.TYPE_ICON_WITHDRAWAL)).isEqualTo(PiIcons.withdrawal)
        assertThat(PiIcons.resolve(PiIcons.Name.LOCK)).isEqualTo(PiIcons.lock)
        assertThat(PiIcons.historyTab).isNotNull()
    }

    @Test
    fun openChrome_usesNavyAndLightBlueTokens() {
        assertThat(PiPlannerColors.NavyPrimary).isEqualTo(Color(0xFF0A2A6B))
        assertThat(PiPlannerColors.ChipLightBlue).isEqualTo(Color(0xFFD6E8FF))
        assertThat(PiPlannerColors.OnChipLightBlue).isEqualTo(PiPlannerColors.NavyPrimary)
    }
}
