package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.StandingSplit
import com.piplanner.util.DemoData
import org.junit.Test

class AskAnswerServiceTest {

    private val formatting = FormattingService()
    private val service = AskAnswerService(
        formatting = formatting,
        goalsTabService = GoalsTabService(DedicatedAccountService()),
    )

    @Test
    fun plainAnswer_usesEngineNumbersForGoals() {
        val goals = DemoData.sampleOpeningSplitGoals()
        val text = service.plainAnswer(
            query = "How am I doing?",
            goals = goals,
            standingSplits = emptyList(),
            totalSavingsPaisa = 10_000_000L,
        )

        assertThat(text).contains("₹1,00,000")
        assertThat(text).contains("Car")
        assertThat(text).contains("Emergency Fund")
        assertThat(text).contains("Needs")
    }

    @Test
    fun inflationChip_mentionsDefaultRateAndAdjustedTargets() {
        val goals = DemoData.sampleOpeningSplitGoals()
        val text = service.plainAnswer(
            query = "Why is inflation 7%?",
            goals = goals,
            standingSplits = emptyList(),
            totalSavingsPaisa = 10_000_000L,
        )

        assertThat(text).contains("7%")
        assertThat(text).contains("Car")
        assertThat(text).contains(formatting.formatInrFromPaisa(goals[0].adjustedTarget()))
    }

    @Test
    fun splitChip_explainsStandingSplit() {
        val goals = DemoData.sampleOpeningSplitGoals()
        val splits = listOf(
            StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.6),
            StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.4),
        )
        val text = service.plainAnswer(
            query = "What happens if I change the split?",
            goals = goals,
            standingSplits = splits,
            totalSavingsPaisa = 10_000_000L,
        )

        assertThat(text).contains("next credit")
        assertThat(text).contains("60%")
        assertThat(text).contains("40%")
    }

    @Test
    fun proposalSummary_transferUsesInr() {
        val goals = DemoData.sampleOpeningSplitGoals()
        val summary = service.proposalSummary(
            action = ProposedAction.Transfer(
                from = DemoData.DEMO_CAR_GOAL_ID,
                to = DemoData.DEMO_EMERGENCY_GOAL_ID,
                amount = 500_000L,
            ),
            goals = goals,
        )
        assertThat(summary).contains("Car")
        assertThat(summary).contains("Emergency Fund")
        assertThat(summary).contains("₹5,000")
    }
}
