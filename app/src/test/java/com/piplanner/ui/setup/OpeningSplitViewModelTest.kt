package com.piplanner.ui.setup

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.AppState
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.FormattingService
import com.piplanner.domain.OpeningSplitService
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
class OpeningSplitViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: OpeningSplitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence()
        val repository = PiPlannerRepository(persistence, dispatcher)
        viewModel = OpeningSplitViewModel(
            repository = repository,
            formattingService = FormattingService(),
            openingSplitService = OpeningSplitService(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun multiGoal_lockDisabledUntilPercentsSumTo100() {
        val goals = DemoData.sampleOpeningSplitGoals()
        viewModel.configure(goals = goals, openingBalance = DemoData.SAVINGS_OPENING_BALANCE_PAISA)
        // Demo seed shares already sum to 100 (60/40)
        assertThat(viewModel.uiState.value.canLock).isTrue()

        viewModel.setDisplayPercent(DemoData.DEMO_CAR_GOAL_ID, 50)
        assertThat(viewModel.uiState.value.canLock).isFalse()
        assertThat(viewModel.uiState.value.statusMessage).contains("Assign the remaining")
    }

    @Test
    fun singleGoal_autoHundredPercent_noEditableNeed() {
        val goals = DemoData.sampleOpeningSplitGoals().take(1)
        viewModel.configure(goals = goals, openingBalance = DemoData.SAVINGS_OPENING_BALANCE_PAISA)

        val state = viewModel.uiState.value
        assertThat(state.isSingleGoal).isTrue()
        assertThat(state.displayPercents[goals.first().id]).isEqualTo(100)
        assertThat(state.canLock).isTrue()
        assertThat(state.statusMessage).contains("100% assigned")
    }

    @Test
    fun confirmLock_createsOpeningHistoryEntryAndNavigates() = runTest(dispatcher) {
        val goals = DemoData.sampleOpeningSplitGoals()
        viewModel.configure(goals = goals, openingBalance = DemoData.SAVINGS_OPENING_BALANCE_PAISA)

        viewModel.confirmLock()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.shouldNavigateToGoals).isTrue()
        assertThat(state.lockedEntry).isNotNull()
        assertThat(state.lockedEntry!!.type).isEqualTo(HistoryEntryType.OpeningBalance)
        assertThat(state.lockedEntry!!.isLocked).isTrue()
        assertThat(state.statusMessage).isEqualTo(OpeningSplitService.LOCKED_AMOUNTS_CAPTION)
        assertThat(state.isReadOnly).isTrue()
        assertThat(state.canLock).isFalse()

        val persisted = persistence.loadState()
        assertThat(persisted.history).hasSize(1)
        assertThat(persisted.history.first().type).isEqualTo(HistoryEntryType.OpeningBalance)
        assertThat(persisted.hasCompletedSetup).isTrue()
        assertThat(persisted.goals.sumOf { it.savedAmount }).isEqualTo(DemoData.SAVINGS_OPENING_BALANCE_PAISA)
    }

    @Test
    fun configureReadOnly_showsLockedCaptionAndDisablesLock() = runTest(dispatcher) {
        val goals = DemoData.sampleOpeningSplitGoals()
        viewModel.configure(goals = goals, openingBalance = DemoData.SAVINGS_OPENING_BALANCE_PAISA)
        viewModel.confirmLock()
        advanceUntilIdle()
        val entry = viewModel.uiState.value.lockedEntry!!

        val readOnlyVm = OpeningSplitViewModel(
            repository = PiPlannerRepository(FakePersistence(), dispatcher),
            formattingService = FormattingService(),
            openingSplitService = OpeningSplitService(),
        )
        readOnlyVm.configureReadOnly(entry, goals)

        val state = readOnlyVm.uiState.value
        assertThat(state.isReadOnly).isTrue()
        assertThat(state.canLock).isFalse()
        assertThat(state.statusMessage)
            .isEqualTo(com.piplanner.domain.HistoryService.ORIGINAL_AMOUNTS_CAPTION)
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
