package com.piplanner.util

import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Rahul demo persona seeding (PIP-66 / PRD §10 / Spec §3.1 / A8).
 *
 * First launch and post–Reset demo expose empty [AppState]; Accounts is seeded from
 * [sampleAccounts] (none dedicated yet for BR-1). Canonical persona accounts with
 * HDFC dedicated are available via [seededPersonaAccounts].
 *
 * Spending SBI payments are never ledgered (R25) — only the dedicated savings account
 * is balance-tracked ([trackedAccountIds]).
 */
object DemoData {
    const val PERSONA_NAME: String = "Rahul"
    const val SAVINGS_BANK: String = "HDFC"
    const val SAVINGS_MASKED: String = "••4821"
    const val SPENDING_BANK: String = "SBI"
    const val SPENDING_MASKED: String = "••7730"
    const val SAVINGS_OPENING_BALANCE_PAISA: Long = 10_000_000L
    const val SPENDING_BALANCE_PAISA: Long = 7_200_000L

    const val DEMO_SAVINGS_ACCOUNT_ID: String = "11111111-1111-1111-1111-111111111111"
    const val DEMO_SPENDING_ACCOUNT_ID: String = "22222222-2222-2222-2222-222222222222"
    const val DEMO_CAR_GOAL_ID: String = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    const val DEMO_EMERGENCY_GOAL_ID: String = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"

    /** Design / Grok stub happy-path goal names (Car / Emergency Fund). */
    const val HAPPY_PATH_CAR_NAME: String = "Car"
    const val HAPPY_PATH_EMERGENCY_NAME: String = "Emergency Fund"

    /**
     * Accounts screen seed (frame 2): HDFC ••4821 and SBI ••7730 with none dedicated yet
     * so Continue starts disabled (BR-1 / R2). Used on first launch and after Reset demo.
     */
    fun sampleAccounts(): List<Account> = listOf(
        Account(
            id = DEMO_SAVINGS_ACCOUNT_ID,
            bankName = SAVINGS_BANK,
            maskedNumber = SAVINGS_MASKED,
            balance = SAVINGS_OPENING_BALANCE_PAISA,
            isDedicated = false,
            isPaytmLinked = true,
            consentAutoUpdate = false,
        ),
        Account(
            id = DEMO_SPENDING_ACCOUNT_ID,
            bankName = SPENDING_BANK,
            maskedNumber = SPENDING_MASKED,
            balance = SPENDING_BALANCE_PAISA,
            isDedicated = false,
            isPaytmLinked = true,
            consentAutoUpdate = false,
        ),
    )

    /**
     * Canonical persona seed (PRD §10): HDFC ••4821 ₹1,00,000 dedicated;
     * SBI ••7730 ₹72,000 spending (not tracked).
     */
    fun seededPersonaAccounts(): List<Account> = sampleAccounts().map { account ->
        when (account.id) {
            DEMO_SAVINGS_ACCOUNT_ID -> account.copy(isDedicated = true)
            else -> account.copy(isDedicated = false)
        }
    }

    /**
     * Demo initialization snapshot for unit tests and first-launch / post-reset wiring.
     * Empty goals/history; accounts from [sampleAccounts] (setup) or [seededPersonaAccounts].
     */
    fun initializeDemo(
        withDedicatedSavings: Boolean = false,
    ): AppState {
        return AppState(
            accounts = if (withDedicatedSavings) seededPersonaAccounts() else sampleAccounts(),
            goals = emptyList(),
            history = emptyList(),
            standingSplits = emptyList(),
            hasCompletedSetup = false,
        )
    }

    /** True when state matches first-run or post–Reset demo (empty ledger). */
    fun isFirstLaunchOrPostReset(state: AppState): Boolean {
        return state.goals.isEmpty() &&
            state.history.isEmpty() &&
            !state.hasCompletedSetup
    }

    /** Account IDs whose balance Sync/Update may create PiPlanner history (dedicated savings only). */
    fun trackedAccountIds(): Set<String> = setOf(DEMO_SAVINGS_ACCOUNT_ID)

    fun isSpendingAccount(accountId: String): Boolean =
        accountId == DEMO_SPENDING_ACCOUNT_ID

    fun isSpendingAccount(account: Account): Boolean =
        isSpendingAccount(account.id)

    /**
     * R25 — spending-account payments are never shown in PiPlanner.
     * Only the dedicated savings account participates in Sync / credits / withdrawals.
     */
    fun isBalanceTracked(account: Account): Boolean =
        account.id in trackedAccountIds() && account.isDedicated

    /** Spending note for Accounts (2) — PRD R25. */
    const val SPENDING_ACCOUNT_NOTE: String =
        "Spending account — everyday payments are not seen in PiPlanner."

    /**
     * Time-of-day greeting with fixed persona name (PRD A8).
     * Morning 05–11, afternoon 12–16, evening otherwise (incl. night).
     */
    fun greeting(
        hourOfDay: Int = LocalTime.now(Clock.systemDefaultZone()).hour,
        name: String = PERSONA_NAME,
    ): String {
        val salutation = when (hourOfDay) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        return "$salutation, $name"
    }

    fun greeting(
        clock: Clock,
        zoneId: ZoneId = ZoneId.systemDefault(),
        name: String = PERSONA_NAME,
    ): String {
        val hour = LocalTime.now(clock.withZone(zoneId)).hour
        return greeting(hourOfDay = hour, name = name)
    }

    /** Car 60% / Emergency 40% — matches design happy-path proposal for Opening split demos. */
    fun sampleOpeningSplitGoals(now: Instant = Instant.now()): List<Goal> {
        val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val end = start.plusYears(1)
        val createdAt = now.toString()
        return listOf(
            Goal(
                id = DEMO_CAR_GOAL_ID,
                name = HAPPY_PATH_CAR_NAME,
                targetAmount = 50_000_000L,
                startDate = start.toString(),
                endDate = end.toString(),
                savedAmount = 0L,
                shareOfNewCredits = 0.6,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
            Goal(
                id = DEMO_EMERGENCY_GOAL_ID,
                name = HAPPY_PATH_EMERGENCY_NAME,
                targetAmount = 20_000_000L,
                startDate = start.toString(),
                endDate = end.toString(),
                savedAmount = 0L,
                shareOfNewCredits = 0.4,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
        )
    }

    /** Happy-path Grok proposal names aligned with StubGrokService (Car / Emergency Fund). */
    fun happyPathProposalNames(): List<String> =
        listOf(HAPPY_PATH_CAR_NAME, HAPPY_PATH_EMERGENCY_NAME)
}
