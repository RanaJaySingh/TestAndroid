package com.piplanner.ui.ask

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.StandingSplit
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.AskAnswerService
import com.piplanner.domain.AskStandingSplitSeed
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalValidationService
import com.piplanner.domain.GoalsTabService
import com.piplanner.domain.GrokService
import com.piplanner.domain.OpeningSplitService
import com.piplanner.domain.ProposedAction
import com.piplanner.domain.StubGrokService
import com.piplanner.util.DemoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AskViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var standingSeed: AskStandingSplitSeed

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence(sampleState())
        standingSeed = AskStandingSplitSeed()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun chipQuestion_showsPlainAnswerWithEngineNumbers() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.selectChip("Why is inflation 5%?")
        advanceUntilIdle()

        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.PlainAnswer)
        assertThat(vm.uiState.value.plainAnswer).contains("5%")
        assertThat(vm.uiState.value.plainAnswer).contains("Car")
        assertThat(vm.uiState.value.proposedAction).isNull()
    }

    @Test
    fun transferAction_showsProposalThenConfirmPrefillsTransfer() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setDraftInput("Transfer ₹5,000 from Car to Emergency Fund")
        vm.submit()
        advanceUntilIdle()

        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Proposal)
        assertThat(vm.uiState.value.checkedByLabel)
            .isEqualTo("Checked by PiPlanner. Estimate.")
        assertThat(vm.uiState.value.proposedAction)
            .isInstanceOf(ProposedAction.Transfer::class.java)

        vm.confirmProposal()
        advanceUntilIdle()

        val prefill = vm.uiState.value.navigateTransfer
        assertThat(prefill).isNotNull()
        assertThat(prefill!!.fromGoalId).isEqualTo(DemoData.DEMO_CAR_GOAL_ID)
        assertThat(prefill.toGoalId).isEqualTo(DemoData.DEMO_EMERGENCY_GOAL_ID)
        assertThat(prefill.amountPaisa).isEqualTo(500_000L)
    }

    @Test
    fun editProposal_transferOpensSameSheetAsConfirm() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setDraftInput("Transfer ₹5,000 from Car to Emergency Fund")
        vm.submit()
        advanceUntilIdle()
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Proposal)

        vm.editProposal()
        advanceUntilIdle()

        val prefill = vm.uiState.value.navigateTransfer
        assertThat(prefill).isNotNull()
        assertThat(prefill!!.amountPaisa).isEqualTo(500_000L)
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Input)
        assertThat(vm.uiState.value.proposedAction).isNull()
        assertThat(vm.uiState.value.statusMessage).isNull()
    }

    @Test
    fun editProposal_changeSplitOpensStandingSplitWithSeed() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setDraftInput("Change my standing split to 50/50")
        vm.submit()
        advanceUntilIdle()
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Proposal)

        vm.editProposal()
        advanceUntilIdle()

        assertThat(vm.uiState.value.navigateStandingSplit).isTrue()
        val seed = standingSeed.take()
        assertThat(seed).isNotNull()
        assertThat(seed!!.map { it.percentage }).containsExactly(0.5, 0.5)
    }

    @Test
    fun changeSplit_confirmOpensStandingSplit() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setDraftInput("Change my standing split to 50/50")
        vm.submit()
        advanceUntilIdle()
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Proposal)

        vm.confirmProposal()
        advanceUntilIdle()
        assertThat(vm.uiState.value.navigateStandingSplit).isTrue()
    }

    @Test
    fun addGoal_confirmOpensGoalForm() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setDraftInput("Add a ₹50,000 vacation by March")
        vm.submit()
        advanceUntilIdle()
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Proposal)

        vm.confirmProposal()
        advanceUntilIdle()
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.GoalForm)
        assertThat(vm.uiState.value.formDraft.name).isEqualTo("Vacation")
    }

    @Test
    fun unavailable_offersFallbacks() = runTest {
        val vm = viewModel(StubGrokService(isUnavailable = true))
        advanceUntilIdle()
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Unavailable)
        assertThat(vm.uiState.value.fallbackTemplates).isNotEmpty()

        vm.useFormPath()
        advanceUntilIdle()
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.GoalForm)
    }

    @Test
    fun unavailable_templateTransferOpensPrefillWithoutCallingGrok() = runTest {
        val tracking = TrackingGrokService(StubGrokService(isUnavailable = true))
        val vm = viewModel(tracking)
        advanceUntilIdle()
        // Init probe + any prior calls; reset counter for the template tap.
        tracking.askQuestionCalls = 0

        vm.selectTemplate("Transfer ₹5,000 from Car to Emergency Fund")
        advanceUntilIdle()

        assertThat(tracking.askQuestionCalls).isEqualTo(0)
        assertThat(vm.uiState.value.navigateTransfer).isNotNull()
        assertThat(vm.uiState.value.navigateTransfer!!.amountPaisa).isEqualTo(500_000L)
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Unavailable)
    }

    @Test
    fun unavailable_templateVacationOpensGoalFormWithoutCallingGrok() = runTest {
        val tracking = TrackingGrokService(StubGrokService(isUnavailable = true))
        val vm = viewModel(tracking)
        advanceUntilIdle()
        tracking.askQuestionCalls = 0

        vm.selectTemplate("Add a ₹50,000 vacation by March")
        advanceUntilIdle()

        assertThat(tracking.askQuestionCalls).isEqualTo(0)
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.GoalForm)
        assertThat(vm.uiState.value.formDraft.name).isEqualTo("Vacation")
    }

    @Test
    fun unavailable_templateStandingOpensStandingWithoutCallingGrok() = runTest {
        val tracking = TrackingGrokService(StubGrokService(isUnavailable = true))
        val vm = viewModel(tracking)
        advanceUntilIdle()
        tracking.askQuestionCalls = 0

        vm.selectTemplate("Change the standing split")
        advanceUntilIdle()

        assertThat(tracking.askQuestionCalls).isEqualTo(0)
        assertThat(vm.uiState.value.navigateStandingSplit).isTrue()
    }

    @Test
    fun invalidDraft_askOnceThenGoalForm_neverShowsProposal() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.setDraftInput("invalid draft please")
        vm.submit()
        advanceUntilIdle()

        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.InvalidDraft)
        assertThat(vm.uiState.value.proposedAction).isNull()
        assertThat(vm.uiState.value.invalidRetryUsed).isTrue()

        vm.setDraftInput("make a draft")
        vm.submit()
        advanceUntilIdle()

        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.GoalForm)
        assertThat(vm.uiState.value.proposedAction).isNull()
    }

    private fun viewModel(grok: GrokService = StubGrokService()): AskViewModel {
        val formatting = FormattingService()
        return AskViewModel(
            repository = PiPlannerRepository(persistence, dispatcher),
            grok = grok,
            askAnswers = AskAnswerService(
                formatting = formatting,
                goalsTabService = GoalsTabService(DedicatedAccountService()),
            ),
            formatting = formatting,
            validation = GoalValidationService(OpeningSplitService()),
            standingSplitSeed = standingSeed,
        )
    }

    private fun sampleState(): AppState {
        val goals = DemoData.sampleOpeningSplitGoals()
        return AppState(
            accounts = listOf(
                Account(
                    id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
                    bankName = DemoData.SAVINGS_BANK,
                    maskedNumber = DemoData.SAVINGS_MASKED,
                    balance = DemoData.SAVINGS_OPENING_BALANCE_PAISA,
                    isDedicated = true,
                    isPaytmLinked = true,
                    consentAutoUpdate = true,
                ),
            ),
            goals = goals,
            standingSplits = listOf(
                StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.6),
                StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.4),
            ),
            hasCompletedSetup = true,
        )
    }

    private class FakePersistence(
        initial: AppState,
    ) : PersistenceService {
        private val state = MutableStateFlow(initial)
        override fun observeState(): Flow<AppState> = state
        override suspend fun loadState(): AppState = state.value
        override suspend fun saveState(newState: AppState) {
            state.value = newState
        }
        override suspend fun resetDemo() {
            state.value = AppState.EMPTY
        }
    }

    /** Counts askQuestion calls so unavailable templates can assert no Grok use. */
    private class TrackingGrokService(
        private val delegate: StubGrokService,
    ) : GrokService by delegate {
        var askQuestionCalls: Int = 0

        override fun askQuestion(query: String) =
            delegate.askQuestion(query).also { askQuestionCalls += 1 }
    }
}
