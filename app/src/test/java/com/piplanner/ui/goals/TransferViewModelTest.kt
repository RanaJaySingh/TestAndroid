package com.piplanner.ui.goals

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.FormattingService
import com.piplanner.domain.TransferPhase
import com.piplanner.domain.TransferService
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
class TransferViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: TransferViewModel

    private val carId = DemoData.DEMO_CAR_GOAL_ID
    private val emergencyId = DemoData.DEMO_EMERGENCY_GOAL_ID

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence(sampleState())
        viewModel = TransferViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    TransferViewModel.NAV_ARG_FROM_GOAL_ID to carId,
                    TransferViewModel.NAV_ARG_TO_GOAL_ID to "",
                    TransferViewModel.NAV_ARG_AMOUNT_PAISA to "",
                ),
            ),
            repository = PiPlannerRepository(persistence, dispatcher),
            formattingService = FormattingService(),
            transferService = TransferService(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun chipsFillAmountAndOverAmountDisablesMove() = runTest {
        advanceUntilIdle()
        viewModel.selectToGoal(emergencyId)
        advanceUntilIdle()
        viewModel.applyChip(100_000L)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.amountPaisa).isEqualTo(100_000L)
        assertThat(viewModel.uiState.value.phase).isEqualTo(TransferPhase.Preview)
        assertThat(viewModel.uiState.value.canMove).isTrue()

        viewModel.setAmountDigits("20000")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.amountPaisa).isEqualTo(2_000_000L)
        assertThat(viewModel.uiState.value.phase).isEqualTo(TransferPhase.OverAmount)
        assertThat(viewModel.uiState.value.canMove).isFalse()
    }

    @Test
    fun askPrefillOpensWithFromToAndAmount() = runTest {
        val prefilled = TransferViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    TransferViewModel.NAV_ARG_FROM_GOAL_ID to carId,
                    TransferViewModel.NAV_ARG_TO_GOAL_ID to emergencyId,
                    TransferViewModel.NAV_ARG_AMOUNT_PAISA to "100000",
                ),
            ),
            repository = PiPlannerRepository(persistence, dispatcher),
            formattingService = FormattingService(),
            transferService = TransferService(),
        )
        advanceUntilIdle()
        assertThat(prefilled.uiState.value.fromGoalId).isEqualTo(carId)
        assertThat(prefilled.uiState.value.toGoalId).isEqualTo(emergencyId)
        assertThat(prefilled.uiState.value.amountPaisa).isEqualTo(100_000L)
        assertThat(prefilled.uiState.value.phase).isEqualTo(TransferPhase.Preview)
        assertThat(prefilled.uiState.value.canMove).isTrue()
    }

    @Test
    fun moveCreatesHistoryAndLeavesStandingSplit() = runTest {
        advanceUntilIdle()
        viewModel.selectToGoal(emergencyId)
        viewModel.applyChip(100_000L)
        advanceUntilIdle()
        val standingBefore = persistence.state.value.standingSplits
        viewModel.move()
        advanceUntilIdle()

        val next = persistence.state.value
        assertThat(next.history.last().type).isEqualTo(HistoryEntryType.Transfer)
        assertThat(next.standingSplits).isEqualTo(standingBefore)
        assertThat(viewModel.uiState.value.phase).isEqualTo(TransferPhase.Complete)
        assertThat(viewModel.uiState.value.navigateBack).isTrue()
    }

    private fun sampleState(): AppState {
        val goals = DemoData.sampleOpeningSplitGoals().map { goal ->
            when (goal.id) {
                carId -> goal.copy(savedAmount = 600_000L, shareOfNewCredits = 0.6)
                emergencyId -> goal.copy(savedAmount = 400_000L, shareOfNewCredits = 0.4)
                else -> goal
            }
        }
        return AppState(
            accounts = listOf(
                Account(
                    id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
                    bankName = DemoData.SAVINGS_BANK,
                    maskedNumber = DemoData.SAVINGS_MASKED,
                    balance = 1_000_000L,
                    isDedicated = true,
                    isPaytmLinked = true,
                    consentAutoUpdate = true,
                ),
            ),
            goals = goals,
            standingSplits = listOf(
                StandingSplit(goalId = carId, percentage = 0.6),
                StandingSplit(goalId = emergencyId, percentage = 0.4),
            ),
            hasCompletedSetup = true,
        )
    }

    private class FakePersistence(
        initial: AppState,
    ) : PersistenceService {
        val state = MutableStateFlow(initial)
        override fun observeState(): Flow<AppState> = state
        override suspend fun loadState(): AppState = state.value
        override suspend fun saveState(newState: AppState) {
            state.value = newState
        }
        override suspend fun resetDemo() {
            state.value = AppState.EMPTY
        }
    }
}
