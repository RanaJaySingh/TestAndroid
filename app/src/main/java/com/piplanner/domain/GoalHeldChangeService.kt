package com.piplanner.domain

import com.piplanner.data.model.AppState
import com.piplanner.data.model.HeldGoalChange
import com.piplanner.data.model.HistoryEntry
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Editable fields for Goal edit (frame 6e). [savedAmount] is never taken from the draft —
 * locked saved money stays on the existing [com.piplanner.data.model.Goal].
 */
data class GoalEditDraft(
    val name: String,
    val targetAmount: Long,
    val startDate: String,
    val endDate: String,
    val inflationRate: Double,
    val shareOfNewCredits: Double,
)

class GoalHeldChangeException(
    val field: String,
    override val message: String,
) : Exception(message)

/**
 * BR-4 / R11 / R24 — goal parameter and standing-share edits apply at the next credit.
 * History allocations stay frozen; saved amount on the goal stays locked.
 */
@Singleton
class GoalHeldChangeService @Inject constructor(
    private val goalValidationService: GoalValidationService,
    private val standingSplitService: StandingSplitService,
) {

    /** Frame 9c toast — acceptance copy for PIP-50. */
    fun toastMessage(): String = TOAST_MESSAGE

    /** Frame 13g edit-held info when viewing later. */
    fun heldInfoMessage(): String = HELD_INFO_MESSAGE

    fun hasHeldChange(state: AppState, goalId: String): Boolean {
        return state.heldGoalChanges.any { it.goalId == goalId }
    }

    /**
     * History rows that mention [goalId] via allocations or transfer endpoints.
     * Newest-last order preserved from [history].
     */
    fun relatedHistoryEntries(history: List<HistoryEntry>, goalId: String): List<HistoryEntry> {
        return history.filter { entry ->
            entry.allocations.any { it.goalId == goalId } ||
                entry.fromGoalId == goalId ||
                entry.toGoalId == goalId
        }
    }

    /**
     * Persists goal field / share edits without rewriting History or changing savedAmount.
     * Records a [HeldGoalChange] so detail can show 13g until the next credit clears it.
     */
    fun applyHeldEdit(
        state: AppState,
        goalId: String,
        draft: GoalEditDraft,
        nowIso: String,
    ): AppState {
        val existing = state.goals.firstOrNull { it.id == goalId }
            ?: throw GoalHeldChangeException(
                field = "goalId",
                message = "Goal not found.",
            )

        val start = parseDateOrThrow(draft.startDate, "startDate")
        val end = parseDateOrThrow(draft.endDate, "endDate")
        val errors = goalValidationService.validationErrors(
            name = draft.name,
            targetPaisa = draft.targetAmount,
            startDate = start,
            endDate = end,
        )
        if (errors.isNotEmpty()) {
            val first = errors.first()
            throw GoalHeldChangeException(field = first.field, message = first.message)
        }
        if (draft.shareOfNewCredits < 0.0 || draft.shareOfNewCredits > 1.0) {
            throw GoalHeldChangeException(
                field = "shareOfNewCredits",
                message = "Share must be between 0% and 100%.",
            )
        }

        val updatedGoal = existing.copy(
            name = draft.name.trim(),
            targetAmount = draft.targetAmount,
            startDate = start.toString(),
            endDate = end.toString(),
            inflationRate = draft.inflationRate,
            shareOfNewCredits = draft.shareOfNewCredits,
            savedAmount = existing.savedAmount,
            updatedAt = nowIso,
        )

        val nextGoals = state.goals.map { if (it.id == goalId) updatedGoal else it }
        val nextSplits = standingSplitService.upsert(
            splits = state.standingSplits,
            goalId = goalId,
            percentage = draft.shareOfNewCredits,
        )
        val nextHeld = state.heldGoalChanges
            .filterNot { it.goalId == goalId } +
            HeldGoalChange(goalId = goalId, savedAt = nowIso)

        return state.copy(
            goals = nextGoals,
            standingSplits = nextSplits,
            heldGoalChanges = nextHeld,
            // history intentionally unchanged (BR-4 / R11)
        )
    }

    /** Clears held flags after a credit is assigned (hook for PIP-48). */
    fun clearHeldChanges(state: AppState): AppState {
        if (state.heldGoalChanges.isEmpty()) return state
        return state.copy(heldGoalChanges = emptyList())
    }

    private fun parseDateOrThrow(value: String, field: String): LocalDate {
        return runCatching { LocalDate.parse(value) }.getOrElse {
            throw GoalHeldChangeException(field = field, message = "Invalid date.")
        }
    }

    companion object {
        const val TOAST_MESSAGE: String = "Change saved. Applies at next credit."
        const val HELD_INFO_MESSAGE: String =
            "Edits apply at the next credit. Earlier history stays as it is."
    }
}
