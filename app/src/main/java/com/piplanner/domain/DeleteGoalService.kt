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
 * Delete goal flow — PRD R13, Spec BR-9.
 * Moves the deleted goal’s saved amount onto remaining goals (equal default, editable once),
 * writes a locked History `GoalDeleted` entry, and renormalizes standing split.
 */
@Singleton
class DeleteGoalService @Inject constructor(
    private val openingSplitService: OpeningSplitService,
    private val standingSplitService: StandingSplitService,
) {

    companion object {
        /** History list label for GoalDeleted (PRD R13 / iOS PIP-53 parity). */
        const val historyTitle: String = "Deleted / moved"
    }

    /** Equal reassignment display percents (0…100) across [destinationGoals]. */
    fun equalReassignmentDisplayPercents(destinationGoals: List<Goal>): Map<String, Int> {
        val percents = standingSplitService.equalDisplayPercents(destinationGoals.size)
        return destinationGoals.mapIndexed { index, goal -> goal.id to percents[index] }.toMap()
    }

    /**
     * Builds destination allocations that sum exactly to the deleted goal’s [Goal.savedAmount].
     * @throws DeleteGoalException when destinations empty or percentages ≠ 100%.
     */
    fun makeReassignmentAllocations(
        deletedGoal: Goal,
        destinationGoals: List<Goal>,
        percentages: Map<String, BigDecimal>,
    ): List<GoalAllocation> {
        if (destinationGoals.isEmpty()) {
            throw DeleteGoalException(
                field = "destinations",
                message = "Create a replacement goal before deleting the only goal.",
            )
        }
        val orderedFractions = destinationGoals.map { percentages[it.id] ?: BigDecimal.ZERO }
        if (!openingSplitService.isValidHundredPercent(orderedFractions)) {
            throw DeleteGoalException(
                field = "percentages",
                message = openingSplitService.shortfallMessage(orderedFractions)
                    ?: "Reassignment must total 100%.",
            )
        }
        val amounts = openingSplitService.allocatePaisa(deletedGoal.savedAmount, orderedFractions)
        return destinationGoals.mapIndexed { index, goal ->
            val fraction = orderedFractions[index].setScale(4, RoundingMode.HALF_UP)
            GoalAllocation(
                goalId = goal.id,
                goalName = goal.name,
                amount = amounts[index],
                percentage = fraction.toDouble(),
            )
        }
    }

    /** Locked History entry for delete / move (PRD R13 “deleted / moved”). */
    fun createLockedDeletedEntry(
        deletedGoal: Goal,
        destinationGoals: List<Goal>,
        percentages: Map<String, BigDecimal>,
        id: String = UUID.randomUUID().toString(),
        createdAt: String,
    ): HistoryEntry {
        val allocations = makeReassignmentAllocations(deletedGoal, destinationGoals, percentages)
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.GoalDeleted,
            createdAt = createdAt,
            isLocked = true,
            deletedGoalName = deletedGoal.name,
            releasedAmount = deletedGoal.savedAmount,
            allocations = allocations,
        )
    }

    /**
     * Applies delete: moves money, removes the goal (and optional prior destinations update),
     * appends locked History, renormalizes standing split across survivors.
     *
     * [replacementGoal] (frame 17e / 17d) is inserted into the surviving goals list before money moves.
     * When [resetStandingSplitToEqual] is true (17d), standing split becomes equal across survivors.
     */
    fun applyDelete(
        state: AppState,
        deletedGoalId: String,
        percentages: Map<String, BigDecimal>,
        nowIso: String,
        replacementGoal: Goal? = null,
        resetStandingSplitToEqual: Boolean = false,
        entryId: String = UUID.randomUUID().toString(),
    ): AppState {
        val deletedGoal = state.goals.firstOrNull { it.id == deletedGoalId }
            ?: throw DeleteGoalException(
                field = "goalId",
                message = "Goal not found.",
            )

        val withoutDeleted = state.goals.filter { it.id != deletedGoalId }
        val destinations = buildList {
            addAll(withoutDeleted)
            if (replacementGoal != null) {
                if (any { it.id == replacementGoal.id }) {
                    throw DeleteGoalException(
                        field = "replacement",
                        message = "Replacement goal id already exists.",
                    )
                }
                add(replacementGoal.copy(savedAmount = 0L, updatedAt = nowIso))
            }
        }

        if (destinations.isEmpty()) {
            throw DeleteGoalException(
                field = "destinations",
                message = "Create a replacement goal before deleting the only goal.",
            )
        }

        val entry = createLockedDeletedEntry(
            deletedGoal = deletedGoal,
            destinationGoals = destinations,
            percentages = percentages,
            id = entryId,
            createdAt = nowIso,
        )

        val amountsById = entry.allocations.associate { it.goalId to it.amount }
        val updatedGoals = destinations.map { goal ->
            val moved = amountsById[goal.id] ?: 0L
            goal.copy(
                savedAmount = goal.savedAmount + moved,
                updatedAt = nowIso,
            )
        }

        val standingSplits = if (resetStandingSplitToEqual) {
            standingSplitService.resetToEqual(updatedGoals.map { it.id })
        } else {
            val baseSplits = state.standingSplits.ifEmpty {
                state.goals.map { StandingSplit(goalId = it.id, percentage = it.shareOfNewCredits) }
            }
            val withReplacement = if (replacementGoal != null) {
                baseSplits + StandingSplit(goalId = replacementGoal.id, percentage = 0.0)
            } else {
                baseSplits
            }
            standingSplitService.renormalizeAfterRemoving(withReplacement, deletedGoalId)
        }

        val goalsWithShares = standingSplitService.applySharesToGoals(
            goals = updatedGoals,
            splits = standingSplits,
            nowIso = nowIso,
        )

        return state.copy(
            goals = goalsWithShares,
            standingSplits = standingSplits,
            history = state.history + entry,
            heldGoalChanges = state.heldGoalChanges.filterNot { it.goalId == deletedGoalId },
        )
    }
}

/** Validation / apply failure for delete goal (BR-9 family). */
class DeleteGoalException(
    val field: String,
    override val message: String,
) : Exception(message)
