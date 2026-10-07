package com.piplanner.domain

import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.StandingSplit
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standing split helpers (PRD R12, Spec BR-2 / BR-4).
 * Hosts [percentagesForNextCredit] for Sync / credit entry defaults (PIP-48).
 */
@Singleton
class StandingSplitService @Inject constructor(
    private val openingSplitService: OpeningSplitService,
) {

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

    fun isValidHundredPercent(fractions: List<BigDecimal>): Boolean =
        openingSplitService.isValidHundredPercent(fractions)

    companion object {
        const val SAVED_MONEY_STAYS_PUT: String = "Saved money stays put"
        const val CHANGE_APPLIES_NEXT_CREDIT: String =
            "Change saved. Applies at the next credit."
    }
}
