package com.piplanner.domain

import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalAllocation
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class WithdrawalException(
    val field: String,
    override val message: String,
) : Exception(message)

/**
 * Withdrawal allocation (frames 18 / 18a / 18b / 18c) — PRD R15, Spec BR-8.
 *
 * Default reductions are proportional to current [Goal.savedAmount] using largest-remainder
 * integer paisa math so totals match the shortfall exactly. No goal may go below ₹0.
 * Standing split is unchanged (point-in-time withdrawal).
 */
@Singleton
class WithdrawalService @Inject constructor(
    private val openingSplitService: OpeningSplitService,
) {

    fun shortfall(previousBalance: Long, newBalance: Long): Long {
        require(previousBalance >= newBalance) {
            "Withdrawal requires previousBalance >= newBalance"
        }
        return previousBalance - newBalance
    }

    /**
     * Default per-goal reductions proportional to current savings (BR-8).
     * Uses [OpeningSplitService.allocatePaisa] so sum(reductions) == [shortfall] exactly
     * when [shortfall] ≤ total saved. Goals with zero saved get zero reduction.
     */
    fun defaultReductions(goals: List<Goal>, shortfall: Long): Map<String, Long> {
        if (goals.isEmpty() || shortfall <= 0L) {
            return goals.associate { it.id to 0L }
        }
        val totalSaved = goals.sumOf { it.savedAmount.coerceAtLeast(0L) }
        if (totalSaved <= 0L) {
            return goals.associate { it.id to 0L }
        }
        val target = minOf(shortfall, totalSaved)
        val fractions = goals.map { goal ->
            val saved = goal.savedAmount.coerceAtLeast(0L)
            if (saved == 0L) {
                BigDecimal.ZERO
            } else {
                BigDecimal.valueOf(saved)
                    .divide(BigDecimal.valueOf(totalSaved), 16, RoundingMode.HALF_UP)
            }
        }
        val amounts = openingSplitService.allocatePaisa(target, fractions)
        return goals.mapIndexed { index, goal ->
            goal.id to amounts[index].coerceIn(0L, goal.savedAmount.coerceAtLeast(0L))
        }.toMap().let { capped ->
            // Cap should be a no-op when shortfall ≤ totalSaved; repair if remainder drift.
            repairToExactShortfall(goals, capped, target)
        }
    }

    /**
     * Ensures sum(reductions) == [shortfall] without sending any goal below ₹0.
     * Prefer increasing uncapped goals when under; decrease from largest when over.
     */
    fun repairToExactShortfall(
        goals: List<Goal>,
        reductions: Map<String, Long>,
        shortfall: Long,
    ): Map<String, Long> {
        if (goals.isEmpty()) return emptyMap()
        val result = goals.associate { goal ->
            val saved = goal.savedAmount.coerceAtLeast(0L)
            val raw = (reductions[goal.id] ?: 0L).coerceIn(0L, saved)
            goal.id to raw
        }.toMutableMap()

        var sum = result.values.sum()
        if (sum < shortfall) {
            val ordered = goals.sortedByDescending { it.savedAmount }
            var need = shortfall - sum
            var guard = 0
            while (need > 0 && guard < goals.size * 4) {
                var progressed = false
                for (goal in ordered) {
                    if (need <= 0) break
                    val saved = goal.savedAmount.coerceAtLeast(0L)
                    val current = result[goal.id] ?: 0L
                    if (current < saved) {
                        result[goal.id] = current + 1
                        need -= 1
                        progressed = true
                    }
                }
                if (!progressed) break
                guard += 1
            }
        } else if (sum > shortfall) {
            val ordered = goals.sortedByDescending { result[it.id] ?: 0L }
            var excess = sum - shortfall
            var guard = 0
            while (excess > 0 && guard < goals.size * 4) {
                var progressed = false
                for (goal in ordered) {
                    if (excess <= 0) break
                    val current = result[goal.id] ?: 0L
                    if (current > 0L) {
                        result[goal.id] = current - 1
                        excess -= 1
                        progressed = true
                    }
                }
                if (!progressed) break
                guard += 1
            }
        }
        return result
    }

    fun totalReductions(reductions: Map<String, Long>): Long =
        reductions.values.sum()

    fun isTotalExact(reductions: Map<String, Long>, shortfall: Long): Boolean =
        shortfall > 0L && totalReductions(reductions) == shortfall

    /** True when every reduction is in [0, savedAmount]. */
    fun hasNoGoalBelowZero(goals: List<Goal>, reductions: Map<String, Long>): Boolean {
        return goals.all { goal ->
            val reduction = reductions[goal.id] ?: 0L
            reduction >= 0L && reduction <= goal.savedAmount.coerceAtLeast(0L)
        }
    }

    fun goalBelowZeroIds(goals: List<Goal>, reductions: Map<String, Long>): List<String> {
        return goals.filter { goal ->
            val reduction = reductions[goal.id] ?: 0L
            reduction < 0L || reduction > goal.savedAmount.coerceAtLeast(0L)
        }.map { it.id }
    }

    fun canSaveAndLock(
        goals: List<Goal>,
        reductions: Map<String, Long>,
        shortfall: Long,
    ): Boolean {
        return shortfall > 0L &&
            goals.isNotEmpty() &&
            isTotalExact(reductions, shortfall) &&
            hasNoGoalBelowZero(goals, reductions)
    }

    /** Running-total / validation copy for frame 18a. */
    fun statusMessage(
        goals: List<Goal>,
        reductions: Map<String, Long>,
        shortfall: Long,
        formatting: FormattingService,
    ): String? {
        if (shortfall <= 0L) {
            return "Withdrawal amount must be greater than ₹0."
        }
        val belowZero = goalBelowZeroIds(goals, reductions)
        if (belowZero.isNotEmpty()) {
            val names = goals.filter { it.id in belowZero }.joinToString { it.name }
            return "No goal may go below ₹0. Check: $names."
        }
        val total = totalReductions(reductions)
        if (total == shortfall) {
            return null
        }
        val totalFmt = formatting.formatInrFromPaisa(total)
        val shortfallFmt = formatting.formatInrFromPaisa(shortfall)
        return if (total < shortfall) {
            val remaining = formatting.formatInrFromPaisa(shortfall - total)
            "Total $totalFmt of $shortfallFmt. Assign the remaining $remaining."
        } else {
            val over = formatting.formatInrFromPaisa(total - shortfall)
            "Total $totalFmt of $shortfallFmt. Reduce by $over."
        }
    }

    fun readyMessage(): String = READY_MESSAGE

    fun makeAllocations(
        goals: List<Goal>,
        reductions: Map<String, Long>,
        shortfall: Long,
    ): List<GoalAllocation> {
        if (!canSaveAndLock(goals, reductions, shortfall)) {
            throw WithdrawalException(
                field = "reductions",
                message = "Reductions must equal the shortfall and no goal may go below ₹0.",
            )
        }
        return goals.map { goal ->
            val amount = reductions[goal.id] ?: 0L
            val percentage = if (shortfall == 0L) {
                0.0
            } else {
                BigDecimal.valueOf(amount)
                    .divide(BigDecimal.valueOf(shortfall), 4, RoundingMode.HALF_UP)
                    .toDouble()
            }
            GoalAllocation(
                goalId = goal.id,
                goalName = goal.name,
                amount = amount,
                percentage = percentage,
            )
        }
    }

    fun createLockedWithdrawalEntry(
        goals: List<Goal>,
        previousBalance: Long,
        newBalance: Long,
        reductions: Map<String, Long>,
        id: String = UUID.randomUUID().toString(),
        createdAt: String,
        isTyped: Boolean = false,
    ): HistoryEntry {
        val amount = shortfall(previousBalance, newBalance)
        val allocations = makeAllocations(goals, reductions, amount)
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.Withdrawal,
            createdAt = createdAt,
            isLocked = true,
            previousBalance = previousBalance,
            newBalance = newBalance,
            withdrawalAmount = amount,
            isTyped = isTyped,
            allocations = allocations,
        )
    }

    /**
     * Applies a locked withdrawal: decreases goal saved amounts, updates dedicated balance,
     * appends History entry (18b). Standing split unchanged.
     */
    fun applyWithdrawal(
        state: AppState,
        previousBalance: Long,
        newBalance: Long,
        reductions: Map<String, Long>,
        dedicatedAccountId: String,
        nowIso: String,
        id: String = UUID.randomUUID().toString(),
        isTyped: Boolean = false,
    ): AppState {
        if (newBalance < 0L) {
            throw WithdrawalException(
                field = "newBalance",
                message = "New balance cannot be negative.",
            )
        }
        if (previousBalance <= newBalance) {
            throw WithdrawalException(
                field = "shortfall",
                message = "Withdrawal requires a lower balance.",
            )
        }
        val amount = shortfall(previousBalance, newBalance)
        if (!canSaveAndLock(state.goals, reductions, amount)) {
            throw WithdrawalException(
                field = "reductions",
                message = "Reductions must equal the shortfall and no goal may go below ₹0.",
            )
        }

        val dedicated = state.accounts.firstOrNull {
            it.id == dedicatedAccountId || it.isDedicated
        } ?: throw WithdrawalException(
            field = "accounts",
            message = "Dedicated savings account not found.",
        )
        if (dedicated.balance != previousBalance) {
            throw WithdrawalException(
                field = "previousBalance",
                message = "Account balance changed. Refresh and try again.",
            )
        }

        val entry = createLockedWithdrawalEntry(
            goals = state.goals,
            previousBalance = previousBalance,
            newBalance = newBalance,
            reductions = reductions,
            id = id,
            createdAt = nowIso,
            isTyped = isTyped,
        )

        val goalsById = state.goals.associateBy { it.id }.toMutableMap()
        for (allocation in entry.allocations) {
            val goal = goalsById[allocation.goalId]
                ?: throw WithdrawalException(
                    field = "goalId",
                    message = "Allocation references unknown goal ${allocation.goalId}.",
                )
            val nextSaved = goal.savedAmount - allocation.amount
            if (nextSaved < 0L) {
                throw WithdrawalException(
                    field = "savedAmount",
                    message = "Goal ${goal.name} cannot go below ₹0.",
                )
            }
            goalsById[allocation.goalId] = goal.copy(
                savedAmount = nextSaved,
                updatedAt = nowIso,
            )
        }

        val accounts = state.accounts.map { account ->
            if (account.id == dedicated.id) {
                account.copy(balance = newBalance)
            } else {
                account
            }
        }

        return state.copy(
            accounts = accounts,
            goals = state.goals.map { goalsById[it.id] ?: it },
            history = state.history + entry,
            // standingSplits intentionally unchanged
        )
    }

    companion object {
        const val READY_MESSAGE: String = "Total matches shortfall. Ready to save and lock."
        const val EDIT_ONCE_CAPTION: String = "You can change these reductions once."
        const val PROPORTIONAL_CAPTION: String = "Default is proportional to current savings."
        const val RECORD_WITHDRAWAL_TITLE: String = "Record a withdrawal"
    }
}
