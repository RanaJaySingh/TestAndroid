package com.piplanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.piplanner.ui.goals.GoalsTabPlaceholder
import com.piplanner.ui.setup.OpeningSplitScreen
import com.piplanner.ui.setup.OpeningSplitViewModel
import com.piplanner.util.DemoData

object PiPlannerRoutes {
    const val OPENING_SPLIT: String = "opening_split"
    const val GOALS_TAB: String = "goals_tab"
}

/**
 * App navigation host. Until earlier setup screens land, Opening split is the entry for PIP-44.
 */
@Composable
fun PiPlannerNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val demoGoals = remember { DemoData.sampleOpeningSplitGoals() }
    val openingBalance = DemoData.SAVINGS_OPENING_BALANCE_PAISA

    NavHost(
        navController = navController,
        startDestination = PiPlannerRoutes.OPENING_SPLIT,
    ) {
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
