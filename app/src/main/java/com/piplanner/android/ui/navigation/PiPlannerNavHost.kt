package com.piplanner.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.ui.screens.ask.AskScreen
import com.piplanner.android.ui.screens.balance.BalanceAutoScreen
import com.piplanner.android.ui.screens.balance.BalanceFetchedScreen
import com.piplanner.android.ui.screens.balance.ManualBalanceScreen
import com.piplanner.android.ui.screens.balance.UpdateBalanceSheet
import com.piplanner.android.ui.screens.balance.UpiPinScreen
import com.piplanner.android.ui.screens.consent.ConsentScreen
import com.piplanner.android.ui.screens.goals.GoalChatScreen
import com.piplanner.android.ui.screens.goals.GoalDetailScreen
import com.piplanner.android.ui.screens.goals.GoalFormScreen
import com.piplanner.android.ui.screens.goals.GoalsHomeScreen
import com.piplanner.android.ui.screens.goals.OpeningSplitScreen
import com.piplanner.android.ui.screens.history.HistoryScreen
import com.piplanner.android.ui.screens.setup.AccountsScreen
import com.piplanner.android.ui.screens.setup.WelcomeScreen
import com.piplanner.android.ui.screens.settings.SettingsScreen
import com.piplanner.android.ui.screens.transfer.TransferScreen
import com.piplanner.android.ui.screens.withdrawal.WithdrawalScreen
import com.piplanner.android.ui.screens.assign.AssignCreditScreen
import com.piplanner.android.ui.screens.goals.StandingSplitSheet

@Composable
fun PiPlannerNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Welcome.route) {
            WelcomeScreen(
                onSetupClick = {
                    navController.navigate(NavRoutes.Accounts.route)
                }
            )
        }
        
        composable(NavRoutes.Accounts.route) {
            AccountsScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(NavRoutes.Consent.route)
                }
            )
        }
        
        composable(NavRoutes.Consent.route) {
            ConsentScreen(
                onBack = { navController.popBackStack() },
                onYesAuto = {
                    navController.navigate(NavRoutes.BalanceAuto.route)
                },
                onNoManual = {
                    navController.navigate(NavRoutes.UpdateBalance.route)
                }
            )
        }
        
        composable(NavRoutes.BalanceAuto.route) {
            BalanceAutoScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(NavRoutes.GoalChat.route) {
                        popUpTo(NavRoutes.Welcome.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(NavRoutes.UpdateBalance.route) {
            UpdateBalanceSheet(
                onDismiss = { navController.popBackStack() },
                onManually = {
                    navController.navigate(NavRoutes.ManualBalance.route)
                },
                onBalanceSync = {
                    navController.navigate(NavRoutes.UpiPin.route)
                }
            )
        }
        
        composable(NavRoutes.ManualBalance.route) {
            ManualBalanceScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(NavRoutes.GoalChat.route) {
                        popUpTo(NavRoutes.Welcome.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(NavRoutes.UpiPin.route) {
            UpiPinScreen(
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(NavRoutes.BalanceFetched.route)
                }
            )
        }
        
        composable(NavRoutes.BalanceFetched.route) {
            BalanceFetchedScreen(
                onContinue = {
                    navController.navigate(NavRoutes.GoalChat.route) {
                        popUpTo(NavRoutes.Welcome.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(NavRoutes.GoalChat.route) {
            GoalChatScreen(
                onBack = { navController.popBackStack() },
                onUseForm = {
                    navController.navigate(NavRoutes.GoalForm.createRoute())
                },
                onGoalsConfirmed = {
                    navController.navigate(NavRoutes.OpeningSplit.route)
                }
            )
        }
        
        composable(
            route = NavRoutes.GoalForm.route,
            arguments = listOf(
                navArgument("goalId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId")
            GoalFormScreen(
                goalId = goalId,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.OpeningSplit.route) {
            OpeningSplitScreen(
                onBack = { navController.popBackStack() },
                onLockSplit = {
                    navController.navigate(NavRoutes.MainShell.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavRoutes.MainShell.route) {
            MainShellScreen(navController)
        }
        
        composable(NavRoutes.GoalsTab.route) {
            GoalsHomeScreen(
                onSettingsClick = {
                    navController.navigate(NavRoutes.Settings.route)
                },
                onGoalClick = { goalId ->
                    navController.navigate(NavRoutes.GoalDetail.createRoute(goalId))
                },
                onNewGoalClick = {
                    navController.navigate(NavRoutes.GoalForm.createRoute())
                },
                onTransferClick = {
                    navController.navigate(NavRoutes.Transfer.createRoute())
                },
                onSyncClick = {
                    navController.navigate(NavRoutes.SyncBalance.route)
                }
            )
        }
        
        composable(NavRoutes.HistoryTab.route) {
            HistoryScreen()
        }
        
        composable(NavRoutes.AskTab.route) {
            AskScreen(
                onTransferConfirm = { fromId, toId ->
                    navController.navigate(NavRoutes.Transfer.createRoute(fromId, toId))
                }
            )
        }
        
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetDemo = {
                    navController.navigate(NavRoutes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = NavRoutes.GoalDetail.route,
            arguments = listOf(
                navArgument("goalId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
            GoalDetailScreen(
                goalId = goalId,
                onBack = { navController.popBackStack() },
                onEdit = {
                    navController.navigate(NavRoutes.GoalForm.createRoute(goalId))
                },
                onDelete = {
                    navController.navigate(NavRoutes.DeleteGoal.createRoute(goalId))
                },
                onTransfer = {
                    navController.navigate(NavRoutes.Transfer.createRoute(fromGoalId = goalId))
                }
            )
        }
        
        composable(
            route = NavRoutes.Transfer.route,
            arguments = listOf(
                navArgument("fromGoalId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("toGoalId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val fromGoalId = backStackEntry.arguments?.getString("fromGoalId")
            val toGoalId = backStackEntry.arguments?.getString("toGoalId")
            TransferScreen(
                preselectedFromGoalId = fromGoalId,
                preselectedToGoalId = toGoalId,
                onBack = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.Withdrawal.route) {
            WithdrawalScreen(
                onBack = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.SyncBalance.route) {
            SyncBalanceSheet(
                onDismiss = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.StandingSplit.route) {
            StandingSplitSheet(
                onDismiss = { navController.popBackStack() },
                onSave = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "assign_credit/{amount}",
            arguments = listOf(
                navArgument("amount") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val amount = backStackEntry.arguments?.getLong("amount") ?: 0L
            AssignCreditScreen(
                newAmount = amount,
                onBack = { navController.popBackStack() },
                onSaveAndLock = { navController.popBackStack() },
                onNewGoal = { navController.navigate(NavRoutes.GoalForm.createRoute()) }
            )
        }
        
        composable(
            route = NavRoutes.DeleteGoal.route,
            arguments = listOf(
                navArgument("goalId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId") ?: ""
            DeleteGoalSheet(
                goalId = goalId,
                onDismiss = { navController.popBackStack() },
                onConfirm = {
                    navController.popBackStack()
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun MainShellScreen(navController: NavHostController) {
    com.piplanner.android.ui.screens.MainShell(
        onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
        onNavigateToGoalDetail = { goalId -> navController.navigate(NavRoutes.GoalDetail.createRoute(goalId)) },
        onNavigateToNewGoal = { navController.navigate(NavRoutes.GoalForm.createRoute()) },
        onNavigateToTransfer = { fromId, toId -> navController.navigate(NavRoutes.Transfer.createRoute(fromId, toId)) },
        onNavigateToWithdrawal = { navController.navigate(NavRoutes.Withdrawal.route) },
        onNavigateToStandingSplit = { navController.navigate(NavRoutes.StandingSplit.route) },
        onNavigateToAssignCredit = { amount -> navController.navigate("assign_credit/$amount") }
    )
}

@Composable
private fun SyncBalanceSheet(onDismiss: () -> Unit) {
    com.piplanner.android.ui.screens.balance.SyncBalanceSheet(
        onDismiss = onDismiss,
        onBalanceUpdated = onDismiss
    )
}

@Composable
private fun DeleteGoalSheet(
    goalId: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    com.piplanner.android.ui.screens.goals.DeleteGoalSheet(
        goalId = goalId,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
