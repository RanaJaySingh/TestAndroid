package com.piplanner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.piplanner.domain.GoalsTabService
import com.piplanner.ui.ask.AskTabPlaceholder
import com.piplanner.ui.goals.GoalsTab
import com.piplanner.ui.goals.GoalsViewModel
import com.piplanner.ui.history.HistoryTabPlaceholder

/**
 * Post-setup shell — Spec §5.3 Bottom nav: Goals | History | Ask (Settings via gear).
 */
@Composable
fun MainTabsScreen(
    onOpenGoal: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreditEntry: (String) -> Unit,
    onOpenWithdrawal: (previousPaisa: Long, newPaisa: Long, isTyped: Boolean) -> Unit,
    onOpenStandingSplit: () -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    goalsViewModel: GoalsViewModel = hiltViewModel(),
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val titles = GoalsTabService.TAB_TITLES

    Scaffold(
        modifier = Modifier.semantics { contentDescription = "Main tab bar" },
        bottomBar = {
            NavigationBar {
                titles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = {
                            Text(
                                text = when (index) {
                                    0 -> "◎"
                                    1 -> "◷"
                                    else -> "?"
                                },
                            )
                        },
                        label = { Text(title) },
                        modifier = Modifier.semantics {
                            contentDescription = when (index) {
                                0 -> "Goals tab"
                                1 -> "History tab"
                                else -> "Ask tab"
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding),
        ) {
            when (selectedIndex) {
                0 -> GoalsTab(
                    viewModel = goalsViewModel,
                    onOpenGoal = onOpenGoal,
                    onOpenSettings = onOpenSettings,
                    onOpenCreditEntry = onOpenCreditEntry,
                    onOpenWithdrawal = onOpenWithdrawal,
                    onOpenStandingSplit = onOpenStandingSplit,
                    onOpenTransfer = onOpenTransfer,
                )
                1 -> HistoryTabPlaceholder()
                else -> AskTabPlaceholder()
            }
        }
    }
}
