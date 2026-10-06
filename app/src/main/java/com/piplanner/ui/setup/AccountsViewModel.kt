package com.piplanner.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Account
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model for Accounts (frame 2) — PRD R2 / R21; Spec BR-1.
 * Exactly one Dedicated savings toggle; Continue gated until that state.
 */
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val dedicatedAccountService: DedicatedAccountService,
    private val formattingService: FormattingService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    /**
     * Seeds the screen with demo (or restored) accounts.
     * Call once when the destination is shown; safe to call again with the same inputs.
     */
    fun configure(accounts: List<Account>) {
        publish(accounts = accounts, clearError = true)
    }

    /** BR-1: toggling Dedicated ON for one account turns the others OFF. */
    fun setDedicated(accountId: String, dedicated: Boolean) {
        val current = _uiState.value.accounts
        if (current.none { it.id == accountId }) return
        val next = dedicatedAccountService.withDedicatedToggle(
            accounts = current,
            accountId = accountId,
            dedicated = dedicated,
        )
        publish(accounts = next, clearError = true)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(shouldNavigateToConsent = false) }
    }

    /**
     * Persists accounts (with dedicated flag) and signals navigation to Consent (PIP-40 placeholder).
     */
    fun onContinue() {
        val state = _uiState.value
        if (!state.canContinue || state.isSaving) return
        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessage = null,
                canContinue = false,
            )
        }

        viewModelScope.launch {
            try {
                val persisted = repository.loadState()
                repository.saveState(persisted.copy(accounts = state.accounts))
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        canContinue = dedicatedAccountService.canContinue(it.accounts),
                        shouldNavigateToConsent = true,
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Couldn’t save dedicated account",
                        canContinue = dedicatedAccountService.canContinue(it.accounts),
                    )
                }
            }
        }
    }

    fun formattedBalance(accountId: String): String {
        val paisa = _uiState.value.accounts.firstOrNull { it.id == accountId }?.balance ?: 0L
        return formattingService.formatInrFromPaisa(paisa)
    }

    private fun publish(accounts: List<Account>, clearError: Boolean) {
        _uiState.update { current ->
            current.copy(
                accounts = accounts,
                canContinue = dedicatedAccountService.canContinue(accounts) && !current.isSaving,
                statusMessage = buildStatusMessage(accounts),
                errorMessage = if (clearError) null else current.errorMessage,
            )
        }
    }

    private fun buildStatusMessage(accounts: List<Account>): String {
        return when (accounts.count { it.isDedicated }) {
            0 -> "Pick one Dedicated savings account to continue."
            1 -> {
                val dedicated = dedicatedAccountService.dedicatedAccount(accounts)!!
                "Dedicated: ${dedicated.bankName} ${dedicated.maskedNumber}."
            }
            else -> "Only one Dedicated savings account is allowed."
        }
    }
}

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val canContinue: Boolean = false,
    val isSaving: Boolean = false,
    val shouldNavigateToConsent: Boolean = false,
    val statusMessage: String = "",
    val errorMessage: String? = null,
) {
    val hasExactlyOneDedicated: Boolean
        get() = accounts.count { it.isDedicated } == 1

    val hasNoneDedicated: Boolean
        get() = accounts.none { it.isDedicated }
}
