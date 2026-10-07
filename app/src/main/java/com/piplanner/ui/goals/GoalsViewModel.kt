package com.piplanner.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.di.PostSetupBalanceSync
import com.piplanner.domain.BalanceCompareResult
import com.piplanner.domain.BalanceSyncService
import com.piplanner.domain.CreditEntryException
import com.piplanner.domain.CreditEntryService
import com.piplanner.domain.CreditProcessOutcome
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
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * View model for Goals tab (frames 9 / 9b / 9c / 11) with Sync/Update credit flow (PIP-48).
 */
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val goalsTabService: GoalsTabService,
    private val dedicatedAccountService: DedicatedAccountService,
    @PostSetupBalanceSync private val balanceSync: BalanceSyncService,
    private val creditEntryService: CreditEntryService,
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

    fun openSettings() {
        _uiState.update { it.copy(navigateToSettings = true) }
    }

    fun consumeSettingsNavigation() {
        _uiState.update { it.copy(navigateToSettings = false) }
    }

    fun selectGoal(goalId: String) {
        _uiState.update { it.copy(selectedGoalId = goalId) }
    }

    fun consumeSelectedGoal() {
        _uiState.update { it.copy(selectedGoalId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Consent On → Sync sheet; Consent Off → Update balance sheet. Blocked by open entry (BR-6). */
    fun tapBalanceAction() {
        if (_uiState.value.isSyncOrUpdateBlocked) {
            _uiState.update {
                it.copy(
                    errorMessage = when (it.balanceAction) {
                        GoalsBalanceAction.Sync -> CreditEntryService.ASSIGN_OPEN_BEFORE_SYNC
                        GoalsBalanceAction.UpdateBalance ->
                            CreditEntryService.ASSIGN_OPEN_BEFORE_UPDATE
                    },
                )
            }
            return
        }
        when (_uiState.value.balanceAction) {
            GoalsBalanceAction.Sync ->
                _uiState.update {
                    it.copy(
                        showSyncSheet = true,
                        syncPhase = SyncSheetPhase.Idle,
                        syncInfoMessage = null,
                        syncErrorMessage = null,
                        createdCreditEntryId = null,
                        withdrawalShortfallPaisa = null,
                        formattedFetchedBalance = null,
                        formattedNewCreditAmount = null,
                    )
                }
            GoalsBalanceAction.UpdateBalance ->
                _uiState.update {
                    it.copy(
                        showUpdateBalanceSheet = true,
                        updateInfoMessage = null,
                        updateErrorMessage = null,
                        createdCreditEntryId = null,
                        withdrawalShortfallPaisa = null,
                    )
                }
        }
    }

    fun dismissSyncSheet() {
        _uiState.update {
            it.copy(
                showSyncSheet = false,
                syncPhase = SyncSheetPhase.Idle,
                isSyncing = false,
            )
        }
    }

    fun dismissUpdateBalanceSheet() {
        _uiState.update { it.copy(showUpdateBalanceSheet = false) }
    }

    fun performSync() {
        viewModelScope.launch {
            if (_uiState.value.isSyncOrUpdateBlocked) {
                _uiState.update {
                    it.copy(syncInfoMessage = CreditEntryService.ASSIGN_OPEN_BEFORE_SYNC)
                }
                return@launch
            }
            val dedicated = dedicatedAccountService.dedicatedAccount(_uiState.value.accounts)
            if (dedicated == null) {
                _uiState.update {
                    it.copy(
                        syncPhase = SyncSheetPhase.ShowingResult,
                        syncErrorMessage = "No dedicated savings account.",
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncPhase = SyncSheetPhase.Syncing,
                    syncInfoMessage = null,
                    syncErrorMessage = null,
                    createdCreditEntryId = null,
                    withdrawalShortfallPaisa = null,
                    formattedPreviousBalance = formattingService.formatInrFromPaisa(dedicated.balance),
                )
            }

            val result = balanceSync.fetchBalance(dedicated.id)
            result.fold(
                onSuccess = { paisa ->
                    processBalanceOutcome(
                        fetchedBalance = paisa,
                        dedicatedAccountId = dedicated.id,
                        isTyped = false,
                        previousBalance = dedicated.balance,
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncPhase = SyncSheetPhase.ShowingResult,
                            syncErrorMessage = error.message ?: error.toString(),
                        )
                    }
                },
            )
        }
    }

    fun applyManualBalance(paisa: Long) {
        viewModelScope.launch {
            if (_uiState.value.isSyncOrUpdateBlocked) {
                _uiState.update {
                    it.copy(updateInfoMessage = CreditEntryService.ASSIGN_OPEN_BEFORE_UPDATE)
                }
                return@launch
            }
            val dedicated = dedicatedAccountService.dedicatedAccount(_uiState.value.accounts)
            if (dedicated == null) {
                _uiState.update {
                    it.copy(updateErrorMessage = "No dedicated savings account.")
                }
                return@launch
            }
            processBalanceOutcome(
                fetchedBalance = paisa,
                dedicatedAccountId = dedicated.id,
                isTyped = true,
                previousBalance = dedicated.balance,
                forUpdateSheet = true,
            )
        }
    }

    fun openCreditEntryFromSheet() {
        val entryId = _uiState.value.createdCreditEntryId ?: return
        _uiState.update {
            it.copy(
                showSyncSheet = false,
                showUpdateBalanceSheet = false,
                navigateToCreditEntryId = entryId,
                isSyncing = false,
            )
        }
    }

    fun openCreditEntryFromBanner() {
        val entryId = _uiState.value.openCreditEntryId ?: return
        _uiState.update { it.copy(navigateToCreditEntryId = entryId) }
    }

    fun consumeCreditEntryNavigation() {
        _uiState.update { it.copy(navigateToCreditEntryId = null) }
    }

    fun continueToWithdrawalStub() {
        val shortfall = _uiState.value.withdrawalShortfallPaisa ?: return
        _uiState.update {
            it.copy(
                showSyncSheet = false,
                showUpdateBalanceSheet = false,
                isSyncing = false,
                withdrawalStubMessage = formattingService.formatInrFromPaisa(shortfall).let { amount ->
                    "Balance went down by $amount. Withdrawal flow lands in a separate ticket."
                },
                navigateToWithdrawalStub = true,
            )
        }
    }

    fun consumeWithdrawalNavigation() {
        _uiState.update { it.copy(navigateToWithdrawalStub = false) }
    }

    fun formattedSavedAmount(goal: Goal): String =
        formattingService.formatInrFromPaisa(goal.savedAmount)

    fun statusLabel(goal: Goal): String =
        goalsTabService.statusLabel(goal.status())

    private suspend fun processBalanceOutcome(
        fetchedBalance: Long,
        dedicatedAccountId: String,
        isTyped: Boolean,
        previousBalance: Long,
        forUpdateSheet: Boolean = false,
    ) {
        try {
            val state = repository.loadState()
            val outcome = creditEntryService.processFetchedBalance(
                state = state,
                fetchedBalance = fetchedBalance,
                dedicatedAccountId = dedicatedAccountId,
                isTyped = isTyped,
                id = UUID.randomUUID().toString(),
                createdAt = Instant.now().toString(),
            )
            val formattedFetched = formattingService.formatInrFromPaisa(fetchedBalance)
            val compare = creditEntryService.compare(previousBalance, fetchedBalance)
            val formattedNew = when (compare) {
                is BalanceCompareResult.Higher ->
                    formattingService.formatInrFromPaisa(compare.creditAmount)
                else -> null
            }

            when (outcome) {
                is CreditProcessOutcome.NoNewCredit -> {
                    if (forUpdateSheet) {
                        _uiState.update {
                            it.copy(
                                updateInfoMessage = outcome.message,
                                updateErrorMessage = null,
                                createdCreditEntryId = null,
                                withdrawalShortfallPaisa = null,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                syncPhase = SyncSheetPhase.ShowingResult,
                                syncInfoMessage = outcome.message,
                                formattedFetchedBalance = formattedFetched,
                                formattedNewCreditAmount = null,
                                createdCreditEntryId = null,
                                withdrawalShortfallPaisa = null,
                            )
                        }
                    }
                }
                is CreditProcessOutcome.WithdrawalRequired -> {
                    val message = "Balance went down by ${
                        formattingService.formatInrFromPaisa(outcome.shortfall)
                    }."
                    if (forUpdateSheet) {
                        _uiState.update {
                            it.copy(
                                updateInfoMessage = message,
                                updateErrorMessage = null,
                                withdrawalShortfallPaisa = outcome.shortfall,
                                createdCreditEntryId = null,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                syncPhase = SyncSheetPhase.ShowingResult,
                                syncInfoMessage = message,
                                formattedFetchedBalance = formattedFetched,
                                formattedNewCreditAmount = null,
                                withdrawalShortfallPaisa = outcome.shortfall,
                                createdCreditEntryId = null,
                            )
                        }
                    }
                }
                is CreditProcessOutcome.OpenCreditCreated -> {
                    repository.saveState(outcome.state)
                    apply(outcome.state)
                    if (forUpdateSheet) {
                        _uiState.update {
                            it.copy(
                                updateInfoMessage = null,
                                updateErrorMessage = null,
                                createdCreditEntryId = outcome.entry.id,
                                withdrawalShortfallPaisa = null,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                syncPhase = SyncSheetPhase.ShowingResult,
                                syncInfoMessage = null,
                                formattedFetchedBalance = formattedFetched,
                                formattedNewCreditAmount = formattedNew,
                                createdCreditEntryId = outcome.entry.id,
                                withdrawalShortfallPaisa = null,
                            )
                        }
                    }
                }
            }
        } catch (error: CreditEntryException) {
            if (forUpdateSheet) {
                _uiState.update { it.copy(updateErrorMessage = error.message) }
            } else {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncPhase = SyncSheetPhase.ShowingResult,
                        syncErrorMessage = error.message,
                    )
                }
            }
        } catch (error: Exception) {
            val message = error.message ?: error.toString()
            if (forUpdateSheet) {
                _uiState.update { it.copy(updateErrorMessage = message) }
            } else {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncPhase = SyncSheetPhase.ShowingResult,
                        syncErrorMessage = message,
                    )
                }
            }
        }
    }

    private fun apply(state: AppState) {
        val action = goalsTabService.balanceAction(state.accounts)
        val total = goalsTabService.totalSavingsPaisa(state.accounts, state.goals)
        val openEntry = creditEntryService.openCreditEntry(state.history)
        val dedicated = dedicatedAccountService.dedicatedAccount(state.accounts)
        _uiState.update {
            it.copy(
                accounts = state.accounts,
                goals = state.goals,
                history = state.history,
                formattedTotalSavings = formattingService.formatInrFromPaisa(total),
                totalSavingsPaisa = total,
                balanceAction = action,
                balanceActionTitle = goalsTabService.balanceActionTitle(action),
                dedicatedAccountSubtitle = goalsTabService.dedicatedAccountSubtitle(state.accounts),
                hasGoals = goalsTabService.hasGoals(state.goals),
                isLoading = false,
                openCreditEntryId = openEntry?.id,
                openEntryBannerMessage = openEntry?.let { entry ->
                    creditEntryService.openEntryBannerMessage(entry, formattingService)
                },
                isSyncOrUpdateBlocked = openEntry != null,
                formattedPreviousBalance = formattingService.formatInrFromPaisa(
                    dedicated?.balance ?: 0L,
                ),
            )
        }
    }
}

data class GoalsUiState(
    val accounts: List<Account> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
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
    val syncPhase: SyncSheetPhase = SyncSheetPhase.Idle,
    val syncInfoMessage: String? = null,
    val syncErrorMessage: String? = null,
    val updateInfoMessage: String? = null,
    val updateErrorMessage: String? = null,
    val formattedPreviousBalance: String = "₹0",
    val formattedFetchedBalance: String? = null,
    val formattedNewCreditAmount: String? = null,
    val createdCreditEntryId: String? = null,
    val openCreditEntryId: String? = null,
    val openEntryBannerMessage: String? = null,
    val isSyncOrUpdateBlocked: Boolean = false,
    val withdrawalShortfallPaisa: Long? = null,
    val withdrawalStubMessage: String? = null,
    val navigateToCreditEntryId: String? = null,
    val navigateToWithdrawalStub: Boolean = false,
    val navigateToSettings: Boolean = false,
    val selectedGoalId: String? = null,
) {
    val canContinueToCreditEntry: Boolean
        get() = createdCreditEntryId != null

    val canContinueToWithdrawal: Boolean
        get() = withdrawalShortfallPaisa != null
}
