package com.piplanner.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.BalanceSyncService
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalsBalanceAction
import com.piplanner.domain.GoalsTabService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model for Goals tab (frames 9 / 9b / 9c / 11) — PIP-46.
 */
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val goalsTabService: GoalsTabService,
    private val dedicatedAccountService: DedicatedAccountService,
    private val balanceSync: BalanceSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

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

    /** Gear → Settings. */
    fun openSettings() {
        _uiState.update { it.copy(navigateToSettings = true) }
    }

    fun consumeSettingsNavigation() {
        _uiState.update { it.copy(navigateToSettings = false) }
    }

    /** Goal card tap → Goal detail. */
    fun selectGoal(goalId: String) {
        _uiState.update { it.copy(selectedGoalId = goalId) }
    }

    fun consumeSelectedGoal() {
        _uiState.update { it.copy(selectedGoalId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Consent On → Sync sheet; Consent Off → Update balance sheet. */
    fun tapBalanceAction() {
        when (_uiState.value.balanceAction) {
            GoalsBalanceAction.Sync ->
                _uiState.update { it.copy(showSyncSheet = true) }
            GoalsBalanceAction.UpdateBalance ->
                _uiState.update { it.copy(showUpdateBalanceSheet = true) }
        }
    }

    fun dismissSyncSheet() {
        _uiState.update { it.copy(showSyncSheet = false) }
    }

    fun dismissUpdateBalanceSheet() {
        _uiState.update { it.copy(showUpdateBalanceSheet = false) }
    }

    /**
     * Sync sheet hook — calls [BalanceSyncService] stub.
     * Full credit-entry assignment is PIP-48.
     */
    fun performSync() {
        viewModelScope.launch {
            val dedicated = dedicatedAccountService.dedicatedAccount(_uiState.value.accounts)
            if (dedicated == null) {
                _uiState.update {
                    it.copy(
                        showSyncSheet = false,
                        errorMessage = "No dedicated savings account.",
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isSyncing = true) }
            val result = balanceSync.fetchBalance(dedicated.id)
            result.fold(
                onSuccess = { paisa ->
                    updateDedicatedBalance(paisa)
                    _uiState.update { it.copy(isSyncing = false, showSyncSheet = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            showSyncSheet = false,
                            errorMessage = error.message ?: error.toString(),
                        )
                    }
                },
            )
        }
    }

    /**
     * Update-balance sheet hook — applies typed amount to dedicated account.
     * Full credit-entry flow is PIP-48.
     */
    fun applyManualBalance(paisa: Long) {
        viewModelScope.launch {
            updateDedicatedBalance(paisa)
            _uiState.update { it.copy(showUpdateBalanceSheet = false) }
        }
    }

    fun formattedSavedAmount(goal: Goal): String =
        formattingService.formatInrFromPaisa(goal.savedAmount)

    fun statusLabel(goal: Goal): String =
        goalsTabService.statusLabel(goal.status())

    private suspend fun updateDedicatedBalance(paisa: Long) {
        val state = repository.loadState()
        val updated = state.copy(
            accounts = state.accounts.map { account ->
                if (account.isDedicated) account.copy(balance = paisa) else account
            },
        )
        repository.saveState(updated)
        apply(updated)
    }

    private fun apply(state: AppState) {
        val action = goalsTabService.balanceAction(state.accounts)
        val total = goalsTabService.totalSavingsPaisa(state.accounts, state.goals)
        _uiState.update {
            it.copy(
                accounts = state.accounts,
                goals = state.goals,
                formattedTotalSavings = formattingService.formatInrFromPaisa(total),
                totalSavingsPaisa = total,
                balanceAction = action,
                balanceActionTitle = goalsTabService.balanceActionTitle(action),
                dedicatedAccountSubtitle = goalsTabService.dedicatedAccountSubtitle(state.accounts),
                hasGoals = goalsTabService.hasGoals(state.goals),
                isLoading = false,
            )
        }
    }
}

data class GoalsUiState(
    val accounts: List<Account> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val formattedTotalSavings: String = "₹0",
    val totalSavingsPaisa: Long = 0L,
    val balanceAction: GoalsBalanceAction = GoalsBalanceAction.UpdateBalance,
    val balanceActionTitle: String = GoalsTabService.ACTION_UPDATE_BALANCE,
    val dedicatedAccountSubtitle: String? = null,
    val hasGoals: Boolean = false,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val showSyncSheet: Boolean = false,
    val showUpdateBalanceSheet: Boolean = false,
    val navigateToSettings: Boolean = false,
    val selectedGoalId: String? = null,
)
