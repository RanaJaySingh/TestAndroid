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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class WithdrawalServiceTest {

    private lateinit var service: WithdrawalService
    private lateinit var formatting: FormattingService
    private val nowIso = Instant.parse("2024-06-01T12:00:00Z").toString()

    @Before
    fun setUp() {
        service = WithdrawalService(OpeningSplitService())
        formatting = FormattingService()
    }

    @Test
    fun proportionalDefaultsMatchShortfallExactly() {
        val goals = sampleGoals(
            carSaved = 6_000_000L,
            emergencySaved = 4_000_000L,
        )
        val shortfall = 1_500_000L
        val reductions = service.defaultReductions(goals, shortfall)

        assertThat(service.totalReductions(reductions)).isEqualTo(shortfall)
        // 60/40 of 15000 = 9000 / 6000 rupees
        assertThat(reductions[DemoData.DEMO_CAR_GOAL_ID]).isEqualTo(900_000L)
        assertThat(reductions[DemoData.DEMO_EMERGENCY_GOAL_ID]).isEqualTo(600_000L)
        assertThat(service.hasNoGoalBelowZero(goals, reductions)).isTrue()
        assertThat(service.canSaveAndLock(goals, reductions, shortfall)).isTrue()
    }

    @Test
    fun proportionalUsesLargestRemainderForOddShortfall() {
        val goals = sampleGoals(
            carSaved = 6_000_000L,
            emergencySaved = 4_000_000L,
        )
        val shortfall = 100L // 1 rupee — odd paisa split
        val reductions = service.defaultReductions(goals, shortfall)
        assertThat(service.totalReductions(reductions)).isEqualTo(100L)
        assertThat(reductions.values.all { it >= 0L }).isTrue()
    }

    @Test
    fun sumNotEqualShortfallDisablesSave() {
        val goals = sampleGoals()
        val shortfall = 1_000_000L
        val bad = mapOf(
            DemoData.DEMO_CAR_GOAL_ID to 400_000L,
            DemoData.DEMO_EMERGENCY_GOAL_ID to 400_000L,
        )
        assertThat(service.isTotalExact(bad, shortfall)).isFalse()
        assertThat(service.canSaveAndLock(goals, bad, shortfall)).isFalse()
        val message = service.statusMessage(goals, bad, shortfall, formatting)
        assertThat(message).contains("Assign the remaining")
    }

    @Test
    fun reductionAboveSavedBlocksBelowZero() {
        val goals = sampleGoals(
            carSaved = 500_000L,
            emergencySaved = 4_000_000L,
        )
        val shortfall = 1_000_000L
        val bad = mapOf(
            DemoData.DEMO_CAR_GOAL_ID to 600_000L,
            DemoData.DEMO_EMERGENCY_GOAL_ID to 400_000L,
        )
        assertThat(service.hasNoGoalBelowZero(goals, bad)).isFalse()
        assertThat(service.canSaveAndLock(goals, bad, shortfall)).isFalse()
        val message = service.statusMessage(goals, bad, shortfall, formatting)
        assertThat(message).contains("below ₹0")
    }

    @Test
    fun applyWithdrawalCreatesLockedHistoryAndUpdatesBalances() {
        val goals = sampleGoals(
            carSaved = 6_000_000L,
            emergencySaved = 4_000_000L,
        )
        val previous = 10_000_000L
        val newBalance = 8_500_000L
        val shortfall = previous - newBalance
        val reductions = service.defaultReductions(goals, shortfall)
        val state = AppState(
            accounts = listOf(dedicatedAccount(previous)),
            goals = goals,
            standingSplits = listOf(
                StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.6),
                StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.4),
            ),
            hasCompletedSetup = true,
        )

        val next = service.applyWithdrawal(
            state = state,
            previousBalance = previous,
            newBalance = newBalance,
            reductions = reductions,
            dedicatedAccountId = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
            nowIso = nowIso,
            id = "wd-1",
        )

        val entry = next.history.single()
        assertThat(entry.type).isEqualTo(HistoryEntryType.Withdrawal)
        assertThat(entry.isLocked).isTrue()
        assertThat(entry.withdrawalAmount).isEqualTo(shortfall)
        assertThat(entry.allocations.sumOf { it.amount }).isEqualTo(shortfall)
        assertThat(next.accounts.single().balance).isEqualTo(newBalance)
        assertThat(next.goals.sumOf { it.savedAmount }).isEqualTo(newBalance)
        // Standing split unchanged
        assertThat(next.standingSplits).isEqualTo(state.standingSplits)
    }

    @Test
    fun applyWithdrawalRejectsInvalidTotal() {
        val goals = sampleGoals()
        val state = AppState(
            accounts = listOf(dedicatedAccount(10_000_000L)),
            goals = goals,
            hasCompletedSetup = true,
        )
        assertThrows(WithdrawalException::class.java) {
            service.applyWithdrawal(
                state = state,
                previousBalance = 10_000_000L,
                newBalance = 9_000_000L,
                reductions = mapOf(
                    DemoData.DEMO_CAR_GOAL_ID to 100_000L,
                    DemoData.DEMO_EMERGENCY_GOAL_ID to 100_000L,
                ),
                dedicatedAccountId = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
                nowIso = nowIso,
            )
        }
    }

    private fun dedicatedAccount(balance: Long): Account = Account(
        id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
        bankName = DemoData.SAVINGS_BANK,
        maskedNumber = DemoData.SAVINGS_MASKED,
        balance = balance,
        isDedicated = true,
        isPaytmLinked = true,
        consentAutoUpdate = true,
    )

    private fun sampleGoals(
        carSaved: Long = 6_000_000L,
        emergencySaved: Long = 4_000_000L,
    ): List<Goal> {
        val now = Instant.parse("2024-01-01T00:00:00Z")
        val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val end = start.plusYears(1)
        val createdAt = now.toString()
        return listOf(
            Goal(
                id = DemoData.DEMO_CAR_GOAL_ID,
                name = "Car",
                targetAmount = 50_000_000L,
                startDate = start.toString(),
                endDate = end.toString(),
                savedAmount = carSaved,
                shareOfNewCredits = 0.6,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
            Goal(
                id = DemoData.DEMO_EMERGENCY_GOAL_ID,
                name = "Emergency Fund",
                targetAmount = 20_000_000L,
                startDate = start.toString(),
                endDate = end.toString(),
                savedAmount = emergencySaved,
                shareOfNewCredits = 0.4,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
        )
    }
}
