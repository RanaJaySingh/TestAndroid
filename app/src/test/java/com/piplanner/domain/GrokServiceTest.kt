package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.util.DemoData
import org.junit.Test
import java.math.BigDecimal

class GrokServiceTest {

    @Test
    fun happyPathProposal_carAndEmergencyFund() {
        val service = StubGrokService()
        val result = service.analyzeGoalInput("I want a car and an emergency fund")

        assertThat(result.isSuccess).isTrue()
        val analysis = result.getOrNull() as GrokGoalAnalysis.Proposals
        assertThat(analysis.proposals).hasSize(2)
        assertThat(analysis.proposals[0].name).isEqualTo("Car")
        assertThat(analysis.proposals[0].sharePercentage).isEqualTo(0.6)
        assertThat(analysis.proposals[0].suggestedTarget).isEqualTo(50_000_000L)
        assertThat(analysis.proposals[1].name).isEqualTo("Emergency Fund")
        assertThat(analysis.proposals[1].sharePercentage).isEqualTo(0.4)
        assertThat(analysis.proposals[1].suggestedTarget).isEqualTo(20_000_000L)

        val shares = analysis.proposals.map { BigDecimal.valueOf(it.sharePercentage) }
        assertThat(OpeningSplitService().isValidHundredPercent(shares)).isTrue()
    }

    @Test
    fun analyzeGoals_specWrapperMatchesHappyPath() {
        val service = StubGrokService()
        val result = service.analyzeGoals("Save for a car")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.map { it.name })
            .containsExactly("Car", "Emergency Fund")
            .inOrder()
    }

    @Test
    fun vagueInput_needsClarification() {
        val service = StubGrokService()
        val result = service.analyzeGoalInput("save money")

        assertThat(result.isSuccess).isTrue()
        val analysis = result.getOrNull() as GrokGoalAnalysis.NeedsClarification
        assertThat(analysis.question).isNotEmpty()
        assertThat(StubGrokService.isVague("save money")).isTrue()
        assertThat(StubGrokService.isVague("car")).isFalse()
    }

    @Test
    fun vagueAnalyzeGoals_mapsToInvalidDraft() {
        val service = StubGrokService()
        val result = service.analyzeGoals("goals")

        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as GrokException
        assertThat(error.error).isEqualTo(GrokError.InvalidDraft)
    }

    @Test
    fun unavailableMode() {
        val service = StubGrokService(isUnavailable = true)
        val analyze = service.analyzeGoalInput("car emergency")
        assertThat(analyze.isFailure).isTrue()
        assertThat((analyze.exceptionOrNull() as GrokException).error)
            .isEqualTo(GrokError.Unavailable)

        val ask = service.askQuestion("How am I doing?")
        assertThat(ask.isFailure).isTrue()
        assertThat((ask.exceptionOrNull() as GrokException).error)
            .isEqualTo(GrokError.Unavailable)
    }

    @Test
    fun askQuestion_plainAnswerWhenAvailable() {
        val service = StubGrokService()
        val result = service.askQuestion("What is my plan?")

        assertThat(result.isSuccess).isTrue()
        val answer = result.getOrNull() as AskResponse.PlainAnswer
        assertThat(answer.text).isNotEmpty()
    }

    @Test
    fun askQuestion_chipsStayPlainAnswers() {
        val service = StubGrokService()
        StubGrokService.ASK_CHIPS.forEach { chip ->
            val result = service.askQuestion(chip)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isInstanceOf(AskResponse.PlainAnswer::class.java)
        }
    }

    @Test
    fun askQuestion_transferProposalWhenQueryMentionsTransfer() {
        val service = StubGrokService()
        val result = service.askQuestion("Please transfer ₹5,000")

        assertThat(result.isSuccess).isTrue()
        val proposal = result.getOrNull() as AskResponse.ActionProposal
        val prefill = StubGrokService.transferPrefill(proposal.action)
        assertThat(prefill).isNotNull()
        assertThat(prefill!!.fromGoalId).isEqualTo(DemoData.DEMO_CAR_GOAL_ID)
        assertThat(prefill.toGoalId).isEqualTo(DemoData.DEMO_EMERGENCY_GOAL_ID)
        assertThat(prefill.amountPaisa).isEqualTo(500_000L)
    }

    @Test
    fun askQuestion_addGoalProposalForVacation() {
        val service = StubGrokService()
        val result = service.askQuestion("Add a ₹50,000 vacation by March")

        assertThat(result.isSuccess).isTrue()
        val proposal = result.getOrNull() as AskResponse.ActionProposal
        val action = proposal.action as ProposedAction.AddGoal
        assertThat(action.proposal.name).isEqualTo("Vacation")
        assertThat(action.proposal.suggestedTarget).isEqualTo(5_000_000L)
    }

    @Test
    fun askQuestion_changeSplitProposal() {
        val service = StubGrokService()
        val result = service.askQuestion("Change my standing split to 50/50")

        assertThat(result.isSuccess).isTrue()
        val proposal = result.getOrNull() as AskResponse.ActionProposal
        assertThat(proposal.action).isInstanceOf(ProposedAction.ChangeSplit::class.java)
    }

    @Test
    fun askQuestion_invalidDraftNeverReturnsProposal() {
        val service = StubGrokService()
        val result = service.askQuestion("invalid draft please")

        assertThat(result.isFailure).isTrue()
        assertThat((result.exceptionOrNull() as GrokException).error)
            .isEqualTo(GrokError.InvalidDraft)
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun checkedByLabel_matchesDesignCopy() {
        assertThat(StubGrokService.CHECKED_BY_LABEL)
            .isEqualTo("Checked by PiPlanner. Estimate.")
    }

    @Test
    fun followUpQuestions_bounded() {
        assertThat(StubGrokService.FOLLOW_UP_QUESTIONS).hasSize(2)
        assertThat(StubGrokService.followUpQuestion(at = 0))
            .isEqualTo(StubGrokService.FOLLOW_UP_QUESTIONS[0])
        assertThat(StubGrokService.followUpQuestion(at = 1))
            .isEqualTo(StubGrokService.FOLLOW_UP_QUESTIONS[1])
    }
}
