package com.piplanner.domain

import com.piplanner.data.model.Account
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalStatus
import javax.inject.Inject
import javax.inject.Singleton

/** Balance-action CTA on the Goals tab balance card (frames 9 / 9b / 9c). */
enum class GoalsBalanceAction {
    /** Consent On — Sync button (frame 9b). */
    Sync,

    /** Consent Off — Update balance (frame 9c / 11). */
    UpdateBalance,
}

/**
 * Pure Goals-tab presentation helpers (PIP-46 / Spec §5.2 GoalsTab).
 * Mirrors iOS GoalsTabService (PIP-45).
 */
@Singleton
class GoalsTabService @Inject constructor(
    private val dedicatedAccountService: DedicatedAccountService,
) {

    /** Status copy matching design frames 9 / 11. */
    fun statusLabel(status: GoalStatus): String = when (status) {
        is GoalStatus.OnTrack -> STATUS_ON_TRACK
        is GoalStatus.Behind -> STATUS_BEHIND
    }

    /**
     * Total savings shown on the balance card.
     * Prefers dedicated account balance (PRD R21); falls back to sum of goal `savedAmount`.
     */
    fun totalSavingsPaisa(accounts: List<Account>, goals: List<Goal>): Long {
        val dedicated = dedicatedAccountService.dedicatedAccount(accounts)
        if (dedicated != null) {
            return dedicated.balance
        }
        return goals.sumOf { it.savedAmount }
    }

    /** Consent from the dedicated account drives Sync vs Update balance. */
    fun balanceAction(accounts: List<Account>): GoalsBalanceAction {
        val dedicated = dedicatedAccountService.dedicatedAccount(accounts)
        return if (dedicated != null && dedicated.consentAutoUpdate) {
            GoalsBalanceAction.Sync
        } else {
            GoalsBalanceAction.UpdateBalance
        }
    }

    fun balanceActionTitle(action: GoalsBalanceAction): String = when (action) {
        GoalsBalanceAction.Sync -> ACTION_SYNC
        GoalsBalanceAction.UpdateBalance -> ACTION_UPDATE_BALANCE
    }

    /** Account subtitle for the balance card (bank + masked number). */
    fun dedicatedAccountSubtitle(accounts: List<Account>): String? {
        val dedicated = dedicatedAccountService.dedicatedAccount(accounts) ?: return null
        return displayTitle(dedicated)
    }

    fun hasGoals(goals: List<Goal>): Boolean = goals.isNotEmpty()

    fun displayTitle(account: Account): String = "${account.bankName} ${account.maskedNumber}"

    companion object {
        /** Tab bar labels — Spec §5.3 (no Settings tab). */
        val TAB_TITLES: List<String> = listOf("Goals", "History", "Ask")

        const val STATUS_ON_TRACK: String = "On track"
        const val STATUS_BEHIND: String = "Behind"
        const val ACTION_SYNC: String = "Sync"
        const val ACTION_UPDATE_BALANCE: String = "Update balance"
    }
}
