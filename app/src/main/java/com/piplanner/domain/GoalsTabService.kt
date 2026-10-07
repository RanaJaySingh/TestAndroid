package com.piplanner.domain

import com.piplanner.data.model.Account
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalStatus
import com.piplanner.data.model.HistoryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/** Balance-action CTA on the Goals tab balance card (frames 9 / 9b / 9c). */
enum class GoalsBalanceAction {
    /** Consent On — Sync button (frame 9b). */
    Sync,

    /** Consent Off — Update balance (frame 9c / 11). */
    UpdateBalance,
}

/**
 * Pure Goals-tab presentation helpers (PIP-46 / PIP-82 / Spec §5.2 GoalsTab).
 * Mirrors iOS GoalsTabService (PIP-45 / PIP-81).
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

        /** Quick-action labels in design order (frame 9). */
        val QUICK_ACTION_TITLES: List<String> = listOf("Sync", "New goal", "Transfer", "History")

        const val STATUS_ON_TRACK: String = "On track"
        const val STATUS_BEHIND: String = "Behind"
        const val ACTION_SYNC: String = "Sync"
        const val ACTION_UPDATE_BALANCE: String = "Update balance"
        const val ACTION_UPDATE_SHORT: String = "Update"
        const val QUICK_NEW_GOAL: String = "New goal"
        const val QUICK_TRANSFER: String = "Transfer"
        const val QUICK_HISTORY: String = "History"

        private val EN_IN: Locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        private val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm a", EN_IN)
        private val DAY_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMM", EN_IN)

        /** Quick-action compact label — "Sync" or "Update" (Tech Spec §3.5 QuickActionRow). */
        fun quickBalanceActionTitle(action: GoalsBalanceAction): String = when (action) {
            GoalsBalanceAction.Sync -> ACTION_SYNC
            GoalsBalanceAction.UpdateBalance -> ACTION_UPDATE_SHORT
        }

        /** Newest History `createdAt` as last balance activity (presentation only). */
        fun lastBalanceActivityInstant(history: List<HistoryEntry>): Instant? {
            return history.mapNotNull { entry ->
                runCatching { Instant.parse(entry.createdAt) }.getOrNull()
            }.maxOrNull()
        }

        /**
         * Navy card footer — "Last synced today, 7:42 pm" / "Last updated …" (R10 / frames 9 / 9c).
         */
        fun lastBalanceActivityLine(
            action: GoalsBalanceAction,
            reference: Instant?,
            now: Instant = Instant.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String {
            val verb = when (action) {
                GoalsBalanceAction.Sync -> "Last synced"
                GoalsBalanceAction.UpdateBalance -> "Last updated"
            }
            if (reference == null) {
                return "$verb —"
            }
            return "$verb ${relativeDayTimeLabel(reference, now, zoneId)}"
        }

        /** Goal card primary amount line — "₹60,000 of ₹13,10,796" (frame 9 / 11). */
        fun savedOfTargetLabel(
            savedPaisa: Long,
            targetPaisa: Long,
            formatting: FormattingService,
        ): String {
            val saved = formatting.formatInrFromPaisa(savedPaisa)
            val target = formatting.formatInrFromPaisa(targetPaisa)
            return "$saved of $target"
        }

        /** Goal card monthly need — "Needs ₹26,058 a month". */
        fun monthlyNeedLabel(
            monthlyNeedPaisa: Long,
            formatting: FormattingService,
        ): String = "Needs ${formatting.formatInrFromPaisa(monthlyNeedPaisa)} a month"

        /** Goal card credit share — "60% of credits". */
        fun creditsPercentLabel(shareOfNewCredits: Double): String {
            val percent = round(shareOfNewCredits * 100.0).toInt()
            return "$percent% of credits"
        }

        /** "today, 7:42 pm" / "yesterday, 7:42 pm" / "5 Oct, 7:42 pm". */
        private fun relativeDayTimeLabel(
            reference: Instant,
            now: Instant,
            zoneId: ZoneId,
        ): String {
            val refDate = LocalDate.ofInstant(reference, zoneId)
            val today = LocalDate.ofInstant(now, zoneId)
            val time = TIME_FORMATTER.format(reference.atZone(zoneId))
                .replace("AM", "am")
                .replace("PM", "pm")
            return when (refDate) {
                today -> "today, $time"
                today.minusDays(1) -> "yesterday, $time"
                else -> "${DAY_FORMATTER.format(refDate)}, $time"
            }
        }
    }
}
