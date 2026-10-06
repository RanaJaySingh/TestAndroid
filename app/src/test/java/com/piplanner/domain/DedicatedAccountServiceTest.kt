package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.util.DemoData
import org.junit.Test

class DedicatedAccountServiceTest {

    private val service = DedicatedAccountService()
    private val accounts = DemoData.sampleAccounts()

    @Test
    fun toggleOn_turnsOthersOff() {
        val hdfcOn = service.withDedicatedToggle(
            accounts = accounts,
            accountId = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
            dedicated = true,
        )
        assertThat(hdfcOn.single { it.id == DemoData.DEMO_SAVINGS_ACCOUNT_ID }.isDedicated).isTrue()
        assertThat(hdfcOn.single { it.id == DemoData.DEMO_SPENDING_ACCOUNT_ID }.isDedicated).isFalse()
        assertThat(service.hasExactlyOneDedicated(hdfcOn)).isTrue()

        val sbiOn = service.withDedicatedToggle(
            accounts = hdfcOn,
            accountId = DemoData.DEMO_SPENDING_ACCOUNT_ID,
            dedicated = true,
        )
        assertThat(sbiOn.single { it.id == DemoData.DEMO_SPENDING_ACCOUNT_ID }.isDedicated).isTrue()
        assertThat(sbiOn.single { it.id == DemoData.DEMO_SAVINGS_ACCOUNT_ID }.isDedicated).isFalse()
        assertThat(service.hasExactlyOneDedicated(sbiOn)).isTrue()
    }

    @Test
    fun toggleOff_leavesNoneDedicated_andContinueDisabled() {
        val dedicated = service.withDedicatedToggle(
            accounts = accounts,
            accountId = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
            dedicated = true,
        )
        assertThat(service.canContinue(dedicated)).isTrue()

        val none = service.withDedicatedToggle(
            accounts = dedicated,
            accountId = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
            dedicated = false,
        )
        assertThat(none.none { it.isDedicated }).isTrue()
        assertThat(service.hasExactlyOneDedicated(none)).isFalse()
        assertThat(service.canContinue(none)).isFalse()
    }

    @Test
    fun sampleAccounts_startWithNoneDedicated() {
        assertThat(accounts).hasSize(2)
        assertThat(accounts.map { "${it.bankName} ${it.maskedNumber}" })
            .containsExactly("HDFC ••4821", "SBI ••7730")
            .inOrder()
        assertThat(service.canContinue(accounts)).isFalse()
    }
}
