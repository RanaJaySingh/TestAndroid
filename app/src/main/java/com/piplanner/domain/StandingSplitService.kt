package com.piplanner.domain

import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.StandingSplit
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standing split (frame 15) — PRD R12 / R13 / R22 / R24, Spec BR-2 / BR-4 / BR-9.
 *
 * Reusable for PIP-54 (delete renormalize) and PIP-48 ("Use this split" / next-credit defaults).
 * Persists [AppState.standingSplits] and mirrors shares onto [Goal.shareOfNewCredits].
 * Does not rewrite History or change saved amounts ("Saved money stays put").
 *
 * Hosts [percentagesForNextCredit] for Sync / credit entry defaults (PIP-48),
 * equal / renormalize / apply helpers for delete (PIP-54), and the Standing split
 * screen (PIP-52). Delegates single-goal and 100% checks to [OpeningSplitService]
 * where shared.
 */
@Singleton
class StandingSplitService @Inject constructor(
    private val openingSplitService: OpeningSplitService,
) {

    // MARK: - UI gating

    /** One goal → skip Standing split UI and assign 100% automatically (R12). */
    fun shouldSkipUi(goals: List<Goal>): Boolean = goals.size <= 1

    // MARK: - Validation (BR-2 / R22)

    /** Returns true when fractions (0.0–1.0) sum to exactly 100%. */
    fun isValidHundredPercent(fractions: List<BigDecimal>): Boolean {
        if (fractions.isEmpty()) return false
        return normalizedTotal(fractions).compareTo(HUNDRED_PERCENT_FRACTION) == 0
    }

    fun normalizedTotal(fractions: List<BigDecimal>): BigDecimal {
        val sum = fractions.fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }
        return sum.setScale(4, RoundingMode.HALF_UP)
    }

    fun totalDisplayPercent(fractions: List<BigDecimal>): BigDecimal {
        return normalizedTotal(fractions)
            .multiply(HUNDRED_PERCENT_DISPLAY)
            .setScale(2, RoundingMode.HALF_UP)
    }

    /** Shortfall copy when total ≠ 100% (PRD R22). */
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

    fun singleGoalPercentages(goalId: String): Map<String, BigDecimal> {
        return mapOf(goalId to HUNDRED_PERCENT_FRACTION)
    }

    // MARK: - Defaults for editor

    /**
     * Display percents (0…100) for the Standing split editor.
     * Prefers persisted [AppState.standingSplits], then [Goal.shareOfNewCredits], else equal.
     */
    fun defaultDisplayPercents(goals: List<Goal>, standingSplits: List<StandingSplit>): Map<String, Int> {
        if (goals.isEmpty()) return emptyMap()
        if (goals.size == 1) return mapOf(goals.first().id to 100)

        val fromStanding = goals.associate { goal ->
            val fraction = standingSplits.firstOrNull { it.goalId == goal.id }?.percentage
            goal.id to ((fraction ?: goal.shareOfNewCredits) * 100.0).toInt()
        }
        if (fromStanding.values.sum() == 100) return fromStanding

        val fromShares = goals.associate { goal ->
            goal.id to (goal.shareOfNewCredits * 100.0).toInt()
        }
        if (fromShares.values.sum() == 100) return fromShares

        return equalDisplayPercents(goals)
    }

    fun equalDisplayPercents(goals: List<Goal>): Map<String, Int> {
        if (goals.isEmpty()) return emptyMap()
        val base = 100 / goals.size
        var remainder = 100 - (base * goals.size)
        return goals.associate { goal ->
            val extra = if (remainder > 0) 1 else 0
            if (remainder > 0) remainder -= 1
            goal.id to (base + extra)
        }
    }

    fun displayPercentsToFractions(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
    ): Map<String, BigDecimal> {
        if (goals.size == 1) {
            return singleGoalPercentages(goals.first().id)
        }
        return goals.associate { goal ->
            val percent = displayPercents[goal.id] ?: 0
            goal.id to BigDecimal.valueOf(percent.toLong())
                .divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        }
    }

    // MARK: - Next-credit defaults (PIP-48)

    /**
     * Default split fractions for the next credit.
     * Prefers persisted standing splits when they total 100%; else goal shares.
     * Single goal → 100% (frame 13e).
     */
    fun percentagesForNextCredit(state: AppState): Map<String, BigDecimal> {
        return percentagesForNextCredit(
            goals = state.goals,
            standingSplits = state.standingSplits,
        )
    }

    fun percentagesForNextCredit(
        goals: List<Goal>,
        standingSplits: List<StandingSplit>,
    ): Map<String, BigDecimal> {
        if (goals.isEmpty()) return emptyMap()
        if (goals.size == 1) {
            return openingSplitService.singleGoalPercentages(goals.first().id)
        }

        val fromStanding = standingSplits.associate {
            it.goalId to BigDecimal.valueOf(it.percentage).setScale(4, RoundingMode.HALF_UP)
        }
        val orderedFromStanding = goals.map { fromStanding[it.id] ?: BigDecimal.ZERO }
        if (openingSplitService.isValidHundredPercent(orderedFromStanding)) {
            return goals.associate { goal ->
                goal.id to (fromStanding[goal.id] ?: BigDecimal.ZERO)
            }
        }

        return goals.associate { goal ->
            goal.id to BigDecimal.valueOf(goal.shareOfNewCredits).setScale(4, RoundingMode.HALF_UP)
        }
    }

    /**
     * Fractions used when the next credit arrives.
     * Falls back to [Goal.shareOfNewCredits] when standingSplits is empty.
     */
    fun fractionsForNextCredit(state: AppState): Map<String, BigDecimal> {
        val goals = state.goals
        if (goals.isEmpty()) return emptyMap()
        if (goals.size == 1) return singleGoalPercentages(goals.first().id)

        if (state.standingSplits.isNotEmpty()) {
            val byId = state.standingSplits.associate { it.goalId to it.percentage }
            val map = goals.associate { goal ->
                val pct = byId[goal.id] ?: goal.shareOfNewCredits
                goal.id to BigDecimal.valueOf(pct).setScale(4, RoundingMode.HALF_UP)
            }
            if (isValidHundredPercent(map.values.toList())) return map
        }

        return goals.associate { goal ->
            goal.id to BigDecimal.valueOf(goal.shareOfNewCredits).setScale(4, RoundingMode.HALF_UP)
        }
    }

    // MARK: - Persistence

    /**
     * Applies a valid standing split: updates [AppState.standingSplits] and each goal's
     * [Goal.shareOfNewCredits]. Saved amounts and History are unchanged.
     *
     * @throws StandingSplitException when goals empty or percentages ≠ 100%.
     */
    fun applyStandingSplit(
        state: AppState,
        percentages: Map<String, BigDecimal>,
        nowIso: String,
    ): AppState {
        val goals = state.goals
        if (goals.isEmpty()) {
            throw StandingSplitException(
                field = "goals",
                message = "At least one goal is required for standing split.",
            )
        }

        val ordered = goals.map { percentages[it.id] ?: BigDecimal.ZERO }
        if (!isValidHundredPercent(ordered)) {
            throw StandingSplitException(
                field = "percentages",
                message = shortfallMessage(ordered) ?: "Splits must total 100%.",
            )
        }

        val nextSplits = goals.map { goal ->
            val fraction = (percentages[goal.id] ?: BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP)
            StandingSplit(goalId = goal.id, percentage = fraction.toDouble())
        }
        val nextGoals = goals.map { goal ->
            val fraction = (percentages[goal.id] ?: BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP)
            goal.copy(
                shareOfNewCredits = fraction.toDouble(),
                updatedAt = nowIso,
                // savedAmount intentionally unchanged — "Saved money stays put"
            )
        }

        return state.copy(
            goals = nextGoals,
            standingSplits = nextSplits,
            // history intentionally unchanged (R11 / R24)
        )
    }

    /** One-goal path: persist 100% without showing the editor. */
    fun applySingleGoalSkip(state: AppState, nowIso: String): AppState {
        val goal = state.goals.singleOrNull()
            ?: throw StandingSplitException(
                field = "goals",
                message = "Single-goal skip requires exactly one goal.",
            )
        return applyStandingSplit(
            state = state,
            percentages = singleGoalPercentages(goal.id),
            nowIso = nowIso,
        )
    }

    /** Equal shares across [goalIds] that sum to exactly 1.0 (largest-remainder on basis points). */
    fun equalSplits(goalIds: List<String>): List<StandingSplit> {
        if (goalIds.isEmpty()) return emptyList()
        if (goalIds.size == 1) {
            return listOf(StandingSplit(goalId = goalIds.first(), percentage = 1.0))
        }
        val fractions = equalFractions(goalIds.size)
        return goalIds.mapIndexed { index, id ->
            StandingSplit(goalId = id, percentage = fractions[index].toDouble())
        }
    }

    /**
     * Renormalizes standing split after [removedGoalId] is deleted (BR-9 / R13).
     * - Zero remaining → empty
     * - One remaining → 100%
     * - Else proportional to prior shares among survivors; if prior sum is 0 → equal
     */
    fun renormalizeAfterRemoving(
        splits: List<StandingSplit>,
        removedGoalId: String,
    ): List<StandingSplit> {
        val remaining = splits.filter { it.goalId != removedGoalId }
        if (remaining.isEmpty()) return emptyList()
        if (remaining.size == 1) {
            return listOf(StandingSplit(goalId = remaining.first().goalId, percentage = 1.0))
        }

        val fractions = remaining.map { BigDecimal.valueOf(it.percentage) }
        val total = fractions.fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }
            .setScale(8, RoundingMode.HALF_UP)
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return equalSplits(remaining.map { it.goalId })
        }

        val renormalized = fractions.map { fraction ->
            fraction.divide(total, 8, RoundingMode.HALF_UP)
        }
        val snapped = snapToHundred(renormalized)
        return remaining.mapIndexed { index, split ->
            StandingSplit(goalId = split.goalId, percentage = snapped[index].toDouble())
        }
    }

    /** Replaces standing shares with equal distribution (frame 17d mid-delete create). */
    fun resetToEqual(goalIds: List<String>): List<StandingSplit> = equalSplits(goalIds)

    /** Insert or update one goal’s standing share (shared with Goal edit / PIP-52). */
    fun upsert(
        splits: List<StandingSplit>,
        goalId: String,
        percentage: Double,
    ): List<StandingSplit> {
        if (splits.none { it.goalId == goalId }) {
            return splits + StandingSplit(goalId = goalId, percentage = percentage)
        }
        return splits.map {
            if (it.goalId == goalId) it.copy(percentage = percentage) else it
        }
    }

    fun isValidHundred(splits: List<StandingSplit>): Boolean {
        if (splits.isEmpty()) return false
        return openingSplitService.isValidHundredPercent(
            splits.map { BigDecimal.valueOf(it.percentage) },
        )
    }

    /** Copies standing percentages onto matching goals’ [Goal.shareOfNewCredits]. */
    fun applySharesToGoals(goals: List<Goal>, splits: List<StandingSplit>, nowIso: String): List<Goal> {
        val byId = splits.associateBy { it.goalId }
        return goals.map { goal ->
            val share = byId[goal.id]?.percentage ?: goal.shareOfNewCredits
            goal.copy(shareOfNewCredits = share, updatedAt = nowIso)
        }
    }

    /** Display percents 0…100 that sum to 100 for [count] goals. */
    fun equalDisplayPercents(count: Int): List<Int> {
        if (count <= 0) return emptyList()
        if (count == 1) return listOf(100)
        val base = 100 / count
        var remainder = 100 - (base * count)
        return List(count) {
            val extra = if (remainder > 0) 1 else 0
            if (remainder > 0) remainder -= 1
            base + extra
        }
    }

    private fun equalFractions(count: Int): List<BigDecimal> {
        val percents = equalDisplayPercents(count)
        return percents.map {
            BigDecimal.valueOf(it.toLong()).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        }
    }

    /** Snap fractions so they sum to exactly 1.0000 (basis-point largest remainder). */
    private fun snapToHundred(fractions: List<BigDecimal>): List<BigDecimal> {
        if (fractions.isEmpty()) return emptyList()
        val basis = fractions.map {
            it.multiply(BigDecimal("10000")).setScale(0, RoundingMode.DOWN).toLong()
        }.toMutableList()
        var leftover = 10_000L - basis.sum()
        var index = 0
        while (leftover > 0 && index < basis.size) {
            basis[index] += 1
            leftover -= 1
            index += 1
        }
        return basis.map {
            BigDecimal.valueOf(it).divide(BigDecimal("10000"), 4, RoundingMode.HALF_UP)
        }
    }

    companion object {
        val HUNDRED_PERCENT_FRACTION: BigDecimal = BigDecimal.ONE
        val HUNDRED_PERCENT_DISPLAY: BigDecimal = BigDecimal("100")

        /** Frame 15 copy — PRD R12 / ticket acceptance. */
        const val SAVED_MONEY_STAYS_PUT: String = "Saved money stays put"

        const val READY_MESSAGE: String = "Total 100%. Ready to save."

        const val CHANGE_APPLIES_NEXT_CREDIT: String =
            "Change saved. Applies at the next credit."

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

/** Validation failure for standing split (Spec SplitNotHundred family). */
class StandingSplitException(
    val field: String,
    override val message: String,
) : Exception(message)
