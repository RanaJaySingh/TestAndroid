package com.piplanner.ui.setup

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.local.PersistenceService
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.MockBalanceSyncService
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
class ConsentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var persistence: FakePersistence
    private lateinit var viewModel: ConsentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        persistence = FakePersistence()
        viewModel = ConsentViewModel(
            repository = PiPlannerRepository(persistence, dispatcher),
            balanceSync = MockBalanceSyncService(
                knownAccountIds = setOf(DemoData.DEMO_SAVINGS_ACCOUNT_ID),
            ),
            dedicatedAccountService = DedicatedAccountService(),
            formattingService = FormattingService(),
        )
        viewModel.configure(dedicatedAccounts())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun consentYes_fetchesDemoBalance() = runTest(dispatcher) {
        viewModel.chooseConsentYes()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.resolvedBalancePaisa).isEqualTo(10_000_000L)
        assertThat(state.consentAutoUpdate).isTrue()
        assertThat(state.shouldShowFetchedBalance).isTrue()
        assertThat(viewModel.formattedBalance(state.resolvedBalancePaisa!!))
            .isEqualTo("₹1,00,000")
    }

    /**
     * PIP-62 / iOS PIP-61 parity: Settings Off→On Yes persists consent only.
     * Must not overwrite dedicated balance with setup mock ₹1,00,000 (10_000_000 paisa).
     */
    @Test
    fun settingsConsentYes_persistsConsentWithoutOverwritingBalance() = runTest(dispatcher) {
        val customBalance = 12_345_678L // not DEMO_BALANCE / ₹1,00,000
        val accounts = dedicatedAccounts().map { account ->
            if (account.isDedicated) account.copy(balance = customBalance) else account
        }
        persistence.saveState(AppState(accounts = accounts, hasCompletedSetup = true))
        viewModel.configure(accounts = accounts, fetchesBalanceOnYes = false)

        viewModel.chooseConsentYes()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.consentAutoUpdate).isTrue()
        assertThat(state.shouldShowFetchedBalance).isTrue()
        assertThat(state.resolvedBalancePaisa).isNull()

        val dedicated = persistence.loadState().accounts.single { it.isDedicated }
        assertThat(dedicated.consentAutoUpdate).isTrue()
        assertThat(dedicated.balance).isEqualTo(customBalance)
        assertThat(dedicated.balance).isNotEqualTo(MockBalanceSyncService.DEMO_BALANCE_PAISA)
        assertThat(dedicated.balance).isNotEqualTo(10_000_000L)
    }

    @Test
    fun consentNo_clearsResolvedBalance_andShowsUpdateSheet() = runTest(dispatcher) {
        viewModel.chooseConsentNo()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.consentAutoUpdate).isFalse()
        assertThat(state.resolvedBalancePaisa).isNull()
        assertThat(state.shouldShowUpdateBalance).isTrue()
    }

    @Test
    fun manualZero_disablesContinue() = runTest(dispatcher) {
        viewModel.setManualRupeeDigits("0")
        assertThat(viewModel.uiState.value.canContinueManual).isFalse()
        viewModel.continueManual()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.shouldContinueAfterBalance).isFalse()

        viewModel.setManualRupeeDigits("2500")
        assertThat(viewModel.uiState.value.canContinueManual).isTrue()
        viewModel.continueManual()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.resolvedBalancePaisa).isEqualTo(250_000L)
        assertThat(state.shouldContinueAfterBalance).isTrue()
    }

    @Test
    fun pinSuccessAndFailure() = runTest(dispatcher) {
        "1234".forEach { viewModel.appendPinDigit(it.toString()) }
        viewModel.checkBalanceWithPin()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertThat(state.lastPinOutcome).isEqualTo(UpiPinCheckOutcome.Success(10_000_000L))
        assertThat(state.resolvedBalancePaisa).isEqualTo(10_000_000L)
        assertThat(state.shouldShowFetchedBalance).isTrue()

        viewModel.clearPin()
        viewModel.consumeNavigation()
        "0000".forEach { viewModel.appendPinDigit(it.toString()) }
        viewModel.checkBalanceWithPin()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertThat(state.lastPinOutcome).isEqualTo(UpiPinCheckOutcome.WrongPin)
        assertThat(state.shouldShowWrongPin).isTrue()
        assertThat(state.errorMessage).isNotNull()
    }

    @Test
    fun otherApp_forcesManualPathSignal() {
        viewModel.accountOnOtherUpiApp()
        val state = viewModel.uiState.value
        assertThat(state.lastPinOutcome).isEqualTo(UpiPinCheckOutcome.OtherApp)
        assertThat(state.shouldShowOtherApp).isTrue()
        assertThat(state.pinDigits).isEmpty()
    }

    private fun dedicatedAccounts(): List<Account> = listOf(
        Account(
            id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
            bankName = DemoData.SAVINGS_BANK,
            maskedNumber = DemoData.SAVINGS_MASKED,
            balance = DemoData.SAVINGS_OPENING_BALANCE_PAISA,
            isDedicated = true,
            isPaytmLinked = true,
            consentAutoUpdate = false,
        ),
        Account(
            id = DemoData.DEMO_SPENDING_ACCOUNT_ID,
            bankName = DemoData.SPENDING_BANK,
            maskedNumber = DemoData.SPENDING_MASKED,
            balance = DemoData.SPENDING_BALANCE_PAISA,
            isDedicated = false,
            isPaytmLinked = true,
            consentAutoUpdate = false,
        ),
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
