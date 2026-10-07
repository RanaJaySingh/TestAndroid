package com.piplanner.ui.setup

import com.google.common.truth.Truth.assertThat
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalValidationService
import com.piplanner.domain.OpeningSplitService
import com.piplanner.domain.StubGrokService
import org.junit.Test

class GoalChatViewModelTest {

    private fun viewModel(grok: StubGrokService = StubGrokService()): GoalChatViewModel {
        return GoalChatViewModel(
            grok = grok,
            formatting = FormattingService(),
            validation = GoalValidationService(OpeningSplitService()),
        )
    }

    @Test
    fun concreteInput_showsProposalCard() {
        val vm = viewModel()
        vm.setDraftInput("I want a car and emergency fund")
        vm.sendDraft()

        val state = vm.uiState.value
        assertThat(state.phase).isEqualTo(GoalChatPhase.Proposal)
        assertThat(state.proposals).hasSize(2)
        assertThat(state.checkedByLabel).isEqualTo("Checked by PiPlanner. Estimate.")
    }

    @Test
    fun vagueInput_twoFollowUpsThenForm() {
        val vm = viewModel()
        vm.setDraftInput("save money")
        vm.sendDraft()
        assertThat(vm.uiState.value.phase).isEqualTo(GoalChatPhase.FollowUp)
        assertThat(vm.uiState.value.followUpCount).isEqualTo(1)

        vm.setDraftInput("help plan")
        vm.sendDraft()
        assertThat(vm.uiState.value.phase).isEqualTo(GoalChatPhase.FollowUp)
        assertThat(vm.uiState.value.followUpCount).isEqualTo(2)

        vm.setDraftInput("something")
        vm.sendDraft()
        assertThat(vm.uiState.value.phase).isEqualTo(GoalChatPhase.Form)
    }

    @Test
    fun unavailable_offersHandFormPath() {
        val vm = viewModel(StubGrokService(isUnavailable = true))
        assertThat(vm.uiState.value.phase).isEqualTo(GoalChatPhase.Unavailable)

        vm.useFormPath()
        assertThat(vm.uiState.value.phase).isEqualTo(GoalChatPhase.Form)
    }

    @Test
    fun confirmProposals_continueRequiresHundredPercent() {
        val vm = viewModel()
        vm.setDraftInput("car")
        vm.sendDraft()
        vm.confirmProposals()

        val state = vm.uiState.value
        assertThat(state.phase).isEqualTo(GoalChatPhase.GoalsDefined)
        assertThat(vm.canContinue()).isTrue()

        vm.updateShare(state.definedGoals[0].id, 50)
        assertThat(vm.canContinue()).isFalse()
        assertThat(vm.continueDisabledReason()).isNotNull()
    }

    @Test
    fun formSave_disabledUntilValid() {
        val vm = viewModel()
        vm.useFormPath()
        assertThat(vm.saveForm()).isFalse()

        vm.updateFormDraft {
            it.copy(
                name = "Car",
                targetRupeeDigits = "500000",
            )
        }
        assertThat(vm.saveForm()).isTrue()
        assertThat(vm.uiState.value.phase).isEqualTo(GoalChatPhase.GoalsDefined)
        assertThat(vm.uiState.value.definedGoals).hasSize(1)
    }
}
