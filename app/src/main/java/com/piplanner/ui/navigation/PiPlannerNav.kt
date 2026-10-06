package com.piplanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.piplanner.ui.goals.GoalsTabPlaceholder
import com.piplanner.ui.setup.AccountsScreen
import com.piplanner.ui.setup.AccountsViewModel
import com.piplanner.ui.setup.ConsentPlaceholder
import com.piplanner.ui.setup.OpeningSplitScreen
import com.piplanner.ui.setup.OpeningSplitViewModel
import com.piplanner.util.DemoData

object PiPlannerRoutes {
    const val ACCOUNTS: String = "accounts"
    const val CONSENT: String = "consent"
    const val OPENING_SPLIT: String = "opening_split"
    const val GOALS_TAB: String = "goals_tab"
}

/**
 * App navigation host.
 * Setup order: Accounts (PIP-38) → Consent placeholder (PIP-40) …
 * Opening split (PIP-44) remains registered so that flow stays intact.
 */
@Composable
fun PiPlannerNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val demoAccounts = remember { DemoData.sampleAccounts() }
    val demoGoals = remember { DemoData.sampleOpeningSplitGoals() }
    val openingBalance = DemoData.SAVINGS_OPENING_BALANCE_PAISA

    NavHost(
        navController = navController,
        startDestination = PiPlannerRoutes.ACCOUNTS,
    ) {
        composable(PiPlannerRoutes.ACCOUNTS) {
            val viewModel: AccountsViewModel = hiltViewModel()
            AccountsScreen(
                viewModel = viewModel,
                accounts = demoAccounts,
                onNavigateToConsent = {
                    navController.navigate(PiPlannerRoutes.CONSENT)
                },
            )
        }
        composable(PiPlannerRoutes.CONSENT) {
            ConsentPlaceholder()
        }
        composable(PiPlannerRoutes.OPENING_SPLIT) {
            val viewModel: OpeningSplitViewModel = hiltViewModel()
            OpeningSplitScreen(
                viewModel = viewModel,
                goals = demoGoals,
                openingBalance = openingBalance,
                onNavigateToGoals = {
                    navController.navigate(PiPlannerRoutes.GOALS_TAB) {
                        popUpTo(PiPlannerRoutes.OPENING_SPLIT) { inclusive = true }
                    }
                },
            )
        }
        composable(PiPlannerRoutes.GOALS_TAB) {
            GoalsTabPlaceholder()
        }
    }
}
