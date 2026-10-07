package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.util.DemoData
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BalanceSyncServiceTest {

    private val hdfcId = DemoData.DEMO_SAVINGS_ACCOUNT_ID
    private val unknownId = "99999999-9999-9999-9999-999999999999"

    @Test
    fun fetchBalanceYesPath_returnsOneLakhRupees() = runTest {
        val service = MockBalanceSyncService(knownAccountIds = setOf(hdfcId))
        val result = service.fetchBalance(hdfcId)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(10_000_000L)
        assertThat(result.getOrNull()).isEqualTo(MockBalanceSyncService.DEMO_BALANCE_PAISA)
        assertThat(FormattingService().formatInrFromPaisa(result.getOrNull()!!))
            .isEqualTo("₹1,00,000")
    }

    @Test
    fun fetchBalance_unknownAccount_fails() = runTest {
        val service = MockBalanceSyncService(knownAccountIds = setOf(hdfcId))
        val result = service.fetchBalance(unknownId)

        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as SyncException
        assertThat(error.error).isEqualTo(SyncError.AccountNotFound)
    }

    @Test
    fun fetchBalance_spendingAccount_notTracked_r25() = runTest {
        val service = MockBalanceSyncService(knownAccountIds = DemoData.trackedAccountIds())
        val result = service.fetchBalance(DemoData.DEMO_SPENDING_ACCOUNT_ID)

        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as SyncException
        assertThat(error.error).isEqualTo(SyncError.AccountNotFound)
    }

    @Test
    fun fetchBalance_emptyKnownSet_alwaysSucceeds() = runTest {
        val service = MockBalanceSyncService()
        val result = service.fetchBalance(unknownId)
        assertThat(result.getOrNull()).isEqualTo(10_000_000L)
    }

    @Test
    fun verifyUpiPin_demoSucceeds() = runTest {
        val service = MockBalanceSyncService()
        val result = service.verifyUpiPin(MockBalanceSyncService.DEMO_PIN)
        assertThat(result.getOrNull()).isEqualTo(10_000_000L)
        assertThat(MockBalanceSyncService.DEMO_PIN).isEqualTo("1234")
    }

    @Test
    fun verifyUpiPin_wrongPinFails() = runTest {
        val service = MockBalanceSyncService()
        val result = service.verifyUpiPin("0000")
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as PinException
        assertThat(error.error).isEqualTo(PinError.WrongPin)
    }

    @Test
    fun accountOnOtherUpiApp_forcesOtherAppError() {
        val service = MockBalanceSyncService()
        assertThat(service.accountOnOtherUpiApp()).isEqualTo(PinError.OtherApp)
    }

    @Test
    fun manualContinue_disabledAtZero() {
        assertThat(ConsentService.canContinueManual(0L)).isFalse()
        assertThat(ConsentService.canContinueManual(100L)).isTrue()
        assertThat(ConsentService.paisaFromRupeeDigits("")).isEqualTo(0L)
        assertThat(ConsentService.paisaFromRupeeDigits("0")).isEqualTo(0L)
        assertThat(ConsentService.paisaFromRupeeDigits("100000")).isEqualTo(10_000_000L)
        assertThat(ConsentService.paisaFromRupeeDigits("₹1,00,000")).isEqualTo(10_000_000L)
    }

    @Test
    fun pinCompleteness() {
        assertThat(ConsentService.isCompletePin("")).isFalse()
        assertThat(ConsentService.isCompletePin("123")).isFalse()
        assertThat(ConsentService.isCompletePin("12ab")).isFalse()
        assertThat(ConsentService.isCompletePin("1234")).isTrue()
    }

    @Test
    fun consentBullets_matchPrdThemes() {
        val joined = ConsentService.consentBullets.joinToString(" ").lowercase()
        assertThat(ConsentService.consentBullets).hasSize(4)
        assertThat(joined).contains("balance")
        assertThat(joined).contains("grok")
        assertThat(joined).contains("settings")
    }
}
