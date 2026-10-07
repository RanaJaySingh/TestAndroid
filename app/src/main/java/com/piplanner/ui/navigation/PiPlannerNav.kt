package com.piplanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.piplanner.ui.setup.ConsentSheet
import com.piplanner.ui.setup.ConsentViewModel
import com.piplanner.ui.setup.FetchedBalanceScreen
import com.piplanner.ui.setup.ManualBalanceScreen
import com.piplanner.ui.setup.OpeningSplitScreen
import com.piplanner.ui.setup.OpeningSplitViewModel
import com.piplanner.ui.setup.OtherAppScreen
import com.piplanner.ui.setup.UPIPinScreen
import com.piplanner.ui.setup.UpdateBalanceSheet
import com.piplanner.ui.setup.WelcomeScreen
import com.piplanner.ui.setup.WelcomeViewModel
import com.piplanner.ui.setup.WrongPinScreen
import com.piplanner.util.DemoData

object PiPlannerRoutes {
    const val WELCOME: String = "welcome"
    const val ACCOUNTS: String = "accounts"
    const val CONSENT: String = "consent"
    const val FETCHED_BALANCE: String = "fetched_balance"
    const val UPDATE_BALANCE: String = "update_balance"
    const val MANUAL_BALANCE: String = "manual_balance"
    const val UPI_PIN: String = "upi_pin"
    const val OTHER_APP: String = "other_app"
    const val WRONG_PIN: String = "wrong_pin"
    const val OPENING_SPLIT: String = "opening_split"
    const val GOALS_TAB: String = "goals_tab"
}

/**
 * App navigation host.
 * First-run / post–Reset → Welcome (PIP-36); CTA → Accounts (PIP-38);
 * Consent + balance entry (PIP-40) → Opening split (PIP-44) with resolved paisa;
 * setup complete → Goals tab.
 */
@Composable
fun PiPlannerNavHost(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val demoAccounts = remember { DemoData.sampleAccounts() }
    val demoGoals = remember { DemoData.sampleOpeningSplitGoals() }
    var resolvedOpeningBalance by remember {
        mutableLongStateOf(DemoData.SAVINGS_OPENING_BALANCE_PAISA)
    }

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
            val viewModel: ConsentViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                viewModel.loadAccounts()
            }
            ConsentSheet(
                viewModel = viewModel,
                onYesFetched = {
                    navController.navigate(PiPlannerRoutes.FETCHED_BALANCE)
                },
                onNo = {
                    navController.navigate(PiPlannerRoutes.UPDATE_BALANCE)
                },
            )
        }
        composable(PiPlannerRoutes.FETCHED_BALANCE) { backStackEntry ->
            val consentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(PiPlannerRoutes.CONSENT)
            }
            val viewModel: ConsentViewModel = hiltViewModel(consentEntry)
            FetchedBalanceScreen(
                viewModel = viewModel,
                onContinue = {
                    resolvedOpeningBalance =
                        viewModel.uiState.value.resolvedBalancePaisa
                            ?: DemoData.SAVINGS_OPENING_BALANCE_PAISA
                    navController.navigate(PiPlannerRoutes.OPENING_SPLIT)
                },
            )
        }
        composable(PiPlannerRoutes.UPDATE_BALANCE) {
            UpdateBalanceSheet(
                onManually = {
                    navController.navigate(PiPlannerRoutes.MANUAL_BALANCE)
                },
                onBalanceSync = {
                    navController.navigate(PiPlannerRoutes.UPI_PIN)
                },
            )
        }
        composable(PiPlannerRoutes.MANUAL_BALANCE) { backStackEntry ->
            val consentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(PiPlannerRoutes.CONSENT)
            }
            val viewModel: ConsentViewModel = hiltViewModel(consentEntry)
            ManualBalanceScreen(
                viewModel = viewModel,
                onContinue = {
                    resolvedOpeningBalance =
                        viewModel.uiState.value.resolvedBalancePaisa
                            ?: DemoData.SAVINGS_OPENING_BALANCE_PAISA
                    navController.navigate(PiPlannerRoutes.OPENING_SPLIT)
                },
            )
        }
        composable(PiPlannerRoutes.UPI_PIN) { backStackEntry ->
            val consentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(PiPlannerRoutes.CONSENT)
            }
            val viewModel: ConsentViewModel = hiltViewModel(consentEntry)
            LaunchedEffect(Unit) {
                viewModel.clearPin()
            }
            UPIPinScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.navigate(PiPlannerRoutes.FETCHED_BALANCE)
                },
                onWrongPin = {
                    navController.navigate(PiPlannerRoutes.WRONG_PIN)
                },
                onCancel = {
                    navController.navigate(PiPlannerRoutes.UPDATE_BALANCE) {
                        popUpTo(PiPlannerRoutes.CONSENT)
                    }
                },
                onOtherApp = {
                    navController.navigate(PiPlannerRoutes.OTHER_APP)
                },
            )
        }
        composable(PiPlannerRoutes.OTHER_APP) {
            OtherAppScreen(
                onContinueManual = {
                    navController.navigate(PiPlannerRoutes.MANUAL_BALANCE)
                },
            )
        }
        composable(PiPlannerRoutes.WRONG_PIN) { backStackEntry ->
            val consentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(PiPlannerRoutes.CONSENT)
            }
            val viewModel: ConsentViewModel = hiltViewModel(consentEntry)
            WrongPinScreen(
                viewModel = viewModel,
                onRetry = {
                    navController.navigate(PiPlannerRoutes.UPI_PIN) {
                        popUpTo(PiPlannerRoutes.WRONG_PIN) { inclusive = true }
                    }
                },
                onManual = {
                    navController.navigate(PiPlannerRoutes.MANUAL_BALANCE)
                },
            )
        }
        composable(PiPlannerRoutes.OPENING_SPLIT) {
            val viewModel: OpeningSplitViewModel = hiltViewModel()
            OpeningSplitScreen(
                viewModel = viewModel,
                goals = demoGoals,
                openingBalance = resolvedOpeningBalance,
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
