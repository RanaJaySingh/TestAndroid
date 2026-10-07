package com.piplanner.data.model

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.AppStateJsonSerializer
import org.junit.Test

class ModelSerializationTest {

    private val serializer = AppStateJsonSerializer()

    @Test
    fun appState_roundTripsAllSharedModels() {
        val state = AppState(
            accounts = listOf(
                Account(
                    id = "acc-hdfc",
                    bankName = "HDFC",
                    maskedNumber = "••4821",
                    balance = 10_000_000L,
                    isDedicated = true,
                    isPaytmLinked = true,
                    consentAutoUpdate = true,
                ),
                Account(
                    id = "acc-sbi",
                    bankName = "SBI",
                    maskedNumber = "••7730",
                    balance = 7_200_000L,
                    isDedicated = false,
                    isPaytmLinked = false,
                    consentAutoUpdate = false,
                ),
            ),
            goals = listOf(
                Goal(
                    id = "goal-car",
                    name = "Car",
                    targetAmount = 6_000_000L,
                    startDate = "2026-01-01",
                    endDate = "2027-01-01",
                    inflationRate = 0.07,
                    savedAmount = 6_000_000L,
                    shareOfNewCredits = 0.6,
                    createdAt = "2026-01-01T00:00:00Z",
                    updatedAt = "2026-01-01T00:00:00Z",
                ),
            ),
            history = listOf(
                HistoryEntry(
                    id = "hist-1",
                    type = HistoryEntryType.OpeningBalance,
                    createdAt = "2026-01-01T00:00:00Z",
                    isLocked = true,
                    previousBalance = null,
                    newBalance = 10_000_000L,
                    creditAmount = 10_000_000L,
                    allocations = listOf(
                        GoalAllocation(
                            goalId = "goal-car",
                            goalName = "Car",
                            amount = 6_000_000L,
                            percentage = 0.6,
                        ),
                        GoalAllocation(
                            goalId = "goal-ef",
                            goalName = "Emergency Fund",
                            amount = 4_000_000L,
                            percentage = 0.4,
                        ),
                    ),
                ),
            ),
            standingSplits = listOf(
                StandingSplit(goalId = "goal-car", percentage = 0.6),
                StandingSplit(goalId = "goal-ef", percentage = 0.4),
            ),
            hasCompletedSetup = true,
            heldGoalChanges = listOf(
                HeldGoalChange(
                    goalId = "goal-car",
                    savedAt = "2026-06-01T12:00:00Z",
                ),
            ),
        )

        val encoded = serializer.encode(state)
        val decoded = serializer.decode(encoded)

        assertThat(decoded).isEqualTo(state)
        assertThat(decoded.accounts[0].balance).isEqualTo(10_000_000L)
        assertThat(decoded.goals[0].inflationRate).isEqualTo(0.07)
        assertThat(decoded.history[0].type).isEqualTo(HistoryEntryType.OpeningBalance)
        assertThat(decoded.standingSplits.sumOf { it.percentage }).isEqualTo(1.0)
        assertThat(decoded.heldGoalChanges).hasSize(1)
        assertThat(decoded.heldGoalChanges[0].goalId).isEqualTo("goal-car")
    }

    @Test
    fun emptyAppState_encodesAndDecodes() {
        val encoded = serializer.encode(AppState.EMPTY)
        val decoded = serializer.decode(encoded)
        assertThat(decoded).isEqualTo(AppState.EMPTY)
        assertThat(decoded.accounts).isEmpty()
        assertThat(decoded.goals).isEmpty()
        assertThat(decoded.history).isEmpty()
        assertThat(decoded.standingSplits).isEmpty()
        assertThat(decoded.heldGoalChanges).isEmpty()
        assertThat(decoded.hasCompletedSetup).isFalse()
    }

    @Test
    fun historyEntryTypes_allSerialize() {
        HistoryEntryType.entries.forEach { type ->
            val entry = HistoryEntry(
                id = "id-$type",
                type = type,
                createdAt = "2026-01-01T00:00:00Z",
                isLocked = false,
            )
            val state = AppState(history = listOf(entry))
            val decoded = serializer.decode(serializer.encode(state))
            assertThat(decoded.history.single().type).isEqualTo(type)
        }
    }
}
