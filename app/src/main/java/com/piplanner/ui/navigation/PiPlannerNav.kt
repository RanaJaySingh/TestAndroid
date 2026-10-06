package com.piplanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.piplanner.ui.AppViewModel
import com.piplanner.ui.goals.GoalsTabPlaceholder
import com.piplanner.ui.setup.AccountsScreen
import com.piplanner.ui.setup.AccountsViewModel
import com.piplanner.ui.setup.ConsentPlaceholder
import com.piplanner.ui.setup.OpeningSplitScreen
import com.piplanner.ui.setup.OpeningSplitViewModel
import com.piplanner.ui.setup.WelcomeScreen
import com.piplanner.ui.setup.WelcomeViewModel
import com.piplanner.util.DemoData

object PiPlannerRoutes {
    const val WELCOME: String = "welcome"
    const val ACCOUNTS: String = "accounts"
    const val CONSENT: String = "consent"
    const val OPENING_SPLIT: String = "opening_split"
    const val GOALS_TAB: String = "goals_tab"
}

/**
 * App navigation host.
 * First-run / post–Reset → Welcome (PIP-36); CTA → Accounts (PIP-38);
 * setup complete → Goals tab. Opening split (PIP-44) remains registered.
 */
@Composable
fun PiPlannerNavHost(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val demoAccounts = remember { DemoData.sampleAccounts() }
    val demoGoals = remember { DemoData.sampleOpeningSplitGoals() }
    val openingBalance = DemoData.SAVINGS_OPENING_BALANCE_PAISA

    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination = appViewModel.resolveStartRoute()
    }

    val resolvedStart = startDestination ?: return

    NavHost(
        navController = navController,
        startDestination = resolvedStart,
    ) {
        composable(PiPlannerRoutes.WELCOME) {
            val viewModel: WelcomeViewModel = hiltViewModel()
            WelcomeScreen(
                viewModel = viewModel,
                onNavigateToAccounts = {
                    navController.navigate(PiPlannerRoutes.ACCOUNTS)
                },
            )
        }
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
