package com.piplanner.ui.history

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.AppState
import com.piplanner.data.model.GoalAllocation
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.FormattingService
import com.piplanner.domain.HistoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence

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
    fun lockedNewCredit_exposesOriginalAmountsCaptionNotLockedAmounts() = runTest(dispatcher) {
        val entry = HistoryEntry(
            id = "credit-locked",
            type = HistoryEntryType.NewCredit,
            createdAt = "2024-02-01T00:00:00Z",
            isLocked = true,
            creditAmount = 5_000_000L,
            previousBalance = 10_000_000L,
            newBalance = 15_000_000L,
            allocations = listOf(
                GoalAllocation("g1", "Car", 3_000_000L, 0.6),
                GoalAllocation("g2", "Emergency", 2_000_000L, 0.4),
            ),
        )
        persistence.saveState(AppState(history = listOf(entry), hasCompletedSetup = true))

        val viewModel = HistoryDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(HistoryDetailViewModel.NAV_ARG_ENTRY_ID to "credit-locked"),
            ),
            repository = PiPlannerRepository(persistence, dispatcher),
            historyService = HistoryService(FormattingService()),
            formattingService = FormattingService(),
        )

        advanceUntilIdle()
        val state = viewModel.uiState.first { it.entry != null }

        assertThat(state.missing).isFalse()
        assertThat(state.isReadOnly).isTrue()
        assertThat(state.typeLabel).isEqualTo("New credit")
        assertThat(state.caption).isEqualTo(HistoryService.ORIGINAL_AMOUNTS_CAPTION)
        assertThat(state.caption).isEqualTo("Original amounts never change")
        assertThat(state.caption).isNotEqualTo("Locked amounts never change")
        assertThat(state.formattedAmount).isEqualTo("₹50,000")
        assertThat(state.allocationRows).hasSize(2)
    }

    @Test
    fun goalDeleted_listTypeLabelIsGoalDeleted() = runTest(dispatcher) {
        val entry = HistoryEntry(
            id = "deleted-1",
            type = HistoryEntryType.GoalDeleted,
            createdAt = "2024-03-03T00:00:00Z",
            isLocked = true,
            deletedGoalName = "Vacation",
            releasedAmount = 1_000_000L,
            allocations = listOf(
                GoalAllocation("g1", "Car", 600_000L, 0.6),
                GoalAllocation("g2", "Emergency", 400_000L, 0.4),
            ),
        )
        persistence.saveState(AppState(history = listOf(entry), hasCompletedSetup = true))

        val viewModel = HistoryDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(HistoryDetailViewModel.NAV_ARG_ENTRY_ID to "deleted-1"),
            ),
            repository = PiPlannerRepository(persistence, dispatcher),
            historyService = HistoryService(FormattingService()),
            formattingService = FormattingService(),
        )

        advanceUntilIdle()
        val state = viewModel.uiState.first { it.entry != null }

        assertThat(state.typeLabel).isEqualTo("Goal deleted")
        assertThat(state.caption).isEqualTo(HistoryService.ORIGINAL_AMOUNTS_CAPTION)
    }

    @Test
    fun lockedTransfer_exposesReadOnlyCaptionAndAllocations() = runTest(dispatcher) {
        val entry = HistoryEntry(
            id = "transfer-1",
            type = HistoryEntryType.Transfer,
            createdAt = "2024-02-15T00:00:00Z",
            isLocked = true,
            fromGoalId = "g2",
            toGoalId = "g1",
            transferAmount = 500_000L,
            allocations = listOf(
                GoalAllocation("g1", "Car", 500_000L, 1.0),
            ),
        )
        persistence.saveState(AppState(history = listOf(entry), hasCompletedSetup = true))

        val viewModel = HistoryDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(HistoryDetailViewModel.NAV_ARG_ENTRY_ID to "transfer-1"),
            ),
            repository = PiPlannerRepository(persistence, dispatcher),
            historyService = HistoryService(FormattingService()),
            formattingService = FormattingService(),
        )

        advanceUntilIdle()
        val state = viewModel.uiState.first { it.entry != null }

        assertThat(state.missing).isFalse()
        assertThat(state.isReadOnly).isTrue()
        assertThat(state.caption).isEqualTo(HistoryService.ORIGINAL_AMOUNTS_CAPTION)
        assertThat(state.typeLabel).isEqualTo("Transfer")
        assertThat(state.formattedAmount).isEqualTo("₹5,000")
        assertThat(state.allocationRows).hasSize(1)
        assertThat(state.allocationRows.first().goalName).isEqualTo("Car")
    }

    @Test
    fun missingEntry_setsMissingFlag() = runTest(dispatcher) {
        persistence.saveState(AppState(history = emptyList(), hasCompletedSetup = true))
        val viewModel = HistoryDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(HistoryDetailViewModel.NAV_ARG_ENTRY_ID to "gone"),
            ),
            repository = PiPlannerRepository(persistence, dispatcher),
            historyService = HistoryService(FormattingService()),
            formattingService = FormattingService(),
        )
        advanceUntilIdle()
        val state = viewModel.uiState.first { it.entryId == "gone" && it.missing }
        assertThat(state.missing).isTrue()
        assertThat(state.entry).isNull()
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
