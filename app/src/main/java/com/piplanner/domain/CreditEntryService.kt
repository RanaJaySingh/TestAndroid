package com.piplanner.domain

import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalAllocation
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of comparing previous vs fetched/typed balance (frames 10 / 10a / 10b). */
sealed class BalanceCompareResult {
    data class Higher(
        val creditAmount: Long,
        val previousBalance: Long,
        val newBalance: Long,
    ) : BalanceCompareResult()

    data object Same : BalanceCompareResult()

    data class Lower(
        val shortfall: Long,
        val previousBalance: Long,
        val newBalance: Long,
    ) : BalanceCompareResult()
}

/** Result of Sync / typed Update processing. */
sealed class CreditProcessOutcome {
    data class NoNewCredit(val message: String) : CreditProcessOutcome()

    data class WithdrawalRequired(
        val shortfall: Long,
        val previousBalance: Long,
        val newBalance: Long,
    ) : CreditProcessOutcome()

    data class OpenCreditCreated(
        val state: AppState,
        val entry: HistoryEntry,
    ) : CreditProcessOutcome()
}

class CreditEntryException(
    val field: String,
    override val message: String,
) : Exception(message)

/**
 * Pure Sync / Update / Credit-entry state machine (PRD R7–R9, R26; Spec BR-5, BR-6).
 * Reuses [OpeningSplitService] for 100% validation and [StandingSplitService] for next-credit defaults.
 */
@Singleton
class CreditEntryService @Inject constructor(
    private val openingSplitService: OpeningSplitService,
    private val standingSplitService: StandingSplitService,
) {

    fun compare(previousBalance: Long, newBalance: Long): BalanceCompareResult {
        return when {
            newBalance > previousBalance -> BalanceCompareResult.Higher(
                creditAmount = newBalance - previousBalance,
                previousBalance = previousBalance,
                newBalance = newBalance,
            )
            newBalance == previousBalance -> BalanceCompareResult.Same
            else -> BalanceCompareResult.Lower(
                shortfall = previousBalance - newBalance,
                previousBalance = previousBalance,
                newBalance = newBalance,
            )
        }
    }

    /** Unsaved New credit entry, if any (newest first). */
    fun openCreditEntry(history: List<HistoryEntry>): HistoryEntry? {
        return history
            .filter { it.type == HistoryEntryType.NewCredit && !it.isLocked }
            .maxByOrNull { it.createdAt }
    }

    /** BR-6: Sync / Update blocked while an open credit entry exists. */
    fun isSyncOrUpdateBlocked(history: List<HistoryEntry>): Boolean =
        openCreditEntry(history) != null

    fun openEntryBannerMessage(creditAmount: Long, formatting: FormattingService): String {
        val amount = formatting.formatInrFromPaisa(creditAmount)
        return "$OPEN_ENTRY_BANNER_PREFIX $amount. $ASSIGN_NOW_TITLE"
    }

    fun openEntryBannerMessage(entry: HistoryEntry, formatting: FormattingService): String =
        openEntryBannerMessage(entry.creditAmount ?: 0L, formatting)

    /**
     * Defaults for this-credit-only split from standing splits / goal shares.
     * Single goal → 100% (frame 13e). Delegates to [StandingSplitService.percentagesForNextCredit].
     */
    fun defaultPercentages(
        goals: List<Goal>,
        standingSplits: List<StandingSplit>,
    ): Map<String, BigDecimal> {
        val fromStanding = standingSplitService.percentagesForNextCredit(goals, standingSplits)
        if (goals.size <= 1) return fromStanding
        val ordered = goals.map { fromStanding[it.id] ?: BigDecimal.ZERO }
        if (openingSplitService.isValidHundredPercent(ordered)) {
            return fromStanding
        }
        return equalPercentages(goals)
    }

    fun equalPercentages(goals: List<Goal>): Map<String, BigDecimal> {
        if (goals.isEmpty()) return emptyMap()
        val count = BigDecimal.valueOf(goals.size.toLong())
        val base = BigDecimal.ONE.divide(count, 4, RoundingMode.HALF_UP)
        var assigned = BigDecimal.ZERO
        val result = LinkedHashMap<String, BigDecimal>()
        goals.forEachIndexed { index, goal ->
            if (index == goals.lastIndex) {
                result[goal.id] = BigDecimal.ONE.subtract(assigned).setScale(4, RoundingMode.HALF_UP)
            } else {
                result[goal.id] = base
                assigned = assigned.add(base)
            }
        }
        return result
    }

    fun createOpenCreditEntry(
        goals: List<Goal>,
        standingSplits: List<StandingSplit>,
        previousBalance: Long,
        newBalance: Long,
        creditAmount: Long,
        isTyped: Boolean,
        id: String = UUID.randomUUID().toString(),
        createdAt: String,
    ): HistoryEntry {
        if (creditAmount <= 0L || newBalance != previousBalance + creditAmount) {
            throw CreditEntryException(
                field = "creditAmount",
                message = "Credit amount must equal the increase in balance.",
            )
        }
        if (goals.isEmpty()) {
            throw CreditEntryException(
                field = "goals",
                message = "At least one goal is required to assign a credit.",
            )
        }

        val percentages = defaultPercentages(goals, standingSplits)
        val allocations = openingSplitService.makeAllocations(
            goals = goals,
            openingBalance = creditAmount,
            percentages = percentages,
        )

        return HistoryEntry(
            id = id,
            type = HistoryEntryType.NewCredit,
            createdAt = createdAt,
            isLocked = false,
            previousBalance = previousBalance,
            newBalance = newBalance,
            creditAmount = creditAmount,
            isTyped = isTyped,
            allocations = allocations,
        )
    }

    /**
     * Applies an open credit: updates dedicated balance, appends unlocked History entry.
     * Rejects when another open credit already exists (BR-6).
     */
    fun applyOpenCredit(
        state: AppState,
        entry: HistoryEntry,
        dedicatedAccountId: String,
    ): AppState {
        if (entry.type != HistoryEntryType.NewCredit) {
            throw CreditEntryException(
                field = "type",
                message = "Expected a New credit History entry.",
            )
        }
        if (entry.isLocked) {
            throw CreditEntryException(
                field = "isLocked",
                message = "Open credit entry must start unlocked.",
            )
        }
        if (isSyncOrUpdateBlocked(state.history)) {
            throw CreditEntryException(
                field = "history",
                message = "An open credit must be assigned before Sync or Update.",
            )
        }
        val newBalance = entry.newBalance
            ?: throw CreditEntryException(
                field = "newBalance",
                message = "Credit entry requires a new balance.",
            )

        var foundDedicated = false
        val accounts = state.accounts.map { account ->
            if (account.id != dedicatedAccountId) {
                account
            } else {
                foundDedicated = true
                account.copy(balance = newBalance)
            }
        }
        if (!foundDedicated) {
            throw CreditEntryException(
                field = "accounts",
                message = "Dedicated savings account not found.",
            )
        }

        return state.copy(
            accounts = accounts,
            history = state.history + entry,
        )
    }

    fun allocationsForSave(
        goals: List<Goal>,
        creditAmount: Long,
        percentages: Map<String, BigDecimal>,
    ): List<GoalAllocation> {
        return openingSplitService.makeAllocations(
            goals = goals,
            openingBalance = creditAmount,
            percentages = percentages,
        )
    }

    fun canSaveAndLock(entry: HistoryEntry, fractions: List<BigDecimal>): Boolean {
        return !entry.isLocked &&
            entry.type == HistoryEntryType.NewCredit &&
            openingSplitService.isValidHundredPercent(fractions)
    }

    /**
     * Locks the open credit: updates goal saved amounts; optionally standing split (BR-5 / R8).
     */
    fun applyCreditLock(
        state: AppState,
        entryId: String,
        percentages: Map<String, BigDecimal>,
        useThisSplitForStanding: Boolean,
        nowIso: String,
    ): AppState {
        val index = state.history.indexOfFirst { it.id == entryId }
        if (index < 0) {
            throw CreditEntryException(
                field = "entryID",
                message = "Credit entry not found.",
            )
        }

        var entry = state.history[index]
        if (entry.type != HistoryEntryType.NewCredit) {
            throw CreditEntryException(
                field = "type",
                message = "Only New credit entries can be locked here.",
            )
        }
        if (entry.isLocked) {
            throw CreditEntryException(
                field = "isLocked",
                message = "This credit is already locked.",
            )
        }

        val creditAmount = entry.creditAmount ?: 0L
        if (creditAmount <= 0L) {
            throw CreditEntryException(
                field = "creditAmount",
                message = "Credit amount must be positive.",
            )
        }

        val allocations = allocationsForSave(
            goals = state.goals,
            creditAmount = creditAmount,
            percentages = percentages,
        )

        entry = entry.copy(
            allocations = allocations,
            isLocked = true,
        )

        val goalsById = state.goals.associateBy { it.id }.toMutableMap()
        for (allocation in allocations) {
            val goal = goalsById[allocation.goalId]
                ?: throw CreditEntryException(
                    field = "goalId",
                    message = "Allocation references unknown goal ${allocation.goalId}.",
                )
            goalsById[allocation.goalId] = goal.copy(
                savedAmount = goal.savedAmount + allocation.amount,
                shareOfNewCredits = if (useThisSplitForStanding) {
                    allocation.percentage
                } else {
                    goal.shareOfNewCredits
                },
                updatedAt = nowIso,
            )
        }

        return state.copy(
            goals = state.goals.map { goalsById[it.id] ?: it },
            history = state.history.toMutableList().also { it[index] = entry },
            standingSplits = if (useThisSplitForStanding) {
                allocations.map {
                    StandingSplit(goalId = it.goalId, percentage = it.percentage)
                }
            } else {
                state.standingSplits
            },
        )
    }

    /**
     * Full Sync / typed Update path after a successful fetch (frames 10 / 10a / 10b).
     */
    fun processFetchedBalance(
        state: AppState,
        fetchedBalance: Long,
        dedicatedAccountId: String,
        isTyped: Boolean,
        id: String = UUID.randomUUID().toString(),
        createdAt: String,
    ): CreditProcessOutcome {
        if (isSyncOrUpdateBlocked(state.history)) {
            throw CreditEntryException(
                field = "history",
                message = "An open credit must be assigned before Sync or Update.",
            )
        }

        val dedicated = state.accounts.firstOrNull {
            it.id == dedicatedAccountId || it.isDedicated
        } ?: throw CreditEntryException(
            field = "accounts",
            message = "Dedicated savings account not found.",
        )

        val previous = dedicated.balance
        return when (val result = compare(previousBalance = previous, newBalance = fetchedBalance)) {
            BalanceCompareResult.Same ->
                CreditProcessOutcome.NoNewCredit(message = NO_NEW_CREDIT_MESSAGE)

            is BalanceCompareResult.Lower ->
                CreditProcessOutcome.WithdrawalRequired(
                    shortfall = result.shortfall,
                    previousBalance = result.previousBalance,
                    newBalance = result.newBalance,
                )

            is BalanceCompareResult.Higher -> {
                val entry = createOpenCreditEntry(
                    goals = state.goals,
                    standingSplits = state.standingSplits,
                    previousBalance = result.previousBalance,
                    newBalance = result.newBalance,
                    creditAmount = result.creditAmount,
                    isTyped = isTyped,
                    id = id,
                    createdAt = createdAt,
                )
                val next = applyOpenCredit(
                    state = state,
                    entry = entry,
                    dedicatedAccountId = dedicatedAccountId,
                )
                CreditProcessOutcome.OpenCreditCreated(state = next, entry = entry)
            }
        }
    }

    companion object {
        const val NO_NEW_CREDIT_MESSAGE: String = "No new credit since the last sync."
        const val OPEN_ENTRY_BANNER_PREFIX: String = "New credit found"
        const val ASSIGN_NOW_TITLE: String = "Assign now"
        const val LOCKED_ONCE_CAPTION: String = "You can change this split once."
        const val USE_THIS_SPLIT_CHECKBOX_TITLE: String =
            "Use this split from the next credit too"
        const val ASSIGN_OPEN_BEFORE_SYNC: String = "Assign the open credit before Sync."
        const val ASSIGN_OPEN_BEFORE_UPDATE: String = "Assign the open credit before Update."
    }
}
