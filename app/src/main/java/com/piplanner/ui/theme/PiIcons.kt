package com.piplanner.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.ui.graphics.vector.ImageVector
import com.piplanner.data.model.HistoryEntryType

/**
 * Material Icons catalog for Tech Spec §3.3 (PIP-72 / PRD R4).
 *
 * Outline for unselected tab chrome; filled for selected where available.
 * String [Name] values are JVM-testable and match Spec Material names.
 */
object PiIcons {

    object Name {
        const val GOALS_TAB: String = "flag"
        const val GOALS_TAB_SELECTED: String = "flag"
        const val HISTORY_TAB: String = "history"
        const val HISTORY_TAB_SELECTED: String = "history"
        const val ASK_TAB: String = "chat"
        const val ASK_TAB_SELECTED: String = "chat"

        const val SETTINGS: String = "settings"
        const val LOCK: String = "lock"
        const val SYNC: String = "sync"
        const val TRANSFER: String = "swap_horiz"
        const val WITHDRAWAL: String = "arrow_downward"
        const val NEW_CREDIT: String = "add_circle"
        const val OPENING_BALANCE: String = "savings"
        const val GOAL_DELETED: String = "delete"

        const val HEADER_SEARCH: String = "search"
        const val HEADER_NOTIFICATIONS: String = "notifications"
        const val HEADER_CHART: String = "bar_chart"
    }

    // Tab bar (R5 / §3.4)

    val goalsTab: ImageVector = Icons.Outlined.Flag
    val goalsTabSelected: ImageVector = Icons.Filled.Flag

    val historyTab: ImageVector = Icons.Outlined.History
    val historyTabSelected: ImageVector = Icons.Filled.History

    val askTab: ImageVector = Icons.AutoMirrored.Outlined.Chat
    val askTabSelected: ImageVector = Icons.AutoMirrored.Filled.Chat

    // Actions / chrome

    val settings: ImageVector = Icons.Outlined.Settings
    val lock: ImageVector = Icons.Filled.Lock
    val sync: ImageVector = Icons.Outlined.Sync
    val syncFilled: ImageVector = Icons.Filled.Sync
    val transfer: ImageVector = Icons.Outlined.SwapHoriz
    val transferFilled: ImageVector = Icons.Filled.SwapHoriz
    val withdrawal: ImageVector = Icons.Outlined.ArrowDownward
    val newCredit: ImageVector = Icons.Outlined.AddCircle
    val newCreditFilled: ImageVector = Icons.Filled.AddCircle
    val openingBalance: ImageVector = Icons.Outlined.Savings
    val openingBalanceFilled: ImageVector = Icons.Filled.Savings
    val goalDeleted: ImageVector = Icons.Filled.Delete

    // Header chrome only (A2 / R20) — catalogued for later Goals home chrome
    val headerSearch: ImageVector = Icons.Filled.Search
    val headerNotifications: ImageVector = Icons.Filled.Notifications
    val headerChart: ImageVector = Icons.Filled.BarChart

    /** Canonical metaphor → Material name pairs for Spec §3.3 smoke / Reviewer checklist. */
    val catalog: List<Pair<String, String>> = listOf(
        "Goals tab" to Name.GOALS_TAB,
        "History tab" to Name.HISTORY_TAB,
        "Ask tab" to Name.ASK_TAB,
        "Settings gear" to Name.SETTINGS,
        "Lock (saved)" to Name.LOCK,
        "Sync" to Name.SYNC,
        "Transfer" to Name.TRANSFER,
        "Withdrawal / down" to Name.WITHDRAWAL,
        "New credit / add" to Name.NEW_CREDIT,
        "Header Search" to Name.HEADER_SEARCH,
        "Header notifications" to Name.HEADER_NOTIFICATIONS,
        "Header chart" to Name.HEADER_CHART,
    )

    fun historyTypeIcon(type: HistoryEntryType): ImageVector {
        return when (type) {
            HistoryEntryType.OpeningBalance -> openingBalance
            HistoryEntryType.NewCredit -> newCredit
            HistoryEntryType.Transfer -> transfer
            HistoryEntryType.Withdrawal -> withdrawal
            HistoryEntryType.GoalDeleted -> goalDeleted
        }
    }

    fun historyTypeName(type: HistoryEntryType): String {
        return when (type) {
            HistoryEntryType.OpeningBalance -> Name.OPENING_BALANCE
            HistoryEntryType.NewCredit -> Name.NEW_CREDIT
            HistoryEntryType.Transfer -> Name.TRANSFER
            HistoryEntryType.Withdrawal -> Name.WITHDRAWAL
            HistoryEntryType.GoalDeleted -> Name.GOAL_DELETED
        }
    }

    fun resolve(name: String): ImageVector {
        return when (name) {
            Name.GOALS_TAB, Name.GOALS_TAB_SELECTED -> goalsTab
            Name.HISTORY_TAB, Name.HISTORY_TAB_SELECTED -> historyTab
            Name.ASK_TAB, Name.ASK_TAB_SELECTED -> askTab
            Name.SETTINGS -> settings
            Name.LOCK -> lock
            Name.SYNC -> sync
            Name.TRANSFER -> transfer
            Name.WITHDRAWAL -> withdrawal
            Name.NEW_CREDIT -> newCredit
            Name.OPENING_BALANCE -> openingBalance
            Name.GOAL_DELETED -> goalDeleted
            Name.HEADER_SEARCH -> headerSearch
            Name.HEADER_NOTIFICATIONS -> headerNotifications
            Name.HEADER_CHART -> headerChart
            else -> settings
        }
    }
}

/**
 * Tab bar chrome contract (Tech Spec §3.4 / PRD R5) — testable without Compose UI.
 *
 * Exactly three tabs Goals · History · Ask in order. No Settings tab.
 */
object MainTabChrome {
    enum class Tab(val index: Int, val title: String, val contentDescription: String) {
        Goals(0, "Goals", "Goals tab"),
        History(1, "History", "History tab"),
        Ask(2, "Ask", "Ask tab");

        val materialName: String
            get() = when (this) {
                Goals -> PiIcons.Name.GOALS_TAB
                History -> PiIcons.Name.HISTORY_TAB
                Ask -> PiIcons.Name.ASK_TAB
            }

        val selectedMaterialName: String
            get() = when (this) {
                Goals -> PiIcons.Name.GOALS_TAB_SELECTED
                History -> PiIcons.Name.HISTORY_TAB_SELECTED
                Ask -> PiIcons.Name.ASK_TAB_SELECTED
            }

        fun icon(selected: Boolean): ImageVector {
            return when (this) {
                Goals -> if (selected) PiIcons.goalsTabSelected else PiIcons.goalsTab
                History -> if (selected) PiIcons.historyTabSelected else PiIcons.historyTab
                Ask -> if (selected) PiIcons.askTabSelected else PiIcons.askTab
            }
        }

        fun materialName(selected: Boolean): String {
            return if (selected) selectedMaterialName else materialName
        }
    }

    val tabCount: Int = Tab.entries.size

    val titlesInOrder: List<String> = Tab.entries.map { it.title }

    /** Selected tab tint — [PiPlannerColors.NavyPrimary] (#0A2A6B). */
    val selectedTint = PiPlannerColors.NavyPrimary

    /** Soft selected indicator under icon (light-blue chip token). */
    val selectedIndicator = PiPlannerColors.ChipLightBlue
}
