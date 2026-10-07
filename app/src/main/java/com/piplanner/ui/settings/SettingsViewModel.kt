package com.piplanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalsTabService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings (frames 20 / 20a / 20b / 20c) — PRD R17 / Spec §5.3.
 * Consent toggle + Reset demo; gear navigation owned by Goals.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val dedicatedAccountService: DedicatedAccountService,
    private val goalsTabService: GoalsTabService,
    private val formattingService: FormattingService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeState().collect { state ->
                apply(state)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            apply(repository.loadState())
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Automatic balance updates switch.
     * Off → On (20b): request Consent sheet (do not persist On until Yes).
     * On → Off (20a): persist `consentAutoUpdate = false` immediately.
     */
    fun onConsentToggle(enabled: Boolean) {
        val current = _uiState.value.consentAutoUpdate
        when {
            enabled && !current -> {
                _uiState.update { it.copy(navigateToConsent = true) }
            }
            !enabled && current -> {
                viewModelScope.launch { persistConsent(autoUpdate = false) }
            }
        }
    }

    fun consumeConsentNavigation() {
        _uiState.update { it.copy(navigateToConsent = false) }
    }

    fun requestResetDemo() {
        _uiState.update { it.copy(showResetConfirmation = true) }
    }

    fun dismissResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = false) }
    }

    /** Clears all persisted demo state and returns to Welcome (R17). */
    fun confirmResetDemo() {
        if (_uiState.value.isWorking) return
        _uiState.update {
            it.copy(isWorking = true, showResetConfirmation = false, errorMessage = null)
        }
        viewModelScope.launch {
            try {
                repository.resetDemo()
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        shouldNavigateToWelcome = true,
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        errorMessage = error.message ?: "Couldn’t reset demo",
                    )
                }
            }
        }
    }

    fun consumeWelcomeNavigation() {
        _uiState.update { it.copy(shouldNavigateToWelcome = false) }
    }

    fun formattedBalance(paisa: Long): String = formattingService.formatInrFromPaisa(paisa)

    private suspend fun persistConsent(autoUpdate: Boolean) {
        try {
            val persisted = repository.loadState()
            val updatedAccounts = persisted.accounts.map { account ->
                if (account.isDedicated) {
                    account.copy(consentAutoUpdate = autoUpdate)
                } else {
                    account
                }
            }
            repository.saveState(persisted.copy(accounts = updatedAccounts))
            apply(persisted.copy(accounts = updatedAccounts))
        } catch (error: Exception) {
            _uiState.update {
                it.copy(errorMessage = error.message ?: "Couldn’t save consent")
            }
        }
    }

    private fun apply(state: AppState) {
        val dedicated = dedicatedAccountService.dedicatedAccount(state.accounts)
        val linked = state.accounts.map { account ->
            LinkedAccountRow(
                id = account.id,
                title = goalsTabService.displayTitle(account),
                subtitle = linkedAccountSubtitle(account),
                isDedicated = account.isDedicated,
                formattedBalance = formattingService.formatInrFromPaisa(account.balance),
            )
        }
        _uiState.update {
            it.copy(
                accounts = state.accounts,
                linkedAccounts = linked,
                dedicatedAccountTitle = dedicated?.let { account ->
                    goalsTabService.displayTitle(account)
                },
                consentAutoUpdate = dedicated?.consentAutoUpdate == true,
                hasGoalsOrHistory = state.goals.isNotEmpty() ||
                    state.history.isNotEmpty() ||
                    state.standingSplits.isNotEmpty() ||
                    state.hasCompletedSetup,
            )
        }
    }

    private fun linkedAccountSubtitle(account: Account): String {
        return buildString {
            append(if (account.isDedicated) "Dedicated savings" else "Spending")
            if (account.isPaytmLinked) append(" · Paytm linked")
        }
    }
}

data class LinkedAccountRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val isDedicated: Boolean,
    val formattedBalance: String,
)

data class SettingsUiState(
    val accounts: List<Account> = emptyList(),
    val linkedAccounts: List<LinkedAccountRow> = emptyList(),
    val dedicatedAccountTitle: String? = null,
    val consentAutoUpdate: Boolean = false,
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val navigateToConsent: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val shouldNavigateToWelcome: Boolean = false,
    val hasGoalsOrHistory: Boolean = false,
)
