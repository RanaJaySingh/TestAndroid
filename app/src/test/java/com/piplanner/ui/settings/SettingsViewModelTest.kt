package com.piplanner.ui.settings

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
import com.piplanner.domain.CreditProcessOutcome
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalsTabService
import com.piplanner.domain.MockBalanceSyncService
import com.piplanner.domain.OpeningSplitService
import com.piplanner.domain.StandingSplitService
import com.piplanner.ui.navigation.PiPlannerRoutes
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
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: SettingsViewModel

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
    fun load_showsLinkedAccountsAndConsentOn() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.linkedAccounts).hasSize(2)
        assertThat(state.consentAutoUpdate).isTrue()
        assertThat(state.dedicatedAccountTitle).contains("HDFC")
        assertThat(state.linkedAccounts.any { it.isDedicated }).isTrue()
    }

    @Test
    fun load_consentOff_reflectsUpdateBalanceMode() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = false))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.consentAutoUpdate).isFalse()
    }

    @Test
    fun consentToggleOff_persistsConsentFalse() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        viewModel.onConsentToggle(enabled = false)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.consentAutoUpdate).isFalse()
        val dedicated = persistence.loadState().accounts.single { it.isDedicated }
        assertThat(dedicated.consentAutoUpdate).isFalse()
        assertThat(viewModel.uiState.value.navigateToConsent).isFalse()
    }

    @Test
    fun consentToggleOn_requestsConsentSheetWithoutPersistingYet() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = false))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        viewModel.onConsentToggle(enabled = true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.navigateToConsent).isTrue()
        assertThat(viewModel.uiState.value.consentAutoUpdate).isFalse()
        val dedicated = persistence.loadState().accounts.single { it.isDedicated }
        assertThat(dedicated.consentAutoUpdate).isFalse()
    }

    @Test
    fun consentPersistedTrue_afterSettingsConsentYesPath() = runTest(dispatcher) {
        // Custom post-setup balance — Settings Yes must not replace with mock ₹1,00,000.
        val customBalance = 12_345_678L
        persistence.saveState(
            makePostSetupState(consent = false).let { state ->
                state.copy(
                    accounts = state.accounts.map { account ->
                        if (account.isDedicated) account.copy(balance = customBalance) else account
                    },
                )
            },
        )
        val repository = PiPlannerRepository(persistence, dispatcher)
        val consentViewModel = com.piplanner.ui.setup.ConsentViewModel(
            repository = repository,
            balanceSync = MockBalanceSyncService(
                knownAccountIds = setOf(DemoData.DEMO_SAVINGS_ACCOUNT_ID),
            ),
            dedicatedAccountService = DedicatedAccountService(),
            formattingService = FormattingService(),
        )
        consentViewModel.loadAccounts(fetchesBalanceOnYes = false)
        advanceUntilIdle()
        consentViewModel.chooseConsentYes()
        advanceUntilIdle()

        val dedicated = persistence.loadState().accounts.single { it.isDedicated }
        assertThat(dedicated.consentAutoUpdate).isTrue()
        assertThat(dedicated.balance).isEqualTo(customBalance)
        assertThat(dedicated.balance).isNotEqualTo(MockBalanceSyncService.DEMO_BALANCE_PAISA)

        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.consentAutoUpdate).isTrue()
        assertThat(
            GoalsTabService(DedicatedAccountService()).balanceAction(
                viewModel.uiState.value.accounts,
            ),
        ).isEqualTo(com.piplanner.domain.GoalsBalanceAction.Sync)
    }

    @Test
    fun resetDemo_clearsGoalsHistoryAndNavigatesToWelcome() = runTest(dispatcher) {
        persistence.saveState(makePostSetupState(consent = true))
        viewModel = createViewModel()
        viewModel.load()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.hasGoalsOrHistory).isTrue()

        viewModel.requestResetDemo()
        assertThat(viewModel.uiState.value.showResetConfirmation).isTrue()

        viewModel.confirmResetDemo()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.shouldNavigateToWelcome).isTrue()
        assertThat(state.showResetConfirmation).isFalse()
        assertThat(persistence.loadState()).isEqualTo(AppState.EMPTY)
        assertThat(persistence.loadState().goals).isEmpty()
        assertThat(persistence.loadState().history).isEmpty()
        assertThat(persistence.loadState().standingSplits).isEmpty()
        assertThat(persistence.loadState().hasCompletedSetup).isFalse()
    }

    @Test
    fun untypedGapWhileOff_syncLaterArrivesAsOneAmount() = runTest(dispatcher) {
        // Frame 20c: balance grew while consent Off (no typed updates); On sync → one credit.
        val opening = DemoData.SAVINGS_OPENING_BALANCE_PAISA
        val later = MockBalanceSyncService.DEMO_HIGHER_BALANCE_PAISA
        persistence.saveState(makePostSetupState(consent = false).let { state ->
            state.copy(
                accounts = state.accounts.map { account ->
                    if (account.isDedicated) account.copy(balance = opening) else account
                },
            )
        })

        val service = CreditEntryService(
            OpeningSplitService(),
            StandingSplitService(OpeningSplitService()),
        )
        val outcome = service.processFetchedBalance(
            state = persistence.loadState(),
            fetchedBalance = later,
            dedicatedAccountId = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
            isTyped = false,
            id = "credit-gap-1",
            createdAt = Instant.parse("2024-06-01T00:00:00Z").toString(),
        )

        val created = outcome as CreditProcessOutcome.OpenCreditCreated
        assertThat(created.entry.creditAmount).isEqualTo(later - opening)
        assertThat(created.entry.isTyped).isFalse()
        assertThat(created.entry.previousBalance).isEqualTo(opening)
        assertThat(created.entry.newBalance).isEqualTo(later)
    }

    @Test
    fun routesContract_settingsAndSettingsConsent() {
        assertThat(PiPlannerRoutes.SETTINGS).isEqualTo("settings")
        assertThat(PiPlannerRoutes.SETTINGS_CONSENT).isEqualTo("settings_consent")
    }

    private fun createViewModel(): SettingsViewModel {
        val dedicated = DedicatedAccountService()
        return SettingsViewModel(
            repository = PiPlannerRepository(persistence, dispatcher),
            dedicatedAccountService = dedicated,
            goalsTabService = GoalsTabService(dedicated),
            formattingService = FormattingService(),
        )
    }

    private fun makePostSetupState(consent: Boolean): AppState {
        val savings = Account(
            id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
            bankName = DemoData.SAVINGS_BANK,
            maskedNumber = DemoData.SAVINGS_MASKED,
            balance = DemoData.SAVINGS_OPENING_BALANCE_PAISA,
            isDedicated = true,
            isPaytmLinked = true,
            consentAutoUpdate = consent,
        )
        val spending = Account(
            id = DemoData.DEMO_SPENDING_ACCOUNT_ID,
            bankName = DemoData.SPENDING_BANK,
            maskedNumber = DemoData.SPENDING_MASKED,
            balance = DemoData.SPENDING_BALANCE_PAISA,
            isDedicated = false,
            isPaytmLinked = true,
            consentAutoUpdate = false,
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
            accounts = listOf(savings, spending),
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
