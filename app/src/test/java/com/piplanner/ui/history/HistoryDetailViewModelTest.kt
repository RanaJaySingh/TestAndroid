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
