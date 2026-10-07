package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.util.DemoData
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class TransferServiceTest {

    private lateinit var service: TransferService

    private val carId = DemoData.DEMO_CAR_GOAL_ID
    private val emergencyId = DemoData.DEMO_EMERGENCY_GOAL_ID
    private val createdAt = "2023-11-14T12:30:00Z"
    private val nowIso = "2023-11-14T12:30:01Z"
    private val entryId = "dddddddd-dddd-dddd-dddd-dddddddddddd"

    @Before
    fun setUp() {
        service = TransferService()
    }

    @Test
    fun overAmountIsInvalidAndCannotMove() {
        val goals = sampleGoals(savedCar = 100_000L, savedEmergency = 200_000L)
        val result = service.validate(
            goals = goals,
            fromGoalId = carId,
            toGoalId = emergencyId,
            amountPaisa = 200_000L,
        )
        assertThat(result).isInstanceOf(TransferValidation.Invalid::class.java)
        val invalid = result as TransferValidation.Invalid
        assertThat(invalid.phase).isEqualTo(TransferPhase.OverAmount)
        assertThat(invalid.message).isEqualTo(TransferService.OVER_AMOUNT_MESSAGE)
        assertThat(service.canMove(goals, carId, emergencyId, 200_000L)).isFalse()
    }

    @Test
    fun sameGoalIsInvalid() {
        val goals = sampleGoals(savedCar = 500_000L, savedEmergency = 200_000L)
        val result = service.validate(goals, carId, carId, 100_000L)
        assertThat(result).isInstanceOf(TransferValidation.Invalid::class.java)
        assertThat((result as TransferValidation.Invalid).phase).isEqualTo(TransferPhase.Select)
        assertThat(service.canMove(goals, carId, carId, 100_000L)).isFalse()
    }

    @Test
    fun zeroAmountIsEnterAmountPhase() {
        val goals = sampleGoals(savedCar = 500_000L, savedEmergency = 200_000L)
        val result = service.validate(goals, carId, emergencyId, 0L)
        assertThat((result as TransferValidation.Invalid).phase).isEqualTo(TransferPhase.EnterAmount)
        assertThat(service.canMove(goals, carId, emergencyId, 0L)).isFalse()
    }

    @Test
    fun validTransferCreatesHistoryAndMovesSaved() {
        val standing = listOf(
            StandingSplit(goalId = carId, percentage = 0.60),
            StandingSplit(goalId = emergencyId, percentage = 0.40),
        )
        val state = sampleState(
            savedCar = 600_000L,
            savedEmergency = 400_000L,
            standing = standing,
        )
        val next = service.applyTransfer(
            state = state,
            fromGoalId = carId,
            toGoalId = emergencyId,
            amountPaisa = 100_000L,
            id = entryId,
            createdAt = createdAt,
            nowIso = nowIso,
        )

        assertThat(next.goals.first { it.id == carId }.savedAmount).isEqualTo(500_000L)
        assertThat(next.goals.first { it.id == emergencyId }.savedAmount).isEqualTo(500_000L)

        val entry = next.history.last()
        assertThat(entry.id).isEqualTo(entryId)
        assertThat(entry.type).isEqualTo(HistoryEntryType.Transfer)
        assertThat(entry.isLocked).isTrue()
        assertThat(entry.fromGoalId).isEqualTo(carId)
        assertThat(entry.toGoalId).isEqualTo(emergencyId)
        assertThat(entry.transferAmount).isEqualTo(100_000L)
    }

    @Test
    fun standingSplitUnchangedAfterTransfer() {
        val standing = listOf(
            StandingSplit(goalId = carId, percentage = 0.70),
            StandingSplit(goalId = emergencyId, percentage = 0.30),
        )
        val state = sampleState(
            savedCar = 1_000_000L,
            savedEmergency = 500_000L,
            standing = standing,
        )
        val beforeShares = state.goals.associate { it.id to it.shareOfNewCredits }
        val next = service.applyTransfer(
            state = state,
            fromGoalId = carId,
            toGoalId = emergencyId,
            amountPaisa = 250_000L,
            id = entryId,
            createdAt = createdAt,
            nowIso = nowIso,
        )

        assertThat(next.standingSplits).isEqualTo(standing)
        assertThat(next.goals.associate { it.id to it.shareOfNewCredits }).isEqualTo(beforeShares)
    }

    @Test
    fun resolvePhaseCoversSelectEnterPreviewOverComplete() {
        val goals = sampleGoals(savedCar = 100_000L, savedEmergency = 50_000L)
        assertThat(service.resolvePhase(goals, null, emergencyId, 0L, false))
            .isEqualTo(TransferPhase.Select)
        assertThat(service.resolvePhase(goals, carId, emergencyId, 0L, false))
            .isEqualTo(TransferPhase.EnterAmount)
        assertThat(service.resolvePhase(goals, carId, emergencyId, 50_000L, false))
            .isEqualTo(TransferPhase.Preview)
        assertThat(service.resolvePhase(goals, carId, emergencyId, 200_000L, false))
            .isEqualTo(TransferPhase.OverAmount)
        assertThat(service.resolvePhase(goals, carId, emergencyId, 50_000L, true))
            .isEqualTo(TransferPhase.Complete)
    }

    @Test
    fun applyTransferRejectsOverAmount() {
        val state = sampleState(savedCar = 50_000L, savedEmergency = 50_000L)
        assertThrows(TransferException::class.java) {
            service.applyTransfer(
                state = state,
                fromGoalId = carId,
                toGoalId = emergencyId,
                amountPaisa = 100_000L,
                id = entryId,
                createdAt = createdAt,
                nowIso = nowIso,
            )
        }
    }

    @Test
    fun amountChipsMatchDesign() {
        assertThat(TransferService.AMOUNT_CHIP_PAISA).containsExactly(
            100_000L,
            500_000L,
            1_000_000L,
        ).inOrder()
    }

    private fun sampleGoals(savedCar: Long, savedEmergency: Long): List<Goal> {
        return DemoData.sampleOpeningSplitGoals().map { goal ->
            when (goal.id) {
                carId -> goal.copy(savedAmount = savedCar, shareOfNewCredits = 0.6)
                emergencyId -> goal.copy(savedAmount = savedEmergency, shareOfNewCredits = 0.4)
                else -> goal
            }
        }
    }

    private fun sampleState(
        savedCar: Long,
        savedEmergency: Long,
        standing: List<StandingSplit> = listOf(
            StandingSplit(goalId = carId, percentage = 0.6),
            StandingSplit(goalId = emergencyId, percentage = 0.4),
        ),
    ): AppState {
        return AppState(
            accounts = listOf(
                Account(
                    id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
                    bankName = DemoData.SAVINGS_BANK,
                    maskedNumber = DemoData.SAVINGS_MASKED,
                    balance = savedCar + savedEmergency,
                    isDedicated = true,
                    isPaytmLinked = true,
                    consentAutoUpdate = true,
                ),
            ),
            goals = sampleGoals(savedCar, savedEmergency),
            standingSplits = standing,
            hasCompletedSetup = true,
        )
    }
}
