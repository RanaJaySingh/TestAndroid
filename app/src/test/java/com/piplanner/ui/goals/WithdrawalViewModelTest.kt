package com.piplanner.ui.goals

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.OpeningSplitService
import com.piplanner.domain.WithdrawalService
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WithdrawalViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: WithdrawalViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDefaultsToProportionalAndEnablesSave() = runTest(dispatcher) {
        persistence.saveState(makeState())
        viewModel = createViewModel(previous = 10_000_000L, newBalance = 8_500_000L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.phase).isEqualTo(WithdrawalPhase.ProportionalDefault)
        assertThat(state.shortfallPaisa).isEqualTo(1_500_000L)
        assertThat(state.reductionsPaisa.values.sum()).isEqualTo(1_500_000L)
        assertThat(state.canSave).isTrue()
        assertThat(state.canStartEdit).isTrue()
    }

    @Test
    fun editInvalidTotalDisablesSave() = runTest(dispatcher) {
        persistence.saveState(makeState())
        viewModel = createViewModel(previous = 10_000_000L, newBalance = 8_500_000L)
        advanceUntilIdle()

        viewModel.beginEdit()
        viewModel.setReductionRupees(DemoData.DEMO_CAR_GOAL_ID, "1000")
        viewModel.setReductionRupees(DemoData.DEMO_EMERGENCY_GOAL_ID, "1000")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.canSave).isFalse()
        assertThat(state.phase).isEqualTo(WithdrawalPhase.InvalidTotal)
        assertThat(state.statusMessage).contains("Assign the remaining")
    }

    @Test
    fun saveAndLockWritesHistoryEntry() = runTest(dispatcher) {
        persistence.saveState(makeState())
        viewModel = createViewModel(previous = 10_000_000L, newBalance = 8_500_000L)
        advanceUntilIdle()

        viewModel.saveAndLock()
        advanceUntilIdle()

        val persisted = persistence.loadState()
        val entry = persisted.history.single { it.type == HistoryEntryType.Withdrawal }
        assertThat(entry.isLocked).isTrue()
        assertThat(entry.withdrawalAmount).isEqualTo(1_500_000L)
        assertThat(viewModel.uiState.value.phase).isEqualTo(WithdrawalPhase.Complete)
        assertThat(viewModel.uiState.value.navigateBack).isTrue()
    }

    private fun createViewModel(previous: Long, newBalance: Long): WithdrawalViewModel {
        val handle = SavedStateHandle(
            mapOf(
                WithdrawalViewModel.NAV_ARG_PREVIOUS_BALANCE to previous,
                WithdrawalViewModel.NAV_ARG_NEW_BALANCE to newBalance,
                WithdrawalViewModel.NAV_ARG_IS_TYPED to false,
            ),
        )
        return WithdrawalViewModel(
            savedStateHandle = handle,
            repository = PiPlannerRepository(persistence, dispatcher),
            formattingService = FormattingService(),
            withdrawalService = WithdrawalService(OpeningSplitService()),
            dedicatedAccountService = DedicatedAccountService(),
        )
    }

    private fun makeState(): AppState {
        val now = Instant.parse("2024-01-01T00:00:00Z")
        val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val end = start.plusYears(1)
        val createdAt = now.toString()
        return AppState(
            accounts = listOf(
                Account(
                    id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
                    bankName = DemoData.SAVINGS_BANK,
                    maskedNumber = DemoData.SAVINGS_MASKED,
                    balance = 10_000_000L,
                    isDedicated = true,
                    isPaytmLinked = true,
                    consentAutoUpdate = true,
                ),
            ),
            goals = listOf(
                Goal(
                    id = DemoData.DEMO_CAR_GOAL_ID,
                    name = "Car",
                    targetAmount = 50_000_000L,
                    startDate = start.toString(),
                    endDate = end.toString(),
                    savedAmount = 6_000_000L,
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
                    savedAmount = 4_000_000L,
                    shareOfNewCredits = 0.4,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                ),
            ),
            standingSplits = listOf(
                StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.6),
                StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.4),
            ),
            hasCompletedSetup = true,
        )
    }

    private class FakePersistence : PersistenceService {
        private val state = MutableStateFlow(AppState.EMPTY)

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
