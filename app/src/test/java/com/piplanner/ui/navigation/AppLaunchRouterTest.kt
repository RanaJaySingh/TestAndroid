package com.piplanner.ui.navigation

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.AppState
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import org.junit.Test

class AppLaunchRouterTest {

    @Test
    fun emptyState_isFirstRunOrPostReset_andRoutesToWelcome() {
        val state = AppState.EMPTY
        assertThat(AppLaunchRouter.isFirstRunOrPostReset(state)).isTrue()
        assertThat(AppLaunchRouter.destination(state)).isEqualTo(AppLaunchDestination.Welcome)
        assertThat(AppLaunchRouter.startRoute(state)).isEqualTo(PiPlannerRoutes.WELCOME)
    }

    @Test
    fun postReset_clearedGoalsAndHistory_routesToWelcome() {
        val state = AppState(
            accounts = emptyList(),
            goals = emptyList(),
            history = emptyList(),
            standingSplits = emptyList(),
            hasCompletedSetup = false,
        )
        assertThat(AppLaunchRouter.isFirstRunOrPostReset(state)).isTrue()
        assertThat(AppLaunchRouter.destination(state)).isEqualTo(AppLaunchDestination.Welcome)
    }

    @Test
    fun hasCompletedSetupFlag_routesToGoals() {
        val state = AppState.EMPTY.copy(hasCompletedSetup = true)
        assertThat(AppLaunchRouter.hasCompletedSetup(state)).isTrue()
        assertThat(AppLaunchRouter.destination(state)).isEqualTo(AppLaunchDestination.Goals)
        assertThat(AppLaunchRouter.startRoute(state)).isEqualTo(PiPlannerRoutes.GOALS_TAB)
    }

    @Test
    fun lockedOpeningBalance_routesToGoals() {
        val entry = HistoryEntry(
            id = "h1",
            type = HistoryEntryType.OpeningBalance,
            createdAt = "2026-01-01T00:00:00Z",
            isLocked = true,
            creditAmount = 10_000_000L,
            newBalance = 10_000_000L,
        )
        val state = AppState(history = listOf(entry))
        assertThat(AppLaunchRouter.isFirstRunOrPostReset(state)).isFalse()
        assertThat(AppLaunchRouter.hasCompletedSetup(state)).isTrue()
        assertThat(AppLaunchRouter.destination(state)).isEqualTo(AppLaunchDestination.Goals)
    }

    @Test
    fun unlockedOpening_doesNotCompleteSetup() {
        val entry = HistoryEntry(
            id = "h1",
            type = HistoryEntryType.OpeningBalance,
            createdAt = "2026-01-01T00:00:00Z",
            isLocked = false,
            creditAmount = 10_000_000L,
        )
        val state = AppState(history = listOf(entry))
        assertThat(AppLaunchRouter.destination(state)).isEqualTo(AppLaunchDestination.Welcome)
    }
}
