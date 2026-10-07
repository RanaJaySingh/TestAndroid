package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalAllocation
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.util.DemoData
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BR-4 / R11 / R24 held-to-next-credit goal edits (PIP-50).
 */
class GoalHeldChangeServiceTest {

    private lateinit var service: GoalHeldChangeService

    @Before
    fun setUp() {
        val opening = OpeningSplitService()
        service = GoalHeldChangeService(
            goalValidationService = GoalValidationService(opening),
            standingSplitService = StandingSplitService(opening),
        )
    }

    @Test
    fun toastMessageMatchesFrame9cAcceptanceCopy() {
        assertThat(service.toastMessage())
            .isEqualTo("Change saved. Applies at next credit.")
    }

    @Test
    fun heldInfoMessageDescribesNextCreditOnly() {
        assertThat(service.heldInfoMessage())
            .contains("next credit")
    }

    @Test
    fun applyHeldEdit_updatesGoalFieldsButKeepsSavedAmountAndHistoryFrozen() {
        val state = seededState()
        val originalHistory = state.history
        val originalSaved = state.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.savedAmount

        val draft = GoalEditDraft(
            name = "Family Car",
            targetAmount = 60_000_000L,
            startDate = "2026-01-01",
            endDate = "2028-01-01",
            inflationRate = 0.08,
            shareOfNewCredits = 0.55,
        )

        val next = service.applyHeldEdit(
            state = state,
            goalId = DemoData.DEMO_CAR_GOAL_ID,
            draft = draft,
            nowIso = "2026-06-01T12:00:00Z",
        )

        val updated = next.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }
        assertThat(updated.name).isEqualTo("Family Car")
        assertThat(updated.targetAmount).isEqualTo(60_000_000L)
        assertThat(updated.startDate).isEqualTo("2026-01-01")
        assertThat(updated.endDate).isEqualTo("2028-01-01")
        assertThat(updated.inflationRate).isEqualTo(0.08)
        assertThat(updated.shareOfNewCredits).isEqualTo(0.55)
        assertThat(updated.savedAmount).isEqualTo(originalSaved)
        assertThat(updated.updatedAt).isEqualTo("2026-06-01T12:00:00Z")
        assertThat(updated.createdAt).isEqualTo(
            state.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.createdAt,
        )

        assertThat(next.history).isEqualTo(originalHistory)
        assertThat(service.hasHeldChange(next, DemoData.DEMO_CAR_GOAL_ID)).isTrue()
        assertThat(next.heldGoalChanges.single().goalId).isEqualTo(DemoData.DEMO_CAR_GOAL_ID)
        assertThat(next.heldGoalChanges.single().savedAt).isEqualTo("2026-06-01T12:00:00Z")
        assertThat(next.standingSplits.first { it.goalId == DemoData.DEMO_CAR_GOAL_ID }.percentage)
            .isEqualTo(0.55)
    }

    @Test
    fun applyHeldEdit_replacesExistingHeldChangeForSameGoal() {
        val once = service.applyHeldEdit(
            state = seededState(),
            goalId = DemoData.DEMO_CAR_GOAL_ID,
            draft = GoalEditDraft(
                name = "Car",
                targetAmount = 55_000_000L,
                startDate = "2026-01-01",
                endDate = "2027-01-01",
                inflationRate = 0.07,
                shareOfNewCredits = 0.6,
            ),
            nowIso = "2026-06-01T12:00:00Z",
        )
        val twice = service.applyHeldEdit(
            state = once,
            goalId = DemoData.DEMO_CAR_GOAL_ID,
            draft = GoalEditDraft(
                name = "Car v2",
                targetAmount = 70_000_000L,
                startDate = "2026-01-01",
                endDate = "2027-06-01",
                inflationRate = 0.09,
                shareOfNewCredits = 0.65,
            ),
            nowIso = "2026-07-01T12:00:00Z",
        )

        assertThat(twice.heldGoalChanges).hasSize(1)
        assertThat(twice.heldGoalChanges.single().savedAt).isEqualTo("2026-07-01T12:00:00Z")
        assertThat(twice.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.name).isEqualTo("Car v2")
    }

    @Test
    fun applyHeldEdit_rejectsUnknownGoal() {
        assertThrows(GoalHeldChangeException::class.java) {
            service.applyHeldEdit(
                state = seededState(),
                goalId = "missing-goal",
                draft = GoalEditDraft(
                    name = "X",
                    targetAmount = 1_000_000L,
                    startDate = "2026-01-01",
                    endDate = "2027-01-01",
                    inflationRate = 0.07,
                    shareOfNewCredits = 0.5,
                ),
                nowIso = "2026-06-01T12:00:00Z",
            )
        }
    }

    @Test
    fun applyHeldEdit_rejectsInvalidDraft() {
        assertThrows(GoalHeldChangeException::class.java) {
            service.applyHeldEdit(
                state = seededState(),
                goalId = DemoData.DEMO_CAR_GOAL_ID,
                draft = GoalEditDraft(
                    name = "  ",
                    targetAmount = 0L,
                    startDate = "2027-01-01",
                    endDate = "2026-01-01",
                    inflationRate = 0.07,
                    shareOfNewCredits = 0.6,
                ),
                nowIso = "2026-06-01T12:00:00Z",
            )
        }
    }

    @Test
    fun clearHeldChanges_removesFlagsWithoutTouchingHistoryOrSavedAmounts() {
        val held = service.applyHeldEdit(
            state = seededState(),
            goalId = DemoData.DEMO_CAR_GOAL_ID,
            draft = GoalEditDraft(
                name = "Car",
                targetAmount = 55_000_000L,
                startDate = "2026-01-01",
                endDate = "2027-01-01",
                inflationRate = 0.07,
                shareOfNewCredits = 0.6,
            ),
            nowIso = "2026-06-01T12:00:00Z",
        )
        val cleared = service.clearHeldChanges(held)

        assertThat(cleared.heldGoalChanges).isEmpty()
        assertThat(cleared.history).isEqualTo(held.history)
        assertThat(cleared.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.savedAmount)
            .isEqualTo(held.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.savedAmount)
        assertThat(cleared.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.targetAmount)
            .isEqualTo(55_000_000L)
    }

    @Test
    fun relatedHistoryEntries_includesAllocationsTransfersForGoal() {
        val state = seededState()
        val related = service.relatedHistoryEntries(state.history, DemoData.DEMO_CAR_GOAL_ID)

        assertThat(related.map { it.id }).containsExactly("hist-opening", "hist-transfer").inOrder()
        assertThat(
            service.relatedHistoryEntries(state.history, DemoData.DEMO_EMERGENCY_GOAL_ID)
                .map { it.id },
        ).containsExactly("hist-opening", "hist-transfer").inOrder()
        assertThat(service.relatedHistoryEntries(state.history, "unrelated")).isEmpty()
    }

    private fun seededState(): AppState {
        val car = Goal(
            id = DemoData.DEMO_CAR_GOAL_ID,
            name = "Car",
            targetAmount = 50_000_000L,
            startDate = "2026-01-01",
            endDate = "2027-01-01",
            inflationRate = 0.07,
            savedAmount = 6_000_000L,
            shareOfNewCredits = 0.6,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )
        val emergency = Goal(
            id = DemoData.DEMO_EMERGENCY_GOAL_ID,
            name = "Emergency Fund",
            targetAmount = 20_000_000L,
            startDate = "2026-01-01",
            endDate = "2027-01-01",
            inflationRate = 0.07,
            savedAmount = 4_000_000L,
            shareOfNewCredits = 0.4,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )
        return AppState(
            goals = listOf(car, emergency),
            history = listOf(
                HistoryEntry(
                    id = "hist-opening",
                    type = HistoryEntryType.OpeningBalance,
                    createdAt = "2026-01-01T00:00:00Z",
                    isLocked = true,
                    creditAmount = 10_000_000L,
                    newBalance = 10_000_000L,
                    allocations = listOf(
                        GoalAllocation(
                            goalId = DemoData.DEMO_CAR_GOAL_ID,
                            goalName = "Car",
                            amount = 6_000_000L,
                            percentage = 0.6,
                        ),
                        GoalAllocation(
                            goalId = DemoData.DEMO_EMERGENCY_GOAL_ID,
                            goalName = "Emergency Fund",
                            amount = 4_000_000L,
                            percentage = 0.4,
                        ),
                    ),
                ),
                HistoryEntry(
                    id = "hist-transfer",
                    type = HistoryEntryType.Transfer,
                    createdAt = "2026-02-01T00:00:00Z",
                    isLocked = true,
                    fromGoalId = DemoData.DEMO_CAR_GOAL_ID,
                    toGoalId = DemoData.DEMO_EMERGENCY_GOAL_ID,
                    transferAmount = 100_000L,
                ),
                HistoryEntry(
                    id = "hist-unrelated-delete",
                    type = HistoryEntryType.GoalDeleted,
                    createdAt = "2026-03-01T00:00:00Z",
                    isLocked = true,
                    deletedGoalName = "Vacation",
                    releasedAmount = 50_000L,
                ),
            ),
            standingSplits = listOf(
                StandingSplit(goalId = DemoData.DEMO_CAR_GOAL_ID, percentage = 0.6),
                StandingSplit(goalId = DemoData.DEMO_EMERGENCY_GOAL_ID, percentage = 0.4),
            ),
            hasCompletedSetup = true,
        )
    }
}
