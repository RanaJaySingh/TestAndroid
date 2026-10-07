package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.util.DemoData
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class StandingSplitServiceTest {

    private lateinit var service: StandingSplitService
    private val nowIso = Instant.parse("2023-11-14T22:13:20Z").toString()

    @Before
    fun setUp() {
        service = StandingSplitService(OpeningSplitService())
    }

    // MARK: - 100% validation (BR-2 / R22 / R12)

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
    fun shortfallMessageWhenAboveHundred() {
        val message = service.shortfallMessage(
            listOf(BigDecimal("0.70"), BigDecimal("0.40")),
        )
        assertThat(message).isEqualTo("Total 110%. Reduce by 10%.")
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

    @Test
    fun shouldSkipUiWhenOneOrZeroGoals() {
        assertThat(service.shouldSkipUi(emptyList())).isTrue()
        assertThat(service.shouldSkipUi(sampleGoals().take(1))).isTrue()
        assertThat(service.shouldSkipUi(sampleGoals())).isFalse()
    }

    // MARK: - Persistence / next credit

    @Test
    fun applyStandingSplitUpdatesSharesWithoutTouchingSavedOrHistory() {
        val goals = sampleGoalsWithSaved()
        val history = listOf(
            HistoryEntry(
                id = "hist-1",
                type = HistoryEntryType.OpeningBalance,
                createdAt = nowIso,
                isLocked = true,
                creditAmount = 10_000_000L,
                newBalance = 10_000_000L,
            ),
        )
        val state = AppState(
            goals = goals,
            standingSplits = listOf(
                StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.6),
                StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.4),
            ),
            history = history,
            hasCompletedSetup = true,
        )

        val next = service.applyStandingSplit(
            state = state,
            percentages = mapOf(
                DemoData.DEMO_CAR_GOAL_ID to BigDecimal("0.50"),
                DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal("0.50"),
            ),
            nowIso = nowIso,
        )

        assertThat(next.standingSplits).hasSize(2)
        assertThat(next.standingSplits.first { it.goalId == DemoData.DEMO_CAR_GOAL_ID }.percentage)
            .isEqualTo(0.50)
        assertThat(next.standingSplits.first { it.goalId == DemoData.DEMO_EMERGENCY_GOAL_ID }.percentage)
            .isEqualTo(0.50)
        assertThat(next.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.shareOfNewCredits)
            .isEqualTo(0.50)
        assertThat(next.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.savedAmount)
            .isEqualTo(6_000_000L)
        assertThat(next.goals.first { it.id == DemoData.DEMO_EMERGENCY_GOAL_ID }.savedAmount)
            .isEqualTo(4_000_000L)
        assertThat(next.history).isEqualTo(history)
    }

    @Test
    fun applyStandingSplitRejectsWhenNotHundred() {
        val error = assertThrows(StandingSplitException::class.java) {
            service.applyStandingSplit(
                state = AppState(goals = sampleGoals()),
                percentages = mapOf(
                    DemoData.DEMO_CAR_GOAL_ID to BigDecimal("0.50"),
                    DemoData.DEMO_EMERGENCY_GOAL_ID to BigDecimal("0.30"),
                ),
                nowIso = nowIso,
            )
        }
        assertThat(error.field).isEqualTo("percentages")
        assertThat(error.message).contains("Total 80%")
    }

    @Test
    fun fractionsForNextCreditUsesPersistedStandingSplit() {
        val state = AppState(
            goals = sampleGoals(),
            standingSplits = listOf(
                StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.25),
                StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.75),
            ),
        )
        val fractions = service.fractionsForNextCredit(state)
        assertThat(fractions[DemoData.DEMO_CAR_GOAL_ID]).isEqualTo(BigDecimal("0.2500"))
        assertThat(fractions[DemoData.DEMO_EMERGENCY_GOAL_ID]).isEqualTo(BigDecimal("0.7500"))
        assertThat(service.isValidHundredPercent(fractions.values.toList())).isTrue()
    }

    @Test
    fun applySingleGoalSkipPersistsHundredPercent() {
        val goal = sampleGoals().first()
        val next = service.applySingleGoalSkip(
            state = AppState(goals = listOf(goal)),
            nowIso = nowIso,
        )
        assertThat(next.standingSplits).hasSize(1)
        assertThat(next.standingSplits.first().percentage).isEqualTo(1.0)
        assertThat(next.goals.first().shareOfNewCredits).isEqualTo(1.0)
    }

    @Test
    fun savedMoneyStaysPutCopy() {
        assertThat(StandingSplitService.SAVED_MONEY_STAYS_PUT)
            .isEqualTo("Saved money stays put")
    }

    // MARK: - Equal / renormalize (PIP-54 delete)

    @Test
    fun equalSplits_twoGoals_sumToOne() {
        val splits = service.equalSplits(
            listOf(DemoData.DEMO_CAR_GOAL_ID, DemoData.DEMO_EMERGENCY_GOAL_ID),
        )
        assertThat(splits).hasSize(2)
        assertThat(service.isValidHundred(splits)).isTrue()
        assertThat(splits[0].percentage).isEqualTo(0.5)
        assertThat(splits[1].percentage).isEqualTo(0.5)
    }

    @Test
    fun equalSplits_threeGoals_largestRemainder() {
        val splits = service.equalSplits(listOf("a", "b", "c"))
        assertThat(service.isValidHundred(splits)).isTrue()
        val percents = splits.map { (it.percentage * 100).toInt() }
        assertThat(percents.sum()).isEqualTo(100)
        assertThat(percents).containsExactly(34, 33, 33)
    }

    @Test
    fun renormalizeAfterRemoving_proportionalAmongSurvivors() {
        val splits = listOf(
            StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.60),
            StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.30),
            StandingSplit("cccccccc-cccc-cccc-cccc-cccccccccccc", 0.10),
        )
        val next = service.renormalizeAfterRemoving(splits, DemoData.DEMO_CAR_GOAL_ID)
        assertThat(next).hasSize(2)
        assertThat(service.isValidHundred(next)).isTrue()
        // 0.30 / 0.40 = 0.75, 0.10 / 0.40 = 0.25
        val emergency = next.first { it.goalId == DemoData.DEMO_EMERGENCY_GOAL_ID }
        val other = next.first { it.goalId == "cccccccc-cccc-cccc-cccc-cccccccccccc" }
        assertThat(emergency.percentage).isWithin(1e-9).of(0.75)
        assertThat(other.percentage).isWithin(1e-9).of(0.25)
    }

    @Test
    fun renormalizeAfterRemoving_singleSurvivorBecomesHundred() {
        val splits = listOf(
            StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.60),
            StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.40),
        )
        val next = service.renormalizeAfterRemoving(splits, DemoData.DEMO_CAR_GOAL_ID)
        assertThat(next).hasSize(1)
        assertThat(next.first().goalId).isEqualTo(DemoData.DEMO_EMERGENCY_GOAL_ID)
        assertThat(next.first().percentage).isEqualTo(1.0)
    }

    @Test
    fun resetToEqual_matchesEqualSplits() {
        val ids = listOf(DemoData.DEMO_CAR_GOAL_ID, DemoData.DEMO_EMERGENCY_GOAL_ID)
        assertThat(service.resetToEqual(ids)).isEqualTo(service.equalSplits(ids))
    }

    private fun sampleGoals(): List<Goal> = DemoData.sampleOpeningSplitGoals()

    private fun sampleGoalsWithSaved(): List<Goal> {
        return sampleGoals().map { goal ->
            when (goal.id) {
                DemoData.DEMO_CAR_GOAL_ID -> goal.copy(savedAmount = 6_000_000L)
                DemoData.DEMO_EMERGENCY_GOAL_ID -> goal.copy(savedAmount = 4_000_000L)
                else -> goal
            }
        }
    }
}
