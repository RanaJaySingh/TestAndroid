package com.piplanner.ui.goals

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.DeleteGoalService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalValidationService
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
class DeleteGoalViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: DeleteGoalViewModel
    private val now = "2023-11-14T22:13:20Z"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence()
        val opening = OpeningSplitService()
        val standing = StandingSplitService(opening)
        viewModel = DeleteGoalViewModel(
            repository = PiPlannerRepository(persistence, dispatcher),
            formattingService = FormattingService(),
            deleteGoalService = DeleteGoalService(opening, standing),
            openingSplitService = opening,
            standingSplitService = standing,
            goalValidationService = GoalValidationService(opening),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun configure_multiGoal_defaultsToEqualReassignment() = runTest(dispatcher) {
        seedTwoGoals()
        viewModel.configure(DemoData.DEMO_CAR_GOAL_ID)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.phase).isEqualTo(DeleteGoalPhase.ReassignDefault)
        assertThat(state.destinationGoals).hasSize(1)
        assertThat(state.displayPercents[DemoData.DEMO_EMERGENCY_GOAL_ID]).isEqualTo(100)
        assertThat(state.canConfirm).isTrue()
        assertThat(state.releasedAmount).isEqualTo(6_000_000L)
    }

    @Test
    fun equalDefault_canConfirmWithoutEditing() = runTest(dispatcher) {
        seedThreeGoals()
        viewModel.configure(DemoData.DEMO_CAR_GOAL_ID)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.hasEditedOnce).isFalse()
        assertThat(state.isEditingPercents).isFalse()
        assertThat(state.canEditPercents).isFalse()
        assertThat(state.canStartEdit).isTrue()
        assertThat(state.canConfirm).isTrue()
    }

    @Test
    fun editOnce_locksAfterDone() = runTest(dispatcher) {
        seedThreeGoals()
        viewModel.configure(DemoData.DEMO_CAR_GOAL_ID)
        advanceUntilIdle()

        // Before Edit: percent changes are ignored
        viewModel.setDisplayPercent(DemoData.DEMO_EMERGENCY_GOAL_ID, 99)
        assertThat(viewModel.uiState.value.displayPercents[DemoData.DEMO_EMERGENCY_GOAL_ID])
            .isNotEqualTo(99)

        viewModel.beginEditPercents()
        assertThat(viewModel.uiState.value.isEditingPercents).isTrue()
        assertThat(viewModel.uiState.value.canEditPercents).isTrue()
        assertThat(viewModel.uiState.value.phase).isEqualTo(DeleteGoalPhase.ReassignEdit)

        viewModel.setDisplayPercent(DemoData.DEMO_EMERGENCY_GOAL_ID, 70)
        viewModel.setDisplayPercent("cccccccc-cccc-cccc-cccc-cccccccccccc", 30)
        assertThat(viewModel.uiState.value.amountsByGoalId[DemoData.DEMO_EMERGENCY_GOAL_ID])
            .isEqualTo(4_200_000L)

        viewModel.finishEditPercents()
        val locked = viewModel.uiState.value
        assertThat(locked.hasEditedOnce).isTrue()
        assertThat(locked.isEditingPercents).isFalse()
        assertThat(locked.canEditPercents).isFalse()
        assertThat(locked.canStartEdit).isFalse()
        assertThat(locked.canConfirm).isTrue()

        // Second pass blocked
        viewModel.beginEditPercents()
        viewModel.setDisplayPercent(DemoData.DEMO_EMERGENCY_GOAL_ID, 10)
        assertThat(viewModel.uiState.value.displayPercents[DemoData.DEMO_EMERGENCY_GOAL_ID])
            .isEqualTo(70)
    }

    @Test
    fun onlyGoal_confirmDisabledUntilReplacementCreated() = runTest(dispatcher) {
        seedOnlyGoal()
        viewModel.configure(DemoData.DEMO_CAR_GOAL_ID)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.phase).isEqualTo(DeleteGoalPhase.OnlyGoalGate)
        assertThat(viewModel.uiState.value.canConfirm).isFalse()

        viewModel.updateReplacementDraft(
            ReplacementGoalDraft(
                name = "Vacation",
                targetRupeesText = "50000",
                startDate = "2023-11-14",
                endDate = "2024-11-14",
            ),
        )
        viewModel.saveReplacementGoal()

        val state = viewModel.uiState.value
        assertThat(state.phase).isEqualTo(DeleteGoalPhase.ReassignDefault)
        assertThat(state.destinationGoals).hasSize(1)
        assertThat(state.destinationGoals.first().name).isEqualTo("Vacation")
        assertThat(state.canConfirm).isTrue()
        assertThat(state.resetStandingToEqual).isTrue()
    }

    @Test
    fun confirmDelete_persistsHistoryAndRemovesGoal() = runTest(dispatcher) {
        seedTwoGoals()
        viewModel.configure(DemoData.DEMO_CAR_GOAL_ID)
        advanceUntilIdle()

        viewModel.confirmDelete()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.phase).isEqualTo(DeleteGoalPhase.Completed)
        assertThat(state.shouldNavigateBack).isTrue()

        val persisted = persistence.loadState()
        assertThat(persisted.goals.map { it.id }).containsExactly(DemoData.DEMO_EMERGENCY_GOAL_ID)
        assertThat(persisted.goals.first().savedAmount).isEqualTo(10_000_000L)
        assertThat(persisted.history.last().type).isEqualTo(HistoryEntryType.GoalDeleted)
        assertThat(persisted.history.last().deletedGoalName).isEqualTo("Car")
        assertThat(persisted.standingSplits.first().percentage).isEqualTo(1.0)
    }

    private suspend fun seedTwoGoals() {
        persistence.saveState(
            AppState(
                goals = listOf(carGoal(), emergencyGoal()),
                standingSplits = listOf(
                    StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.6),
                    StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.4),
                ),
                hasCompletedSetup = true,
            ),
        )
    }

    private suspend fun seedThreeGoals() {
        persistence.saveState(
            AppState(
                goals = listOf(
                    carGoal(),
                    emergencyGoal(),
                    emergencyGoal().copy(
                        id = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                        name = "House",
                        savedAmount = 0L,
                        shareOfNewCredits = 0.0,
                    ),
                ),
                standingSplits = listOf(
                    StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 0.5),
                    StandingSplit(DemoData.DEMO_EMERGENCY_GOAL_ID, 0.3),
                    StandingSplit("cccccccc-cccc-cccc-cccc-cccccccccccc", 0.2),
                ),
                hasCompletedSetup = true,
            ),
        )
    }

    private suspend fun seedOnlyGoal() {
        persistence.saveState(
            AppState(
                goals = listOf(carGoal().copy(savedAmount = 10_000_000L, shareOfNewCredits = 1.0)),
                standingSplits = listOf(StandingSplit(DemoData.DEMO_CAR_GOAL_ID, 1.0)),
                hasCompletedSetup = true,
            ),
        )
    }

    private fun carGoal(): Goal = Goal(
        id = DemoData.DEMO_CAR_GOAL_ID,
        name = "Car",
        targetAmount = 50_000_000L,
        startDate = "2023-11-14",
        endDate = "2024-11-14",
        savedAmount = 6_000_000L,
        shareOfNewCredits = 0.6,
        createdAt = now,
        updatedAt = now,
    )

    private fun emergencyGoal(): Goal = Goal(
        id = DemoData.DEMO_EMERGENCY_GOAL_ID,
        name = "Emergency Fund",
        targetAmount = 20_000_000L,
        startDate = "2023-11-14",
        endDate = "2024-11-14",
        savedAmount = 4_000_000L,
        shareOfNewCredits = 0.4,
        createdAt = now,
        updatedAt = now,
    )

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
