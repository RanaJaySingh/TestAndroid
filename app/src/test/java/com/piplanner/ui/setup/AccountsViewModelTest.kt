package com.piplanner.ui.setup

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.AppState
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
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
class AccountsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: AccountsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence()
        viewModel = AccountsViewModel(
            repository = PiPlannerRepository(persistence, dispatcher),
            dedicatedAccountService = DedicatedAccountService(),
            formattingService = FormattingService(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun continueDisabled_whenNoneDedicated() {
        viewModel.configure(DemoData.sampleAccounts())

        val state = viewModel.uiState.value
        assertThat(state.hasNoneDedicated).isTrue()
        assertThat(state.canContinue).isFalse()
        assertThat(state.statusMessage).contains("Pick one Dedicated")
    }

    @Test
    fun toggleExclusivity_enablesContinue_andTurnsOtherOff() {
        viewModel.configure(DemoData.sampleAccounts())

        viewModel.setDedicated(DemoData.DEMO_SAVINGS_ACCOUNT_ID, true)
        var state = viewModel.uiState.value
        assertThat(state.accounts.single { it.id == DemoData.DEMO_SAVINGS_ACCOUNT_ID }.isDedicated).isTrue()
        assertThat(state.accounts.single { it.id == DemoData.DEMO_SPENDING_ACCOUNT_ID }.isDedicated).isFalse()
        assertThat(state.hasExactlyOneDedicated).isTrue()
        assertThat(state.canContinue).isTrue()

        viewModel.setDedicated(DemoData.DEMO_SPENDING_ACCOUNT_ID, true)
        state = viewModel.uiState.value
        assertThat(state.accounts.single { it.id == DemoData.DEMO_SPENDING_ACCOUNT_ID }.isDedicated).isTrue()
        assertThat(state.accounts.single { it.id == DemoData.DEMO_SAVINGS_ACCOUNT_ID }.isDedicated).isFalse()
        assertThat(state.canContinue).isTrue()
    }

    @Test
    fun onContinue_persistsDedicatedAndNavigatesToConsent() = runTest(dispatcher) {
        viewModel.configure(DemoData.sampleAccounts())
        viewModel.setDedicated(DemoData.DEMO_SAVINGS_ACCOUNT_ID, true)

        viewModel.onContinue()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.shouldNavigateToConsent).isTrue()

        val persisted = persistence.loadState()
        assertThat(persisted.accounts).hasSize(2)
        assertThat(persisted.accounts.single { it.isDedicated }.id)
            .isEqualTo(DemoData.DEMO_SAVINGS_ACCOUNT_ID)
        assertThat(persisted.accounts.single { it.id == DemoData.DEMO_SPENDING_ACCOUNT_ID }.isDedicated)
            .isFalse()
    }

    @Test
    fun onContinue_ignoredWhenNoneDedicated() = runTest(dispatcher) {
        viewModel.configure(DemoData.sampleAccounts())
        viewModel.onContinue()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.shouldNavigateToConsent).isFalse()
        assertThat(persistence.loadState().accounts).isEmpty()
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
