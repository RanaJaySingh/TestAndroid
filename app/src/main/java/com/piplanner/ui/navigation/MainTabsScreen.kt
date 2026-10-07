package com.piplanner.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.piplanner.domain.TransferAskPrefill
import com.piplanner.ui.ask.AskTab
import com.piplanner.ui.goals.GoalsTab
import com.piplanner.ui.goals.GoalsViewModel
import com.piplanner.ui.history.HistoryTab
import com.piplanner.ui.history.HistoryViewModel
import com.piplanner.ui.theme.MainTabChrome
import com.piplanner.ui.theme.PiPlannerColors

/**
 * Post-setup shell — Spec §5.3 / §3.4 Bottom nav: Goals | History | Ask (Settings via gear).
 * PIP-72: Material Icons + navy selected-state chrome (visual only).
 */
@Composable
fun MainTabsScreen(
    onOpenGoal: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreditEntry: (String) -> Unit,
    onOpenWithdrawal: (previousPaisa: Long, newPaisa: Long, isTyped: Boolean) -> Unit,
    onOpenStandingSplit: () -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    onOpenHistoryOpening: (String) -> Unit = {},
    onOpenHistoryDetail: (String) -> Unit = {},
    onOpenTransferPrefill: (TransferAskPrefill) -> Unit = {},
    goalsViewModel: GoalsViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.semantics { contentDescription = "Main tab bar" },
        bottomBar = {
            MainTabsBottomBar(
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding),
        ) {
            when (selectedIndex) {
                MainTabChrome.Tab.Goals.index -> GoalsTab(
                    viewModel = goalsViewModel,
                    onOpenGoal = onOpenGoal,
                    onOpenSettings = onOpenSettings,
                    onOpenCreditEntry = onOpenCreditEntry,
                    onOpenWithdrawal = onOpenWithdrawal,
                    onOpenStandingSplit = onOpenStandingSplit,
                    onOpenTransfer = onOpenTransfer,
                )
                MainTabChrome.Tab.History.index -> HistoryTab(
                    viewModel = historyViewModel,
                    onOpenCreditEntry = onOpenCreditEntry,
                    onOpenOpeningEntry = onOpenHistoryOpening,
                    onOpenLockedDetail = onOpenHistoryDetail,
                )
                else -> AskTab(
                    onOpenTransfer = onOpenTransferPrefill,
                    onOpenStandingSplit = onOpenStandingSplit,
                )
            }
        }
    }
}

/**
 * Goals · History · Ask NavigationBar with Material icons and selected-state chrome.
 * Extracted for Compose UI checks without Hilt ViewModels.
 */
@Composable
fun MainTabsBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.semantics { contentDescription = "Main tab bar" },
        containerColor = PiPlannerColors.SurfaceCard,
    ) {
        MainTabChrome.Tab.entries.forEach { tab ->
            val selected = selectedIndex == tab.index
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.index) },
                icon = {
                    Icon(
                        imageVector = tab.icon(selected = selected),
                        contentDescription = tab.contentDescription,
                    )
                },
                label = { Text(tab.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MainTabChrome.selectedTint,
                    selectedTextColor = MainTabChrome.selectedTint,
                    indicatorColor = MainTabChrome.selectedIndicator,
                    unselectedIconColor = PiPlannerColors.OnSurface.copy(alpha = 0.62f),
                    unselectedTextColor = PiPlannerColors.OnSurface.copy(alpha = 0.62f),
                ),
                modifier = Modifier.semantics {
                    contentDescription = tab.contentDescription
                },
            )
        }
    }
}
