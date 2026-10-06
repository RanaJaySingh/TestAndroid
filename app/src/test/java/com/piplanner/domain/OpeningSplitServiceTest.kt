package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.util.DemoData
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class OpeningSplitServiceTest {

    private lateinit var service: OpeningSplitService
    private val createdAt = Instant.parse("2023-11-14T22:13:20Z")

    @Before
    fun setUp() {
        service = OpeningSplitService()
    }

    // MARK: - 100% validation (BR-2 / R22)

    @Test
    fun multiGoalPercentsMustSumToExactly100ToBeValid() {
        assertThat(
            service.isValidHundredPercent(
                listOf(BigDecimal("0.60"), BigDecimal("0.30")),
            ),
        ).isFalse()
        assertThat(
            service.isValidHundredPercent(
                listOf(BigDecimal("0.60"), BigDecimal("0.40")),
            ),
        ).isTrue()
        assertThat(service.isValidHundredPercent(emptyList())).isFalse()
    }

    @Test
    fun shortfallMessageWhenBelowHundred() {
        val message = service.shortfallMessage(
            listOf(BigDecimal("0.60"), BigDecimal("0.20")),
        )
        assertThat(message).isEqualTo("Total 80%. Assign the remaining 20%.")
    }

    @Test
    fun shortfallMessageNilWhenExactlyHundred() {
        assertThat(service.shortfallMessage(listOf(BigDecimal("1.0")))).isNull()
    }

    @Test
    fun singleGoalAutoHundredPercent() {
        val map = service.singleGoalPercentages(DemoData.DEMO_CAR_GOAL_ID)
        assertThat(map[DemoData.DEMO_CAR_GOAL_ID]).isEqualTo(BigDecimal.ONE)
        assertThat(service.isValidHundredPercent(map.values.toList())).isTrue()
    }

    // MARK: - History entry creation (R6 / BR-3)

    @Test
    fun createLockedOpeningEntryAllocationsAndLock() {
        val goals = sampleGoals()
        val percentages = mapOf(
            DemoData.DEMO_CAR_GOAL_ID to BigDecimal("0.60"),
            DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal("0.40"),
        )
        val entryId = "cccccccc-cccc-cccc-cccc-cccccccccccc"

        val entry = service.createLockedOpeningEntry(
            goals = goals,
            openingBalance = 10_000_000L,
            percentages = percentages,
            id = entryId,
            createdAt = createdAt.toString(),
        )

        assertThat(entry.id).isEqualTo(entryId)
        assertThat(entry.type).isEqualTo(HistoryEntryType.OpeningBalance)
        assertThat(entry.isLocked).isTrue()
        assertThat(entry.creditAmount).isEqualTo(10_000_000L)
        assertThat(entry.newBalance).isEqualTo(10_000_000L)
        assertThat(entry.allocations).hasSize(2)
        assertThat(entry.allocations.sumOf { it.amount }).isEqualTo(10_000_000L)

        val car = entry.allocations.first { it.goalId == DemoData.DEMO_CAR_GOAL_ID }
        val emergency = entry.allocations.first { it.goalId == DemoData.DEMO_EMERGENCY_GOAL_ID }
        assertThat(car.amount).isEqualTo(6_000_000L)
        assertThat(emergency.amount).isEqualTo(4_000_000L)
        assertThat(car.goalName).isEqualTo("Car")
        assertThat(emergency.goalName).isEqualTo("Emergency Fund")
        assertThat(car.percentage).isEqualTo(0.60)
        assertThat(emergency.percentage).isEqualTo(0.40)
    }

    @Test
    fun createLockedOpeningEntryRejectsWhenNotHundred() {
        val goals = sampleGoals()
        val error = assertThrows(OpeningSplitException::class.java) {
            service.createLockedOpeningEntry(
                goals = goals,
                openingBalance = 10_000_000L,
                percentages = mapOf(
                    DemoData.DEMO_CAR_GOAL_ID to BigDecimal("0.50"),
                    DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal("0.30"),
                ),
                createdAt = createdAt.toString(),
            )
        }
        assertThat(error.field).isEqualTo("percentages")
        assertThat(error.message).contains("Total 80%")
    }

    @Test
    fun applyOpeningLockUpdatesGoalsStandingSplitsAndHistory() {
        val goals = sampleGoals()
        val entry = service.createLockedOpeningEntry(
            goals = goals,
            openingBalance = 10_000_000L,
            percentages = mapOf(
                DemoData.DEMO_CAR_GOAL_ID to BigDecimal("0.60"),
                DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal("0.40"),
            ),
            createdAt = createdAt.toString(),
        )

        val state = AppState(goals = goals)
        val next = service.applyOpeningLock(state, entry, nowIso = createdAt.toString())

        assertThat(next.history).hasSize(1)
        assertThat(next.history[0].type).isEqualTo(HistoryEntryType.OpeningBalance)
        assertThat(next.history[0].isLocked).isTrue()
        assertThat(next.hasCompletedSetup).isTrue()

        val car = next.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }
        val emergency = next.goals.first { it.id == DemoData.DEMO_EMERGENCY_GOAL_ID }
        assertThat(car.savedAmount).isEqualTo(6_000_000L)
        assertThat(emergency.savedAmount).isEqualTo(4_000_000L)
        assertThat(car.shareOfNewCredits).isEqualTo(0.60)
        assertThat(emergency.shareOfNewCredits).isEqualTo(0.40)
        assertThat(next.standingSplits).hasSize(2)
        assertThat(next.standingSplits.map { it.percentage }.toSet()).isEqualTo(setOf(0.60, 0.40))
    }

    @Test
    fun applyOpeningLockIsRejectedWhenOpeningAlreadyLocked() {
        val goals = sampleGoals()
        val entry = service.createLockedOpeningEntry(
            goals = goals,
            openingBalance = 10_000_000L,
            percentages = mapOf(
                DemoData.DEMO_CAR_GOAL_ID to BigDecimal("0.60"),
                DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal("0.40"),
            ),
            createdAt = createdAt.toString(),
        )
        val state = service.applyOpeningLock(AppState(goals = goals), entry, createdAt.toString())

        val error = assertThrows(OpeningSplitException::class.java) {
            service.applyOpeningLock(state, entry, createdAt.toString())
        }
        assertThat(error.message).contains("already locked")
    }

    @Test
    fun lockedAmountsCaptionMatchesTicketCopy() {
        assertThat(OpeningSplitService.LOCKED_AMOUNTS_CAPTION)
            .isEqualTo("Locked amounts never change")
    }

    @Test
    fun allocatePaisaSumsExactlyForAwkwardFractions() {
        val amounts = service.allocatePaisa(
            total = 100L,
            fractions = listOf(
                BigDecimal("0.3333"),
                BigDecimal("0.3333"),
                BigDecimal("0.3334"),
            ),
        )
        assertThat(amounts.sum()).isEqualTo(100L)
    }

    private fun sampleGoals(): List<Goal> = DemoData.sampleOpeningSplitGoals(createdAt)
}
