package com.piplanner.domain

import com.piplanner.data.model.Account
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated-account exclusivity (PRD R2 / R21, Spec BR-1).
 * Exactly one account may be marked dedicated savings; Continue requires that state.
 */
@Singleton
class DedicatedAccountService @Inject constructor() {

    /**
     * Applies a Dedicated toggle.
     * Turning [accountId] ON clears dedicated on every other account.
     * Turning it OFF leaves zero dedicated accounts.
     */
    fun withDedicatedToggle(
        accounts: List<Account>,
        accountId: String,
        dedicated: Boolean,
    ): List<Account> {
        require(accounts.any { it.id == accountId }) {
            "Unknown account id: $accountId"
        }
        return accounts.map { account ->
            when {
                account.id == accountId -> account.copy(isDedicated = dedicated)
                dedicated -> account.copy(isDedicated = false)
                else -> account
            }
        }
    }

    /** True when exactly one account has [Account.isDedicated] = true (BR-1). */
    fun hasExactlyOneDedicated(accounts: List<Account>): Boolean =
        accounts.count { it.isDedicated } == 1

    /** Continue is enabled only when exactly one account is dedicated. */
    fun canContinue(accounts: List<Account>): Boolean = hasExactlyOneDedicated(accounts)

    fun dedicatedAccount(accounts: List<Account>): Account? =
        accounts.singleOrNull { it.isDedicated }
}
