package com.piplanner.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.domain.GoalsTabService
import com.piplanner.domain.HistoryService
import org.junit.Test

/**
 * Icon catalog + MainTab chrome contract (PIP-72 / Tech Spec §3.3 / §3.4 / R4–R5).
 */
class PiIconsTest {

    private val historyService = HistoryService(com.piplanner.domain.FormattingService())

    @Test
    fun spec33MetaphorMaterialNames() {
        assertThat(PiIcons.Name.GOALS_TAB).isEqualTo("flag")
        assertThat(PiIcons.Name.HISTORY_TAB).isEqualTo("history")
        assertThat(PiIcons.Name.ASK_TAB).isEqualTo("chat")
        assertThat(PiIcons.Name.SETTINGS).isEqualTo("settings")
        assertThat(PiIcons.Name.LOCK).isEqualTo("lock")
        assertThat(PiIcons.Name.SYNC).isEqualTo("sync")
        assertThat(PiIcons.Name.TRANSFER).isEqualTo("swap_horiz")
        assertThat(PiIcons.Name.WITHDRAWAL).isEqualTo("arrow_downward")
        assertThat(PiIcons.Name.NEW_CREDIT).isEqualTo("add_circle")
    }

    @Test
    fun catalogHasNoGlyphPlaceholdersAndCoversRequiredMetaphors() {
        val metaphors = PiIcons.catalog.map { it.first }.toSet()
        for (required in listOf(
            "Goals tab", "History tab", "Ask tab",
            "Settings gear", "Lock (saved)", "Sync", "Transfer",
            "Withdrawal / down", "New credit / add",
        )) {
            assertThat(metaphors).contains(required)
        }
        for ((_, materialName) in PiIcons.catalog) {
            assertThat(materialName).isNotEmpty()
            assertThat(materialName.all { it.code <= 0x7F }).isTrue()
            assertThat(materialName).doesNotContain("◎")
            assertThat(materialName).doesNotContain("◷")
            assertThat(materialName).isNotEqualTo("?")
            assertThat(materialName).isNotEqualTo("⚙")
            assertThat(materialName).isNotEqualTo("⊠")
        }
    }

    @Test
    fun mainTabChromeExactlyThreeGoalsHistoryAskInOrder() {
        assertThat(MainTabChrome.tabCount).isEqualTo(3)
        assertThat(MainTabChrome.titlesInOrder).containsExactly("Goals", "History", "Ask").inOrder()
        assertThat(MainTabChrome.titlesInOrder).isEqualTo(GoalsTabService.TAB_TITLES)
        assertThat(MainTabChrome.titlesInOrder).doesNotContain("Settings")
        assertThat(MainTabChrome.Tab.entries.map { it.index }).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun mainTabChromeSelectedVsUnselectedIconsDifferInWeight() {
        assertThat(MainTabChrome.Tab.Goals.icon(selected = false))
            .isEqualTo(PiIcons.goalsTab)
        assertThat(MainTabChrome.Tab.Goals.icon(selected = true))
            .isEqualTo(PiIcons.goalsTabSelected)
        assertThat(MainTabChrome.Tab.History.icon(selected = false))
            .isEqualTo(PiIcons.historyTab)
        assertThat(MainTabChrome.Tab.History.icon(selected = true))
            .isEqualTo(PiIcons.historyTabSelected)
        assertThat(MainTabChrome.Tab.Ask.icon(selected = false))
            .isEqualTo(PiIcons.askTab)
        assertThat(MainTabChrome.Tab.Ask.icon(selected = true))
            .isEqualTo(PiIcons.askTabSelected)

        assertThat(MainTabChrome.Tab.Goals.icon(selected = false))
            .isNotEqualTo(MainTabChrome.Tab.Goals.icon(selected = true))
        assertThat(MainTabChrome.Tab.History.icon(selected = false))
            .isNotEqualTo(MainTabChrome.Tab.History.icon(selected = true))
        assertThat(MainTabChrome.Tab.Ask.icon(selected = false))
            .isNotEqualTo(MainTabChrome.Tab.Ask.icon(selected = true))
    }

    @Test
    fun selectedTabTintUsesNavyPrimaryToken() {
        assertThat(MainTabChrome.selectedTint).isEqualTo(PiPlannerColors.NavyPrimary)
        assertThat(MainTabChrome.selectedTint).isEqualTo(Color(0xFF0A2A6B))
        assertThat(MainTabChrome.selectedTint).isNotEqualTo(Color(0xFF0B3D2E))
        assertThat(MainTabChrome.selectedIndicator).isEqualTo(PiPlannerColors.ChipLightBlue)
    }

    @Test
    fun tabContentDescriptionsStable() {
        assertThat(MainTabChrome.Tab.Goals.contentDescription).isEqualTo("Goals tab")
        assertThat(MainTabChrome.Tab.History.contentDescription).isEqualTo("History tab")
        assertThat(MainTabChrome.Tab.Ask.contentDescription).isEqualTo("Ask tab")
    }

    @Test
    fun historyServiceUsesCatalogForSpec33Types() {
        assertThat(historyService.typeIcon(HistoryEntryType.NewCredit))
            .isEqualTo(PiIcons.Name.NEW_CREDIT)
        assertThat(historyService.typeIcon(HistoryEntryType.Transfer))
            .isEqualTo(PiIcons.Name.TRANSFER)
        assertThat(historyService.typeIcon(HistoryEntryType.Withdrawal))
            .isEqualTo(PiIcons.Name.WITHDRAWAL)
        assertThat(historyService.typeIcon(HistoryEntryType.OpeningBalance))
            .isEqualTo(PiIcons.Name.OPENING_BALANCE)
        assertThat(historyService.typeIcon(HistoryEntryType.GoalDeleted))
            .isEqualTo(PiIcons.Name.GOAL_DELETED)

        HistoryEntryType.entries.forEach { type ->
            val name = historyService.typeIcon(type)
            assertThat(name).isEqualTo(PiIcons.historyTypeName(type))
            assertThat(PiIcons.resolve(name)).isEqualTo(PiIcons.historyTypeIcon(type))
            assertThat(name).doesNotContain("◎")
            assertThat(name).doesNotContain("+")
            assertThat(name).doesNotContain("↔")
        }
    }
}
