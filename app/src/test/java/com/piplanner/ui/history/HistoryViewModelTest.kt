package com.piplanner.ui.history

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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence()
        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyState_whenNoHistory() = runTest(dispatcher) {
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isEmpty).isTrue()
        assertThat(state.rows).isEmpty()
    }

    @Test
    fun list_newestFirst_withLockIconsAndTypeLabels() = runTest(dispatcher) {
        persistence.saveState(
            AppState(
                history = listOf(
                    openingEntry("opening", "2024-01-01T00:00:00Z"),
                    lockedCredit("credit", "2024-02-01T00:00:00Z"),
                    openCredit("open", "2024-03-01T00:00:00Z"),
                    transferEntry("transfer", "2024-02-15T00:00:00Z"),
                    deletedEntry("deleted", "2024-02-20T00:00:00Z"),
                ),
                hasCompletedSetup = true,
            ),
        )
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isEmpty).isFalse()
        assertThat(state.rows.map { it.id })
            .containsExactly("open", "deleted", "transfer", "credit", "opening")
            .inOrder()

        val openRow = state.rows.first { it.id == "open" }
        assertThat(openRow.typeLabel).isEqualTo("New credit")
        assertThat(openRow.isOpenAssignable).isTrue()
        assertThat(openRow.showLockIcon).isFalse()
        assertThat(openRow.subtitle).isEqualTo("Assign now")

        val lockedRow = state.rows.first { it.id == "credit" }
        assertThat(lockedRow.showLockIcon).isTrue()
        assertThat(lockedRow.isLocked).isTrue()

        val openingRow = state.rows.first { it.id == "opening" }
        assertThat(openingRow.typeLabel).isEqualTo("Opening balance")
        assertThat(openingRow.showLockIcon).isTrue()

        assertThat(state.rows.map { it.type }.toSet()).containsExactly(
            HistoryEntryType.NewCredit,
            HistoryEntryType.GoalDeleted,
            HistoryEntryType.Transfer,
            HistoryEntryType.OpeningBalance,
        )
    }

    @Test
    fun tapOpenAssignNow_navigatesToCreditEntry() = runTest(dispatcher) {
        persistence.saveState(
            AppState(
                history = listOf(
                    openingEntry("opening", "2024-01-01T00:00:00Z"),
                    openCredit("open", "2024-03-01T00:00:00Z"),
                ),
                hasCompletedSetup = true,
            ),
        )
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        viewModel.onEntryClick("open")
        assertThat(viewModel.uiState.value.navigateToCreditEntryId).isEqualTo("open")
        viewModel.consumeCreditEntryNavigation()
        assertThat(viewModel.uiState.value.navigateToCreditEntryId).isNull()
    }

    @Test
    fun tapOpeningBalance_navigatesToReadOnlyOpening() = runTest(dispatcher) {
        persistence.saveState(
            AppState(
                history = listOf(openingEntry("opening", "2024-01-01T00:00:00Z")),
                hasCompletedSetup = true,
            ),
        )
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        viewModel.onEntryClick("opening")
        assertThat(viewModel.uiState.value.navigateToOpeningEntryId).isEqualTo("opening")
        assertThat(viewModel.uiState.value.navigateToCreditEntryId).isNull()
    }

    @Test
    fun tapLockedTransfer_navigatesToLockedDetail() = runTest(dispatcher) {
        persistence.saveState(
            AppState(
                history = listOf(
                    openingEntry("opening", "2024-01-01T00:00:00Z"),
                    transferEntry("transfer", "2024-02-15T00:00:00Z"),
                ),
                hasCompletedSetup = true,
            ),
        )
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        viewModel.onEntryClick("transfer")
        assertThat(viewModel.uiState.value.navigateToLockedDetailId).isEqualTo("transfer")
    }

    @Test
    fun tapLockedNewCredit_navigatesToLockedDetailNotCreditEntry() = runTest(dispatcher) {
        persistence.saveState(
            AppState(
                history = listOf(
                    openingEntry("opening", "2024-01-01T00:00:00Z"),
                    lockedCredit("credit", "2024-02-01T00:00:00Z"),
                ),
                hasCompletedSetup = true,
            ),
        )
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        val lockedRow = viewModel.uiState.value.rows.first { it.id == "credit" }
        assertThat(lockedRow.showLockIcon).isTrue()
        assertThat(lockedRow.isOpenAssignable).isFalse()

        viewModel.onEntryClick("credit")
        assertThat(viewModel.uiState.value.navigateToLockedDetailId).isEqualTo("credit")
        assertThat(viewModel.uiState.value.navigateToCreditEntryId).isNull()
    }

    @Test
    fun goalDeletedListLabel_isGoalDeleted() = runTest(dispatcher) {
        persistence.saveState(
            AppState(
                history = listOf(
                    openingEntry("opening", "2024-01-01T00:00:00Z"),
                    deletedEntry("deleted", "2024-02-20T00:00:00Z"),
                ),
                hasCompletedSetup = true,
            ),
        )
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        val deletedRow = viewModel.uiState.value.rows.first { it.id == "deleted" }
        assertThat(deletedRow.typeLabel).isEqualTo("Goal deleted")
        assertThat(deletedRow.typeLabel).isNotEqualTo("Deleted / moved")
    }

    @Test
    fun readOnlyCaption_matchesFrame12a() {
        assertThat(HistoryService.ORIGINAL_AMOUNTS_CAPTION)
            .isEqualTo("Original amounts never change")
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(
            repository = PiPlannerRepository(persistence, dispatcher),
            historyService = HistoryService(FormattingService()),
        )
    }

    private fun openingEntry(id: String, createdAt: String): HistoryEntry {
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.OpeningBalance,
            createdAt = createdAt,
            isLocked = true,
            creditAmount = 10_000_000L,
            allocations = listOf(
                GoalAllocation("g1", "Car", 6_000_000L, 0.6),
                GoalAllocation("g2", "Emergency", 4_000_000L, 0.4),
            ),
        )
    }

    private fun lockedCredit(id: String, createdAt: String): HistoryEntry {
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.NewCredit,
            createdAt = createdAt,
            isLocked = true,
            creditAmount = 5_000_000L,
        )
    }

    private fun openCredit(id: String, createdAt: String): HistoryEntry {
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.NewCredit,
            createdAt = createdAt,
            isLocked = false,
            creditAmount = 5_000_000L,
        )
    }

    private fun transferEntry(id: String, createdAt: String): HistoryEntry {
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.Transfer,
            createdAt = createdAt,
            isLocked = true,
            fromGoalId = "g2",
            toGoalId = "g1",
            transferAmount = 500_000L,
        )
    }

    private fun deletedEntry(id: String, createdAt: String): HistoryEntry {
        return HistoryEntry(
            id = id,
            type = HistoryEntryType.GoalDeleted,
            createdAt = createdAt,
            isLocked = true,
            deletedGoalName = "Vacation",
            releasedAmount = 1_000_000L,
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
