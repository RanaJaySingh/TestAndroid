package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.util.DemoData
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class DeleteGoalServiceTest {

    private lateinit var service: DeleteGoalService
    private lateinit var standingSplitService: StandingSplitService
    private val now = Instant.parse("2023-11-14T22:13:20Z").toString()

    @Before
    fun setUp() {
        val opening = OpeningSplitService()
        standingSplitService = StandingSplitService(opening)
        service = DeleteGoalService(opening, standingSplitService)
    }

    @Test
    fun equalReassignment_defaultsToEqualPercents() {
        val goals = seededGoals()
        val percents = service.equalReassignmentDisplayPercents(
            goals.filter { it.id != DemoData.DEMO_CAR_GOAL_ID },
        )
        assertThat(percents[DemoData.DEMO_EMERGENCY_GOAL_ID]).isEqualTo(100)
    }

    @Test
    fun equalReassignment_twoDestinationsSplitFiftyFifty() {
        val destinations = listOf(
            seededGoals()[1],
            seededGoals()[1].copy(id = "cccccccc-cccc-cccc-cccc-cccccccccccc", name = "House"),
        )
        val percents = service.equalReassignmentDisplayPercents(destinations)
        assertThat(percents.values.sum()).isEqualTo(100)
        assertThat(percents.values.toSet()).isEqualTo(setOf(50))
    }

    @Test
    fun makeReassignmentAllocations_movesAllReleasedPaisa() {
        val deleted = seededGoals().first() // Car saved 6_000_000
        val destinations = listOf(seededGoals()[1])
        val allocations = service.makeReassignmentAllocations(
            deletedGoal = deleted,
            destinationGoals = destinations,
            percentages = mapOf(DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal.ONE),
        )
        assertThat(allocations).hasSize(1)
        assertThat(allocations.first().amount).isEqualTo(6_000_000L)
        assertThat(allocations.sumOf { it.amount }).isEqualTo(deleted.savedAmount)
    }

    @Test
    fun makeReassignmentAllocations_rejectsEmptyDestinations() {
        val error = assertThrows(DeleteGoalException::class.java) {
            service.makeReassignmentAllocations(
                deletedGoal = seededGoals().first(),
                destinationGoals = emptyList(),
                percentages = emptyMap(),
            )
        }
        assertThat(error.field).isEqualTo("destinations")
    }

    @Test
    fun applyDelete_movesMoneyWritesHistoryAndRenormalizesStandingSplit() {
        val state = seededState()
        val next = service.applyDelete(
            state = state,
            deletedGoalId = DemoData.DEMO_CAR_GOAL_ID,
            percentages = mapOf(DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal.ONE),
            nowIso = now,
            entryId = "dddddddd-dddd-dddd-dddd-dddddddddddd",
        )

        assertThat(next.goals).hasSize(1)
        val emergency = next.goals.first()
        assertThat(emergency.id).isEqualTo(DemoData.DEMO_EMERGENCY_GOAL_ID)
        assertThat(emergency.savedAmount).isEqualTo(10_000_000L) // 4M + 6M
        assertThat(emergency.shareOfNewCredits).isEqualTo(1.0)

        assertThat(next.standingSplits).hasSize(1)
        assertThat(next.standingSplits.first().percentage).isEqualTo(1.0)

        val entry = next.history.last()
        assertThat(entry.type).isEqualTo(HistoryEntryType.GoalDeleted)
        assertThat(entry.isLocked).isTrue()
        assertThat(entry.deletedGoalName).isEqualTo("Car")
        assertThat(entry.releasedAmount).isEqualTo(6_000_000L)
        assertThat(entry.allocations.sumOf { it.amount }).isEqualTo(6_000_000L)
    }

    @Test
    fun applyDelete_customSplitAmongTwoRemaining() {
        val third = seededGoals()[1].copy(
            id = "cccccccc-cccc-cccc-cccc-cccccccccccc",
            name = "House",
            savedAmount = 0L,
            shareOfNewCredits = 0.0,
        )
        val state = seededState().copy(
            goals = seededGoals() + third,
            standingSplits = listOf(
                StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.50),
                StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.30),
                StandingSplit(third.id, 0.20),
            ),
        )
        val next = service.applyDelete(
            state = state,
            deletedGoalId = DemoData.DEMO_CAR_GOAL_ID,
            percentages = mapOf(
                DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal("0.60"),
                third.id to BigDecimal("0.40"),
            ),
            nowIso = now,
        )

        val emergency = next.goals.first { it.id == DemoData.DEMO_EMERGENCY_GOAL_ID }
        val house = next.goals.first { it.id == third.id }
        assertThat(emergency.savedAmount).isEqualTo(4_000_000L + 3_600_000L)
        assertThat(house.savedAmount).isEqualTo(2_400_000L)
        assertThat(standingSplitService.isValidHundred(next.standingSplits)).isTrue()
        // Prior survivors 0.30 + 0.20 = 0.50 → 0.60 / 0.40 after renormalize
        assertThat(next.standingSplits.first { it.goalId == DemoData.DEMO_EMERGENCY_GOAL_ID }.percentage)
            .isEqualTo(0.6)
        assertThat(next.standingSplits.first { it.goalId == third.id }.percentage)
            .isEqualTo(0.4)
    }

    @Test
    fun applyDelete_onlyGoalRequiresReplacement() {
        val only = seededGoals().first().copy(shareOfNewCredits = 1.0, savedAmount = 10_000_000L)
        val state = AppState(
            goals = listOf(only),
            standingSplits = listOf(StandingSplit(only.id, 1.0)),
        )
        val error = assertThrows(DeleteGoalException::class.java) {
            service.applyDelete(
                state = state,
                deletedGoalId = only.id,
                percentages = emptyMap(),
                nowIso = now,
            )
        }
        assertThat(error.field).isEqualTo("destinations")
    }

    @Test
    fun applyDelete_withReplacement_resetsStandingSplitToEqualWhenRequested() {
        val only = seededGoals().first().copy(shareOfNewCredits = 1.0, savedAmount = 10_000_000L)
        val replacement = Goal(
            id = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
            name = "Vacation",
            targetAmount = 5_000_000L,
            startDate = "2023-11-14",
            endDate = "2024-11-14",
            savedAmount = 0L,
            shareOfNewCredits = 0.0,
            createdAt = now,
            updatedAt = now,
        )
        val state = AppState(
            goals = listOf(only),
            standingSplits = listOf(StandingSplit(only.id, 1.0)),
        )
        val next = service.applyDelete(
            state = state,
            deletedGoalId = only.id,
            percentages = mapOf(replacement.id to BigDecimal.ONE),
            nowIso = now,
            replacementGoal = replacement,
            resetStandingSplitToEqual = true,
        )

        assertThat(next.goals).hasSize(1)
        assertThat(next.goals.first().id).isEqualTo(replacement.id)
        assertThat(next.goals.first().savedAmount).isEqualTo(10_000_000L)
        assertThat(next.goals.first().shareOfNewCredits).isEqualTo(1.0)
        assertThat(next.history.last().type).isEqualTo(HistoryEntryType.GoalDeleted)
    }

    @Test
    fun applyDelete_midDeleteCreate_resetsStandingToEqualAcrossRemainingPlusNew() {
        val state = seededState()
        val replacement = Goal(
            id = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
            name = "Vacation",
            targetAmount = 5_000_000L,
            startDate = "2023-11-14",
            endDate = "2024-11-14",
            savedAmount = 0L,
            shareOfNewCredits = 0.0,
            createdAt = now,
            updatedAt = now,
        )
        val next = service.applyDelete(
            state = state,
            deletedGoalId = DemoData.DEMO_CAR_GOAL_ID,
            percentages = mapOf(
                DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal("0.50"),
                replacement.id to BigDecimal("0.50"),
            ),
            nowIso = now,
            replacementGoal = replacement,
            resetStandingSplitToEqual = true,
        )

        assertThat(next.goals).hasSize(2)
        assertThat(standingSplitService.isValidHundred(next.standingSplits)).isTrue()
        assertThat(next.standingSplits.map { it.percentage }.toSet()).isEqualTo(setOf(0.5))
    }

    private fun seededGoals(): List<Goal> {
        return listOf(
            Goal(
                id = DemoData.DEMO_CAR_GOAL_ID,
                name = "Car",
                targetAmount = 50_000_000L,
                startDate = "2023-11-14",
                endDate = "2024-11-14",
                savedAmount = 6_000_000L,
                shareOfNewCredits = 0.6,
                createdAt = now,
                updatedAt = now,
            ),
            Goal(
                id = DemoData.DEMO_EMERGENCY_GOAL_ID,
                name = "Emergency Fund",
                targetAmount = 20_000_000L,
                startDate = "2023-11-14",
                endDate = "2024-11-14",
                savedAmount = 4_000_000L,
                shareOfNewCredits = 0.4,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private fun seededState(): AppState {
        val goals = seededGoals()
        return AppState(
            goals = goals,
            standingSplits = listOf(
                StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.6),
                StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.4),
            ),
            hasCompletedSetup = true,
        )
    }
}
