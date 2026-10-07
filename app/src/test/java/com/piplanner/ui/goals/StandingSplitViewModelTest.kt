package com.piplanner.ui.goals

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.AppState
import com.piplanner.data.model.StandingSplit
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.OpeningSplitService
import com.piplanner.domain.StandingSplitService
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
class StandingSplitViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: StandingSplitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence()
        val repository = PiPlannerRepository(persistence, dispatcher)
        viewModel = StandingSplitViewModel(
            repository = repository,
            standingSplitService = StandingSplitService(OpeningSplitService()),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun multiGoal_saveDisabledUntilPercentsSumTo100() {
        val goals = DemoData.sampleOpeningSplitGoals()
        viewModel.configure(goals = goals)

        assertThat(viewModel.uiState.value.canSave).isTrue()
        assertThat(viewModel.uiState.value.caption)
            .isEqualTo(StandingSplitService.SAVED_MONEY_STAYS_PUT)

        viewModel.setDisplayPercent(DemoData.DEMO_CAR_GOAL_ID, 50)
        val invalid = viewModel.uiState.value
        assertThat(invalid.canSave).isFalse()
        assertThat(invalid.isInvalid).isTrue()
        assertThat(invalid.statusMessage).contains("Assign the remaining")
    }

    @Test
    fun multiGoal_savePersistsStandingSplitForNextCredit() = runTest(dispatcher) {
        val goals = DemoData.sampleOpeningSplitGoals()
        persistence.saveState(AppState(goals = goals, hasCompletedSetup = true))
        viewModel.configure(goals = goals)

        viewModel.setDisplayPercent(DemoData.DEMO_CAR_GOAL_ID, 70)
        viewModel.setDisplayPercent(DemoData.DEMO_EMERGENCY_GOAL_ID, 30)
        assertThat(viewModel.uiState.value.canSave).isTrue()

        viewModel.save()
        advanceUntilIdle()

        val ui = viewModel.uiState.value
        assertThat(ui.shouldNavigateBack).isTrue()

        val persisted = persistence.loadState()
        assertThat(persisted.standingSplits).containsExactly(
            StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.70),
            StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.30),
        )
        assertThat(persisted.goals.first { it.id == DemoData.DEMO_CAR_GOAL_ID }.shareOfNewCredits)
            .isEqualTo(0.70)

        val nextCredit = StandingSplitService().fractionsForNextCredit(persisted)
        assertThat(nextCredit[DemoData.DEMO_CAR_GOAL_ID]?.toDouble()).isEqualTo(0.70)
        assertThat(nextCredit[DemoData.DEMO_EMERGENCY_GOAL_ID]?.toDouble()).isEqualTo(0.30)
    }

    @Test
    fun singleGoal_skipsUiAndAppliesHundredPercent() = runTest(dispatcher) {
        val goals = DemoData.sampleOpeningSplitGoals().take(1)
        persistence.saveState(AppState(goals = goals, hasCompletedSetup = true))
        viewModel.configure(goals = goals)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.shouldSkip).isTrue()
        assertThat(state.isSingleGoal).isTrue()
        assertThat(state.displayPercents[goals.first().id]).isEqualTo(100)
        assertThat(state.canSave).isFalse()
        assertThat(state.shouldNavigateBack).isTrue()

        val persisted = persistence.loadState()
        assertThat(persisted.standingSplits).hasSize(1)
        assertThat(persisted.standingSplits.first().percentage).isEqualTo(1.0)
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
