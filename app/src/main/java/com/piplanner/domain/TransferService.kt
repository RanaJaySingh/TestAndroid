package com.piplanner.domain

import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class TransferException(
    val field: String,
    override val message: String,
) : Exception(message)

/** UI / validation phase for Transfer (frames 16 / 16a / 16b / 16c). */
enum class TransferPhase {
    Select,
    EnterAmount,
    Preview,
    OverAmount,
    Complete,
}

/**
 * Transfer between goals — PRD R14, Spec BR-7.
 *
 * Moves saved money From → To as a point-in-time action.
 * Does **not** change [AppState.standingSplits] or [Goal.shareOfNewCredits].
 * Amounts are Long paisa; never Float/Double.
 */
@Singleton
class TransferService @Inject constructor() {

    fun validate(
        goals: List<Goal>,
        fromGoalId: String?,
        toGoalId: String?,
        amountPaisa: Long,
    ): TransferValidation {
        if (fromGoalId.isNullOrBlank() || toGoalId.isNullOrBlank()) {
            return TransferValidation.Invalid(
                field = "selection",
                message = SELECT_GOALS_MESSAGE,
                phase = TransferPhase.Select,
            )
        }
        if (fromGoalId == toGoalId) {
            return TransferValidation.Invalid(
                field = "selection",
                message = SAME_GOAL_MESSAGE,
                phase = TransferPhase.Select,
            )
        }
        val from = goals.firstOrNull { it.id == fromGoalId }
            ?: return TransferValidation.Invalid(
                field = "fromGoalId",
                message = "From goal not found.",
                phase = TransferPhase.Select,
            )
        val to = goals.firstOrNull { it.id == toGoalId }
            ?: return TransferValidation.Invalid(
                field = "toGoalId",
                message = "To goal not found.",
                phase = TransferPhase.Select,
            )
        if (amountPaisa <= 0L) {
            return TransferValidation.Invalid(
                field = "amount",
                message = ENTER_AMOUNT_MESSAGE,
                phase = TransferPhase.EnterAmount,
                fromGoal = from,
                toGoal = to,
            )
        }
        if (amountPaisa > from.savedAmount) {
            return TransferValidation.Invalid(
                field = "amount",
                message = OVER_AMOUNT_MESSAGE,
                phase = TransferPhase.OverAmount,
                fromGoal = from,
                toGoal = to,
            )
        }
        return TransferValidation.Valid(
            fromGoal = from,
            toGoal = to,
            amountPaisa = amountPaisa,
            afterFromSaved = from.savedAmount - amountPaisa,
            afterToSaved = to.savedAmount + amountPaisa,
        )
    }

    fun canMove(
        goals: List<Goal>,
        fromGoalId: String?,
        toGoalId: String?,
        amountPaisa: Long,
    ): Boolean = validate(goals, fromGoalId, toGoalId, amountPaisa) is TransferValidation.Valid

    fun resolvePhase(
        goals: List<Goal>,
        fromGoalId: String?,
        toGoalId: String?,
        amountPaisa: Long,
        completed: Boolean,
    ): TransferPhase {
        if (completed) return TransferPhase.Complete
        return when (val result = validate(goals, fromGoalId, toGoalId, amountPaisa)) {
            is TransferValidation.Valid -> TransferPhase.Preview
            is TransferValidation.Invalid -> result.phase
        }
    }

    fun createHistoryEntry(
        fromGoalId: String,
        toGoalId: String,
        amountPaisa: Long,
        id: String = UUID.randomUUID().toString(),
        createdAt: String,
    ): HistoryEntry {
        if (fromGoalId == toGoalId) {
            throw TransferException(field = "selection", message = SAME_GOAL_MESSAGE)
        }
        if (amountPaisa <= 0L) {
            throw TransferException(field = "amount", message = ENTER_AMOUNT_MESSAGE)
        }
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.Transfer,
            createdAt = createdAt,
            isLocked = true,
            fromGoalId = fromGoalId,
            toGoalId = toGoalId,
            transferAmount = amountPaisa,
        )
    }

    /**
     * Debits From saved, credits To saved, appends locked Transfer History.
     * Standing split list and share percentages are left untouched (BR-7).
     */
    fun applyTransfer(
        state: AppState,
        fromGoalId: String,
        toGoalId: String,
        amountPaisa: Long,
        id: String = UUID.randomUUID().toString(),
        createdAt: String,
        nowIso: String,
    ): AppState {
        val validation = validate(state.goals, fromGoalId, toGoalId, amountPaisa)
        if (validation !is TransferValidation.Valid) {
            throw TransferException(
                field = (validation as TransferValidation.Invalid).field,
                message = validation.message,
            )
        }

        val entry = createHistoryEntry(
            fromGoalId = fromGoalId,
            toGoalId = toGoalId,
            amountPaisa = amountPaisa,
            id = id,
            createdAt = createdAt,
        )

        val standingBefore = state.standingSplits
        val nextGoals = state.goals.map { goal ->
            when (goal.id) {
                fromGoalId -> goal.copy(
                    savedAmount = validation.afterFromSaved,
                    updatedAt = nowIso,
                )
                toGoalId -> goal.copy(
                    savedAmount = validation.afterToSaved,
                    updatedAt = nowIso,
                )
                else -> goal
            }
        }

        val next = state.copy(
            goals = nextGoals,
            history = state.history + entry,
            standingSplits = standingBefore,
        )
        require(next.standingSplits == standingBefore) {
            "Transfer must not mutate standing split"
        }
        return next
    }

    companion object {
        /** Design chips — ₹1,000 / ₹5,000 / ₹10,000 (paisa). */
        val AMOUNT_CHIP_PAISA: List<Long> = listOf(100_000L, 500_000L, 1_000_000L)

        const val SELECT_GOALS_MESSAGE: String = "Choose different From and To goals."
        const val SAME_GOAL_MESSAGE: String = "From and To must be different goals."
        const val ENTER_AMOUNT_MESSAGE: String = "Enter an amount to move."
        const val OVER_AMOUNT_MESSAGE: String = "Amount is more than saved in From."
        const val COMPLETE_MESSAGE: String = "Transfer complete."
        const val MOVE_BUTTON_TITLE: String = "Move"
        const val AFTER_TRANSFER_TITLE: String = "After transfer"
    }
}

sealed class TransferValidation {
    data class Valid(
        val fromGoal: Goal,
        val toGoal: Goal,
        val amountPaisa: Long,
        val afterFromSaved: Long,
        val afterToSaved: Long,
    ) : TransferValidation()

    data class Invalid(
        val field: String,
        val message: String,
        val phase: TransferPhase,
        val fromGoal: Goal? = null,
        val toGoal: Goal? = null,
    ) : TransferValidation()
}
