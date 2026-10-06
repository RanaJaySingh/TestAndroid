package com.piplanner.android.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piplanner.android.ui.screens.ask.AskScreen
import com.piplanner.android.ui.screens.goals.GoalsHomeScreen
import com.piplanner.android.ui.screens.history.HistoryScreen
import com.piplanner.android.ui.theme.NavyPrimary
import com.piplanner.android.ui.theme.Surface
import com.piplanner.android.ui.theme.TextSecondary

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Goals : BottomNavItem(
        route = "goals_tab",
        title = "Goals",
        selectedIcon = Icons.Filled.Flag,
        unselectedIcon = Icons.Outlined.Flag
    )
    
    object History : BottomNavItem(
        route = "history_tab",
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
    
    object Ask : BottomNavItem(
        route = "ask_tab",
        title = "Ask",
        selectedIcon = Icons.Outlined.QuestionAnswer,
        unselectedIcon = Icons.Outlined.QuestionAnswer
    )
}

@Composable
fun MainShell(
    onNavigateToSettings: () -> Unit,
    onNavigateToGoalDetail: (String) -> Unit,
    onNavigateToNewGoal: () -> Unit,
    onNavigateToTransfer: (String?, String?) -> Unit,
    onNavigateToWithdrawal: () -> Unit,
    onNavigateToStandingSplit: () -> Unit = {},
    onNavigateToAssignCredit: (Long) -> Unit = {}
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Goals,
        BottomNavItem.History,
        BottomNavItem.Ask
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyPrimary,
                            selectedTextColor = NavyPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = NavyPrimary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Goals.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Goals.route) {
                GoalsHomeScreen(
                    onSettingsClick = onNavigateToSettings,
                    onGoalClick = onNavigateToGoalDetail,
                    onNewGoalClick = onNavigateToNewGoal,
                    onTransferClick = { onNavigateToTransfer(null, null) },
                    onSyncClick = { },
                    onHistoryClick = {
                        navController.navigate(BottomNavItem.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            composable(BottomNavItem.History.route) {
                HistoryScreen()
            }
            
            composable(BottomNavItem.Ask.route) {
                AskScreen(
                    onTransferConfirm = { fromId, toId ->
                        onNavigateToTransfer(fromId, toId)
                    }
                )
            }
        }
    }
}
