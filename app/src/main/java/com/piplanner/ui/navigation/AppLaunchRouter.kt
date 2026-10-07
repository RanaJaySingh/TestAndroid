package com.piplanner.ui.navigation

import com.piplanner.data.model.AppState
import com.piplanner.data.model.HistoryEntryType

/**
 * Root destination after load / Reset demo (PRD R1 / R17).
 * Mirrors iOS [AppLaunchRouter] for cross-platform parity.
 */
enum class AppLaunchDestination {
    /** First-run or post–Reset demo — Welcome (frame 1). */
    Welcome,

    /** Setup completed (locked Opening balance) — Goals tab (or placeholder). */
    Goals,
}

/**
 * Pure routing for app launch. Testable without Compose.
 */
object AppLaunchRouter {

    fun destination(forState: AppState): AppLaunchDestination {
        return if (hasCompletedSetup(forState)) {
            AppLaunchDestination.Goals
        } else {
            AppLaunchDestination.Welcome
        }
    }

    fun startRoute(forState: AppState): String {
        return when (destination(forState)) {
            AppLaunchDestination.Welcome -> PiPlannerRoutes.WELCOME
            AppLaunchDestination.Goals -> PiPlannerRoutes.GOALS_TAB
        }
    }

    /**
     * Setup is complete when [AppState.hasCompletedSetup] is true, or a locked
     * Opening balance History entry exists (R6) — same signals Opening split writes.
     */
    fun hasCompletedSetup(state: AppState): Boolean {
        if (state.hasCompletedSetup) return true
        return state.history.any {
            it.type == HistoryEntryType.OpeningBalance && it.isLocked
        }
    }

    /** Explicit first-run / post-reset check used by tests and Reset wiring. */
    fun isFirstRunOrPostReset(state: AppState): Boolean {
        return state.goals.isEmpty() && state.history.isEmpty() && !state.hasCompletedSetup
    }
}
