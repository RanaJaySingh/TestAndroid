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

/**
 * Opening split validation and History lock helpers (PRD R6 / R22, Spec BR-2 / BR-3).
 * Money is always Long paisa; percentages validated as exact 100% fractions.
 */
@Singleton
class OpeningSplitService @Inject constructor() {

    // MARK: - Validation (BR-2 / R22)

    /** Returns true when fractions (0.0–1.0) sum to exactly 100%. */
    fun isValidHundredPercent(fractions: List<BigDecimal>): Boolean {
        if (fractions.isEmpty()) return false
        return normalizedTotal(fractions).compareTo(HUNDRED_PERCENT_FRACTION) == 0
    }

    /** Sum of fractions (0.0–1.0), normalized to avoid float noise. */
    fun normalizedTotal(fractions: List<BigDecimal>): BigDecimal {
        val sum = fractions.fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }
        return sum.setScale(4, RoundingMode.HALF_UP)
    }

    /** Running total as a display percent 0…100+. */
    fun totalDisplayPercent(fractions: List<BigDecimal>): BigDecimal {
        return normalizedTotal(fractions)
            .multiply(HUNDRED_PERCENT_DISPLAY)
            .setScale(2, RoundingMode.HALF_UP)
    }

    /** Shortfall copy when total ≠ 100% (PRD R22 example style). */
    fun shortfallMessage(fractions: List<BigDecimal>): String? {
        val total = totalDisplayPercent(fractions)
        if (total.compareTo(HUNDRED_PERCENT_DISPLAY) == 0) {
            return null
        }
        if (total < HUNDRED_PERCENT_DISPLAY) {
            val remaining = HUNDRED_PERCENT_DISPLAY.subtract(total).setScale(2, RoundingMode.HALF_UP)
            return "Total ${formatPercent(total)}. Assign the remaining ${formatPercent(remaining)}."
        }
        val over = total.subtract(HUNDRED_PERCENT_DISPLAY).setScale(2, RoundingMode.HALF_UP)
        return "Total ${formatPercent(total)}. Reduce by ${formatPercent(over)}."
    }

    // MARK: - Allocations

    /**
     * Builds per-goal allocations that sum exactly to [openingBalance] (paisa).
     * @throws OpeningSplitException when goals empty or percentages ≠ 100%.
     */
    fun makeAllocations(
        goals: List<Goal>,
        openingBalance: Long,
        percentages: Map<String, BigDecimal>,
    ): List<GoalAllocation> {
        if (goals.isEmpty()) {
            throw OpeningSplitException(
                field = "goals",
                message = "At least one goal is required for opening split.",
            )
        }

        val orderedFractions = goals.map { percentages[it.id] ?: BigDecimal.ZERO }
        if (!isValidHundredPercent(orderedFractions)) {
            throw OpeningSplitException(
                field = "percentages",
                message = shortfallMessage(orderedFractions) ?: "Splits must total 100%.",
            )
        }

        val amounts = allocatePaisa(openingBalance, orderedFractions)
        return goals.mapIndexed { index, goal ->
            val fraction = orderedFractions[index].setScale(4, RoundingMode.HALF_UP)
            GoalAllocation(
                goalId = goal.id,
                goalName = goal.name,
                amount = amounts[index],
                percentage = fraction.toDouble(),
            )
        }
    }

    /** Largest-remainder allocation so paisa amounts sum exactly to [total]. */
    fun allocatePaisa(total: Long, fractions: List<BigDecimal>): List<Long> {
        if (fractions.isEmpty()) return emptyList()
        if (total < 0L) return List(fractions.size) { 0L }

        data class Remainder(val index: Int, val fraction: Double)

        val floors = LongArray(fractions.size)
        val remainders = ArrayList<Remainder>(fractions.size)

        fractions.forEachIndexed { index, fraction ->
            val exact = BigDecimal.valueOf(total).multiply(fraction).toDouble()
            val floorValue = exact.toLong() // toward zero for non-negative
            floors[index] = floorValue
            remainders.add(Remainder(index, exact - floorValue.toDouble()))
        }

        var leftover = total - floors.sum()
        val ranked = remainders.sortedWith(
            compareByDescending<Remainder> { it.fraction }.thenBy { it.index },
        )
        var cursor = 0
        while (leftover > 0 && cursor < ranked.size) {
            floors[ranked[cursor].index] += 1
            leftover -= 1
            cursor += 1
        }
        return floors.toList()
    }

    // MARK: - History entry (BR-3 / R6)

    /** Creates a locked Opening balance History entry. Allocations never change after this. */
    fun createLockedOpeningEntry(
        goals: List<Goal>,
        openingBalance: Long,
        percentages: Map<String, BigDecimal>,
        id: String = UUID.randomUUID().toString(),
        createdAt: String,
        isTyped: Boolean = true,
    ): HistoryEntry {
        val allocations = makeAllocations(goals, openingBalance, percentages)
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.OpeningBalance,
            createdAt = createdAt,
            isLocked = true,
            previousBalance = null,
            newBalance = openingBalance,
            creditAmount = openingBalance,
            isTyped = isTyped,
            allocations = allocations,
        )
    }

    /**
     * Applies a locked opening entry: updates goal saved amounts + standing shares, appends history.
     * Also marks [AppState.hasCompletedSetup] true.
     */
    fun applyOpeningLock(
        state: AppState,
        entry: HistoryEntry,
        nowIso: String,
    ): AppState {
        if (entry.type != HistoryEntryType.OpeningBalance) {
            throw OpeningSplitException(
                field = "type",
                message = "Expected an Opening balance History entry.",
            )
        }
        if (!entry.isLocked) {
            throw OpeningSplitException(
                field = "isLocked",
                message = "Opening balance entry must be locked when saved.",
            )
        }
        if (state.history.any { it.type == HistoryEntryType.OpeningBalance && it.isLocked }) {
            throw OpeningSplitException(
                field = "history",
                message = "Opening balance is already locked and cannot be changed.",
            )
        }

        val goalsById = state.goals.associateBy { it.id }.toMutableMap()
        for (allocation in entry.allocations) {
            val goal = goalsById[allocation.goalId]
                ?: throw OpeningSplitException(
                    field = "goalId",
                    message = "Allocation references unknown goal ${allocation.goalId}.",
                )
            goalsById[allocation.goalId] = goal.copy(
                savedAmount = allocation.amount,
                shareOfNewCredits = allocation.percentage,
                updatedAt = nowIso,
            )
        }

        return state.copy(
            goals = state.goals.map { goalsById[it.id] ?: it },
            standingSplits = entry.allocations.map {
                StandingSplit(goalId = it.goalId, percentage = it.percentage)
            },
            history = state.history + entry,
            hasCompletedSetup = true,
        )
    }

    /** Single-goal setup: auto-assign 100% with no editable % fields (frame 8b). */
    fun singleGoalPercentages(goalId: String): Map<String, BigDecimal> {
        return mapOf(goalId to HUNDRED_PERCENT_FRACTION)
    }

    companion object {
        val HUNDRED_PERCENT_FRACTION: BigDecimal = BigDecimal.ONE
        val HUNDRED_PERCENT_DISPLAY: BigDecimal = BigDecimal("100")

        /** Read-only copy for locked opening entries (PRD R6 / ticket wording). */
        const val LOCKED_AMOUNTS_CAPTION: String = "Locked amounts never change"

        private fun formatPercent(value: BigDecimal): String {
            val stripped = value.stripTrailingZeros()
            return if (stripped.scale() <= 0) {
                "${stripped.toBigIntegerExact()}%"
            } else {
                "${value.toPlainString()}%"
            }
        }
    }
}

/** Validation failure for opening split (Spec SplitNotHundred family). */
class OpeningSplitException(
    val field: String,
    override val message: String,
) : Exception(message)
