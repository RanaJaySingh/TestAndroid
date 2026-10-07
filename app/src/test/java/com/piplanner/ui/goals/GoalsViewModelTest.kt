package com.piplanner.ui.goals

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.CreditEntryService
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalsBalanceAction
import com.piplanner.domain.GoalsTabService
import com.piplanner.domain.MockBalanceSyncService
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: GoalsViewModel

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
    fun loadExposesGoalsAndFormattedTotal() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.goals).hasSize(2)
        assertThat(state.formattedTotalSavings).isEqualTo("₹1,00,000")
        assertThat(state.balanceAction).isEqualTo(GoalsBalanceAction.Sync)
        assertThat(state.balanceActionTitle).isEqualTo(GoalsTabService.ACTION_SYNC)
        assertThat(state.hasGoals).isTrue()
    }

    @Test
    fun consentOffShowsUpdateBalanceAndOpensSheet() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = false))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.balanceAction)
            .isEqualTo(GoalsBalanceAction.UpdateBalance)
        viewModel.tapBalanceAction()
        assertThat(viewModel.uiState.value.showUpdateBalanceSheet).isTrue()
        assertThat(viewModel.uiState.value.showSyncSheet).isFalse()
    }

    @Test
    fun consentOnOpensSyncSheet() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        viewModel.tapBalanceAction()
        assertThat(viewModel.uiState.value.showSyncSheet).isTrue()
        assertThat(viewModel.uiState.value.showUpdateBalanceSheet).isFalse()
    }

    @Test
    fun selectGoalAndOpenSettings() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        val goal = viewModel.uiState.value.goals.first()
        viewModel.selectGoal(goal.id)
        assertThat(viewModel.uiState.value.selectedGoalId).isEqualTo(goal.id)

        viewModel.openSettings()
        assertThat(viewModel.uiState.value.navigateToSettings).isTrue()
    }

    @Test
    fun performSyncHigherCreatesOpenCreditAndBlocksFurtherSync() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel(
            sync = MockBalanceSyncService(
                knownAccountIds = setOf(DemoData.DEMO_SAVINGS_ACCOUNT_ID),
                fetchedBalancePaisa = MockBalanceSyncService.DEMO_HIGHER_BALANCE_PAISA,
            ),
        )
        viewModel.load()
        advanceUntilIdle()

        viewModel.tapBalanceAction()
        viewModel.performSync()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.showSyncSheet).isTrue()
        assertThat(state.canContinueToCreditEntry).isTrue()
        assertThat(state.createdCreditEntryId).isNotNull()
        assertThat(state.isSyncOrUpdateBlocked).isTrue()
        assertThat(state.openEntryBannerMessage).contains("Assign now")
        assertThat(state.totalSavingsPaisa)
            .isEqualTo(MockBalanceSyncService.DEMO_HIGHER_BALANCE_PAISA)

        viewModel.dismissSyncSheet()
        viewModel.tapBalanceAction()
        assertThat(viewModel.uiState.value.errorMessage)
            .contains("open credit")
        assertThat(viewModel.uiState.value.showSyncSheet).isFalse()
    }

    @Test
    fun performSyncSameShowsNoNewCredit() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel(
            sync = MockBalanceSyncService(
                knownAccountIds = setOf(DemoData.DEMO_SAVINGS_ACCOUNT_ID),
                fetchedBalancePaisa = MockBalanceSyncService.DEMO_BALANCE_PAISA,
            ),
        )
        viewModel.load()
        advanceUntilIdle()

        viewModel.tapBalanceAction()
        viewModel.performSync()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.syncInfoMessage).isEqualTo(CreditEntryService.NO_NEW_CREDIT_MESSAGE)
        assertThat(state.createdCreditEntryId).isNull()
        assertThat(state.isSyncOrUpdateBlocked).isFalse()
    }

    @Test
    fun performSyncLowerExposesWithdrawalStub() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel(
            sync = MockBalanceSyncService(
                knownAccountIds = setOf(DemoData.DEMO_SAVINGS_ACCOUNT_ID),
                fetchedBalancePaisa = 8_500_000L,
            ),
        )
        viewModel.load()
        advanceUntilIdle()

        viewModel.tapBalanceAction()
        viewModel.performSync()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.canContinueToWithdrawal).isTrue()
        assertThat(state.withdrawalShortfallPaisa).isEqualTo(1_500_000L)
        assertThat(state.withdrawalPreviousBalancePaisa).isEqualTo(10_000_000L)
        assertThat(state.withdrawalNewBalancePaisa).isEqualTo(8_500_000L)
        assertThat(state.createdCreditEntryId).isNull()

        viewModel.continueToWithdrawal()
        advanceUntilIdle()
        val after = viewModel.uiState.value
        assertThat(after.navigateToWithdrawalPreviousPaisa).isEqualTo(10_000_000L)
        assertThat(after.navigateToWithdrawalNewPaisa).isEqualTo(8_500_000L)
    }

    @Test
    fun routesContract_goalDetailCreditEntryAndSettings() {
        assertThat(com.piplanner.ui.navigation.PiPlannerRoutes.goalDetail("abc"))
            .isEqualTo("goal_detail/abc")
        assertThat(com.piplanner.ui.navigation.PiPlannerRoutes.creditEntry("entry-1"))
            .isEqualTo("credit_entry/entry-1")
        assertThat(com.piplanner.ui.navigation.PiPlannerRoutes.SETTINGS).isEqualTo("settings")
        assertThat(com.piplanner.ui.navigation.PiPlannerRoutes.GOALS_TAB).isEqualTo("goals_tab")
    }

    private fun createViewModel(
        sync: MockBalanceSyncService = MockBalanceSyncService(
            knownAccountIds = setOf(DemoData.DEMO_SAVINGS_ACCOUNT_ID),
            fetchedBalancePaisa = MockBalanceSyncService.DEMO_HIGHER_BALANCE_PAISA,
        ),
    ): GoalsViewModel {
        val dedicated = DedicatedAccountService()
        val opening = OpeningSplitService()
        return GoalsViewModel(
            repository = PiPlannerRepository(persistence, dispatcher),
            formattingService = FormattingService(),
            goalsTabService = GoalsTabService(dedicated),
            dedicatedAccountService = dedicated,
            balanceSync = sync,
            creditEntryService = CreditEntryService(opening, StandingSplitService(opening)),
        )
    }

    private fun makePostSetupState(consent: Boolean): AppState {
        val account = Account(
            id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
            bankName = DemoData.SAVINGS_BANK,
            maskedNumber = DemoData.SAVINGS_MASKED,
            balance = DemoData.SAVINGS_OPENING_BALANCE_PAISA,
            isDedicated = true,
            isPaytmLinked = true,
            consentAutoUpdate = consent,
        )
        val now = Instant.parse("2024-01-01T00:00:00Z")
        val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val end = start.plusYears(1)
        val car = Goal(
            id = DemoData.DEMO_CAR_GOAL_ID,
            name = "Car",
            targetAmount = 50_000_000L,
            startDate = start.toString(),
            endDate = end.toString(),
            savedAmount = 6_000_000L,
            shareOfNewCredits = 0.6,
            createdAt = now.toString(),
            updatedAt = now.toString(),
        )
        val emergency = Goal(
            id = DemoData.DEMO_EMERGENCY_GOAL_ID,
            name = "Emergency Fund",
            targetAmount = 20_000_000L,
            startDate = start.toString(),
            endDate = end.toString(),
            savedAmount = 4_000_000L,
            shareOfNewCredits = 0.4,
            createdAt = now.toString(),
            updatedAt = now.toString(),
        )
        val opening = HistoryEntry(
            id = "opening-1",
            type = HistoryEntryType.OpeningBalance,
            createdAt = now.toString(),
            isLocked = true,
            creditAmount = DemoData.SAVINGS_OPENING_BALANCE_PAISA,
            newBalance = DemoData.SAVINGS_OPENING_BALANCE_PAISA,
        )
        return AppState(
            accounts = listOf(account),
            goals = listOf(car, emergency),
            history = listOf(opening),
            standingSplits = listOf(
                StandingSplit(goalId = DemoData.DEMO_CAR_GOAL_ID, percentage = 0.6),
                StandingSplit(goalId = DemoData.DEMO_EMERGENCY_GOAL_ID, percentage = 0.4),
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
