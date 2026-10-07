package com.piplanner.util

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.AppState
import com.piplanner.domain.FormattingService
import com.piplanner.domain.MockBalanceSyncService
import com.piplanner.domain.StubGrokService
import com.piplanner.domain.SyncError
import com.piplanner.domain.SyncException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DemoDataTest {

    private val formatting = FormattingService()

    @Test
    fun initializeDemo_firstLaunch_seedsRahulAccountsNoneDedicated() {
        val state = DemoData.initializeDemo(withDedicatedSavings = false)

        assertThat(DemoData.isFirstLaunchOrPostReset(state)).isTrue()
        assertThat(DemoData.PERSONA_NAME).isEqualTo("Rahul")
        assertThat(state.accounts).hasSize(2)
        assertThat(state.accounts.none { it.isDedicated }).isTrue()
        assertThat(state.goals).isEmpty()
        assertThat(state.history).isEmpty()
        assertThat(state.hasCompletedSetup).isFalse()

        val hdfc = state.accounts.first { it.id == DemoData.DEMO_SAVINGS_ACCOUNT_ID }
        val sbi = state.accounts.first { it.id == DemoData.DEMO_SPENDING_ACCOUNT_ID }
        assertThat(hdfc.bankName).isEqualTo("HDFC")
        assertThat(hdfc.maskedNumber).isEqualTo("••4821")
        assertThat(hdfc.balance).isEqualTo(DemoData.SAVINGS_OPENING_BALANCE_PAISA)
        assertThat(formatting.formatInrFromPaisa(hdfc.balance)).isEqualTo("₹1,00,000")
        assertThat(sbi.bankName).isEqualTo("SBI")
        assertThat(sbi.maskedNumber).isEqualTo("••7730")
        assertThat(sbi.balance).isEqualTo(DemoData.SPENDING_BALANCE_PAISA)
        assertThat(formatting.formatInrFromPaisa(sbi.balance)).isEqualTo("₹72,000")
    }

    @Test
    fun seededPersonaAccounts_hdfcDedicated_sbiSpending() {
        val accounts = DemoData.seededPersonaAccounts()
        val hdfc = accounts.first { it.id == DemoData.DEMO_SAVINGS_ACCOUNT_ID }
        val sbi = accounts.first { it.id == DemoData.DEMO_SPENDING_ACCOUNT_ID }

        assertThat(hdfc.isDedicated).isTrue()
        assertThat(sbi.isDedicated).isFalse()
        assertThat(DemoData.isSpendingAccount(sbi)).isTrue()
        assertThat(DemoData.isBalanceTracked(hdfc)).isTrue()
        assertThat(DemoData.isBalanceTracked(sbi)).isFalse()
        assertThat(formatting.formatInrFromPaisa(hdfc.balance)).isEqualTo("₹1,00,000")
        assertThat(formatting.formatInrFromPaisa(sbi.balance)).isEqualTo("₹72,000")
    }

    @Test
    fun initializeDemo_postReset_matchesFirstLaunchEmptyLedger() {
        val afterReset = DemoData.initializeDemo()
        assertThat(DemoData.isFirstLaunchOrPostReset(afterReset)).isTrue()
        assertThat(DemoData.isFirstLaunchOrPostReset(AppState.EMPTY)).isTrue()

        val setupComplete = afterReset.copy(hasCompletedSetup = true)
        assertThat(DemoData.isFirstLaunchOrPostReset(setupComplete)).isFalse()
    }

    @Test
    fun greeting_adjustsForTimeOfDay_includesRahul() {
        assertThat(DemoData.greeting(hourOfDay = 8)).isEqualTo("Good morning, Rahul")
        assertThat(DemoData.greeting(hourOfDay = 14)).isEqualTo("Good afternoon, Rahul")
        assertThat(DemoData.greeting(hourOfDay = 19)).isEqualTo("Good evening, Rahul")
        assertThat(DemoData.greeting(hourOfDay = 2)).isEqualTo("Good evening, Rahul")

        val eveningClock = Clock.fixed(Instant.parse("2026-10-07T19:30:00Z"), ZoneOffset.UTC)
        assertThat(DemoData.greeting(clock = eveningClock, zoneId = ZoneOffset.UTC))
            .isEqualTo("Good evening, Rahul")
    }

    @Test
    fun spendingAccount_notTrackedByBalanceSync_r25() = runTest {
        val service = MockBalanceSyncService(
            knownAccountIds = DemoData.trackedAccountIds(),
        )
        val savings = service.fetchBalance(DemoData.DEMO_SAVINGS_ACCOUNT_ID)
        assertThat(savings.isSuccess).isTrue()

        val spending = service.fetchBalance(DemoData.DEMO_SPENDING_ACCOUNT_ID)
        assertThat(spending.isFailure).isTrue()
        assertThat((spending.exceptionOrNull() as SyncException).error)
            .isEqualTo(SyncError.AccountNotFound)
        assertThat(DemoData.trackedAccountIds())
            .doesNotContain(DemoData.DEMO_SPENDING_ACCOUNT_ID)
    }

    @Test
    fun grokHappyPath_matchesDesignCarAndEmergencyFund() {
        assertThat(DemoData.happyPathProposalNames())
            .containsExactly("Car", "Emergency Fund")
            .inOrder()
        val proposals = StubGrokService.HAPPY_PATH_PROPOSALS
        assertThat(proposals[0].name).isEqualTo(DemoData.HAPPY_PATH_CAR_NAME)
        assertThat(proposals[0].sharePercentage).isEqualTo(0.6)
        assertThat(proposals[1].name).isEqualTo(DemoData.HAPPY_PATH_EMERGENCY_NAME)
        assertThat(proposals[1].sharePercentage).isEqualTo(0.4)

        val goals = DemoData.sampleOpeningSplitGoals()
        assertThat(goals.map { it.name }).containsExactly("Car", "Emergency Fund").inOrder()
    }

    @Test
    fun spendingAccountNote_documentsR25() {
        assertThat(DemoData.SPENDING_ACCOUNT_NOTE.lowercase()).contains("not seen")
        assertThat(DemoData.SPENDING_ACCOUNT_NOTE.lowercase()).contains("spending")
    }
}
