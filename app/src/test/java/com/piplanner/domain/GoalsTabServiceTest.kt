package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.Account
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalStatus
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.util.DemoData
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GoalsTabServiceTest {

    private val dedicatedAccountService = DedicatedAccountService()
    private val service = GoalsTabService(dedicatedAccountService)
    private val formatting = FormattingService()

    @Test
    fun tabBarTitlesAreGoalsHistoryAsk() {
        assertThat(GoalsTabService.TAB_TITLES).containsExactly("Goals", "History", "Ask").inOrder()
        assertThat(GoalsTabService.TAB_TITLES).doesNotContain("Settings")
    }

    @Test
    fun quickActionTitlesMatchDesignOrder() {
        assertThat(GoalsTabService.QUICK_ACTION_TITLES)
            .containsExactly("Sync", "New goal", "Transfer", "History")
            .inOrder()
    }

    @Test
    fun totalSavingsPrefersDedicatedAccountBalance() {
        val accounts = listOf(dedicated(consent = true, balance = 10_000_000L))
        val goals = listOf(
            makeGoal(name = "Car", saved = 6_000_000L),
            makeGoal(name = "Emergency", saved = 4_000_000L),
        )
        val total = service.totalSavingsPaisa(accounts, goals)
        assertThat(total).isEqualTo(10_000_000L)
        assertThat(formatting.formatInrFromPaisa(total)).isEqualTo("₹1,00,000")
    }

    @Test
    fun totalSavingsFallsBackToSumOfGoalSavedAmounts() {
        val goals = listOf(
            makeGoal(name = "Car", saved = 6_000_000L),
            makeGoal(name = "Emergency", saved = 4_000_000L),
        )
        val total = service.totalSavingsPaisa(accounts = emptyList(), goals = goals)
        assertThat(total).isEqualTo(10_000_000L)
        assertThat(formatting.formatInrFromPaisa(total)).isEqualTo("₹1,00,000")
    }

    @Test
    fun consentOnShowsSyncAction() {
        val accounts = listOf(dedicated(consent = true))
        assertThat(service.balanceAction(accounts)).isEqualTo(GoalsBalanceAction.Sync)
        assertThat(service.balanceActionTitle(GoalsBalanceAction.Sync))
            .isEqualTo(GoalsTabService.ACTION_SYNC)
    }

    @Test
    fun consentOffShowsUpdateBalanceAction() {
        val accounts = listOf(dedicated(consent = false))
        assertThat(service.balanceAction(accounts)).isEqualTo(GoalsBalanceAction.UpdateBalance)
        assertThat(service.balanceActionTitle(GoalsBalanceAction.UpdateBalance))
            .isEqualTo(GoalsTabService.ACTION_UPDATE_BALANCE)
    }

    @Test
    fun missingDedicatedDefaultsToUpdateBalance() {
        assertThat(service.balanceAction(emptyList())).isEqualTo(GoalsBalanceAction.UpdateBalance)
    }

    @Test
    fun statusLabelsMatchDesignCopy() {
        assertThat(service.statusLabel(GoalStatus.OnTrack))
            .isEqualTo(GoalsTabService.STATUS_ON_TRACK)
        assertThat(service.statusLabel(GoalStatus.Behind(shortfall = 100L)))
            .isEqualTo(GoalsTabService.STATUS_BEHIND)
    }

    @Test
    fun hasGoalsDetectsPostSetupWithGoals() {
        assertThat(service.hasGoals(emptyList())).isFalse()
        assertThat(service.hasGoals(listOf(makeGoal(name = "Car", saved = 0L)))).isTrue()
    }

    @Test
    fun dedicatedAccountSubtitle() {
        val accounts = listOf(dedicated(consent = true))
        assertThat(service.dedicatedAccountSubtitle(accounts)).isEqualTo("HDFC ••4821")
        assertThat(service.dedicatedAccountSubtitle(emptyList())).isNull()
    }

    // MARK: PIP-82 presentation helpers (visual labels only)

    @Test
    fun quickBalanceActionTitleSyncVsUpdate() {
        assertThat(GoalsTabService.quickBalanceActionTitle(GoalsBalanceAction.Sync))
            .isEqualTo("Sync")
        assertThat(GoalsTabService.quickBalanceActionTitle(GoalsBalanceAction.UpdateBalance))
            .isEqualTo("Update")
        assertThat(service.balanceActionTitle(GoalsBalanceAction.UpdateBalance))
            .isEqualTo("Update balance")
    }

    @Test
    fun lastBalanceActivityLineConsentOnUsesSynced() {
        val now = Instant.parse("2023-11-14T22:13:20Z")
        val line = GoalsTabService.lastBalanceActivityLine(
            action = GoalsBalanceAction.Sync,
            reference = now,
            now = now,
            zoneId = ZoneOffset.UTC,
        )
        assertThat(line).startsWith("Last synced today,")
    }

    @Test
    fun lastBalanceActivityLineConsentOffUsesUpdated() {
        val now = Instant.parse("2023-11-14T22:13:20Z")
        val line = GoalsTabService.lastBalanceActivityLine(
            action = GoalsBalanceAction.UpdateBalance,
            reference = now,
            now = now,
            zoneId = ZoneOffset.UTC,
        )
        assertThat(line).startsWith("Last updated today,")
    }

    @Test
    fun lastBalanceActivityInstantUsesNewestHistory() {
        val older = HistoryEntry(
            id = "older",
            type = HistoryEntryType.OpeningBalance,
            createdAt = "2023-11-14T22:13:20Z",
            isLocked = true,
            newBalance = 10_000_000L,
            creditAmount = 10_000_000L,
        )
        val newer = HistoryEntry(
            id = "newer",
            type = HistoryEntryType.NewCredit,
            createdAt = "2023-11-15T22:13:20Z",
            isLocked = true,
            previousBalance = 10_000_000L,
            newBalance = 11_000_000L,
            creditAmount = 1_000_000L,
        )
        assertThat(GoalsTabService.lastBalanceActivityInstant(listOf(older, newer)))
            .isEqualTo(Instant.parse("2023-11-15T22:13:20Z"))
        assertThat(GoalsTabService.lastBalanceActivityInstant(emptyList())).isNull()
    }

    @Test
    fun goalCardPresentationLabels() {
        assertThat(
            GoalsTabService.savedOfTargetLabel(
                savedPaisa = 6_000_000L,
                targetPaisa = 131_079_600L,
                formatting = formatting,
            ),
        ).isEqualTo("₹60,000 of ₹13,10,796")
        assertThat(
            GoalsTabService.monthlyNeedLabel(
                monthlyNeedPaisa = 2_605_800L,
                formatting = formatting,
            ),
        ).isEqualTo("Needs ₹26,058 a month")
        assertThat(GoalsTabService.creditsPercentLabel(0.6)).isEqualTo("60% of credits")
    }

    private fun dedicated(consent: Boolean, balance: Long = 10_000_000L): Account = Account(
        id = DemoData.DEMO_SAVINGS_ACCOUNT_ID,
        bankName = DemoData.SAVINGS_BANK,
        maskedNumber = DemoData.SAVINGS_MASKED,
        balance = balance,
        isDedicated = true,
        isPaytmLinked = true,
        consentAutoUpdate = consent,
    )

    private fun makeGoal(name: String, saved: Long): Goal {
        val now = Instant.parse("2024-01-01T00:00:00Z")
        val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val end = start.plusYears(1)
        return Goal(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            targetAmount = 50_000_000L,
            startDate = start.toString(),
            endDate = end.toString(),
            savedAmount = saved,
            shareOfNewCredits = 0.5,
            createdAt = now.toString(),
            updatedAt = now.toString(),
        )
    }
}
