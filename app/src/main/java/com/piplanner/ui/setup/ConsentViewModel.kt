package com.piplanner.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Account
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.BalanceSyncService
import com.piplanner.domain.ConsentService
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.PinError
import com.piplanner.domain.PinException
import com.piplanner.util.DemoData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Outcome of a UPI PIN check (frames 4b / 4d / 4e). */
sealed class UpiPinCheckOutcome {
    data class Success(val paisa: Long) : UpiPinCheckOutcome()
    data object WrongPin : UpiPinCheckOutcome()
    data object Cancelled : UpiPinCheckOutcome()
    data object OtherApp : UpiPinCheckOutcome()
}

/**
 * View model for Consent + balance entry (frames 3–4e) — PRD R3 / R4; Spec §3.3.
 */
@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val balanceSync: BalanceSyncService,
    private val dedicatedAccountService: DedicatedAccountService,
    private val formattingService: FormattingService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    /**
     * When true (setup): Yes fetches mock opening balance and persists it.
     * When false (Settings Off→On / PIP-62): Yes persists `consentAutoUpdate` only —
     * leave dedicated balance for Goals Sync (iOS PIP-61 parity).
     */
    private var fetchesBalanceOnYes: Boolean = true

    /** Seeds accounts from Accounts screen before Consent actions. */
    fun configure(accounts: List<Account>, fetchesBalanceOnYes: Boolean = true) {
        this.fetchesBalanceOnYes = fetchesBalanceOnYes
        _uiState.update {
            it.copy(
                accounts = accounts,
                errorMessage = null,
            )
        }
    }

    /**
     * Loads persisted accounts (dedicated selection from Accounts Continue).
     * Falls back to demo accounts with HDFC dedicated when none are persisted.
     *
     * @param fetchesBalanceOnYes setup default true; Settings re-consent passes false.
     */
    fun loadAccounts(fetchesBalanceOnYes: Boolean = true) {
        viewModelScope.launch {
            val persisted = repository.loadState().accounts
            val accounts = when {
                persisted.any { it.isDedicated } -> persisted
                else -> DemoData.sampleAccounts().map { account ->
                    account.copy(isDedicated = account.id == DemoData.DEMO_SAVINGS_ACCOUNT_ID)
                }
            }
            configure(accounts = accounts, fetchesBalanceOnYes = fetchesBalanceOnYes)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeNavigation() {
        _uiState.update {
            it.copy(
                shouldShowFetchedBalance = false,
                shouldShowUpdateBalance = false,
                shouldShowWrongPin = false,
                shouldShowOtherApp = false,
                shouldContinueAfterBalance = false,
            )
        }
    }

    // MARK: - Consent (3)

    /**
     * Yes path.
     * Setup (`fetchesBalanceOnYes`): fetch demo balance (3a), persist balance + consent On.
     * Settings Off→On: persist `consentAutoUpdate = true` only — do not overwrite balance.
     */
    fun chooseConsentYes() {
        val dedicated = dedicatedAccountService.dedicatedAccount(_uiState.value.accounts)
        if (dedicated == null) {
            _uiState.update {
                it.copy(errorMessage = "Select a dedicated savings account first.")
            }
            return
        }
        if (_uiState.value.isWorking) return

        _uiState.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            if (!fetchesBalanceOnYes) {
                // Settings re-consent (20b) — iOS PIP-61: confirmConsentOn / fetchesBalanceOnYes=false.
                persistConsentFlag(autoUpdate = true)
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        consentAutoUpdate = true,
                        resolvedBalancePaisa = null,
                        shouldShowFetchedBalance = true,
                    )
                }
                return@launch
            }

            val result = balanceSync.fetchBalance(dedicated.id)
            result.fold(
                onSuccess = { paisa ->
                    persistConsentAndBalance(paisa = paisa, autoUpdate = true)
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            consentAutoUpdate = true,
                            resolvedBalancePaisa = paisa,
                            shouldShowFetchedBalance = true,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            errorMessage = "Couldn’t fetch balance. Try again.",
                        )
                    }
                },
            )
        }
    }

    /** No path — decline auto-update; show Update balance sheet (4). */
    fun chooseConsentNo() {
        _uiState.update {
            it.copy(
                consentAutoUpdate = false,
                errorMessage = null,
                resolvedBalancePaisa = null,
                shouldShowUpdateBalance = true,
            )
        }
        viewModelScope.launch {
            persistConsentFlag(autoUpdate = false)
        }
    }

    // MARK: - Manual (4a)

    fun setManualRupeeDigits(digits: String) {
        _uiState.update {
            it.copy(manualRupeeDigits = digits.filter { ch -> ch.isDigit() })
        }
    }

    /** Confirm typed amount → resolved balance for next setup step. */
    fun continueManual() {
        val state = _uiState.value
        if (!state.canContinueManual || state.isWorking) return
        val paisa = state.manualAmountPaisa
        _uiState.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            persistConsentAndBalance(paisa = paisa, autoUpdate = false)
            _uiState.update {
                it.copy(
                    isWorking = false,
                    consentAutoUpdate = false,
                    resolvedBalancePaisa = paisa,
                    shouldContinueAfterBalance = true,
                )
            }
        }
    }

    // MARK: - UPI PIN (4b / 4d / 4e)

    fun appendPinDigit(digit: String) {
        if (digit.length != 1 || !digit[0].isDigit()) return
        _uiState.update { current ->
            if (current.pinDigits.length >= 4) current
            else {
                current.copy(
                    pinDigits = current.pinDigits + digit,
                    lastPinOutcome = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun deletePinDigit() {
        _uiState.update { current ->
            if (current.pinDigits.isEmpty()) current
            else {
                current.copy(
                    pinDigits = current.pinDigits.dropLast(1),
                    lastPinOutcome = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun clearPin() {
        _uiState.update {
            it.copy(
                pinDigits = "",
                lastPinOutcome = null,
                errorMessage = null,
            )
        }
    }

    /** Check balance with demo PIN. Success → fetched; wrong → retry/manual (4d/4e). */
    fun checkBalanceWithPin() {
        val state = _uiState.value
        if (!state.canCheckPin || state.isWorking) return
        _uiState.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            val result = balanceSync.verifyUpiPin(state.pinDigits)
            result.fold(
                onSuccess = { paisa ->
                    persistConsentAndBalance(paisa = paisa, autoUpdate = false)
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            consentAutoUpdate = false,
                            resolvedBalancePaisa = paisa,
                            lastPinOutcome = UpiPinCheckOutcome.Success(paisa),
                            shouldShowFetchedBalance = true,
                        )
                    }
                },
                onFailure = { error ->
                    val pinError = (error as? PinException)?.error ?: PinError.WrongPin
                    when (pinError) {
                        PinError.WrongPin -> {
                            _uiState.update {
                                it.copy(
                                    isWorking = false,
                                    lastPinOutcome = UpiPinCheckOutcome.WrongPin,
                                    errorMessage =
                                        "Incorrect PIN. Try again or enter the balance manually.",
                                    shouldShowWrongPin = true,
                                )
                            }
                        }
                        PinError.Cancelled -> {
                            _uiState.update {
                                it.copy(
                                    isWorking = false,
                                    lastPinOutcome = UpiPinCheckOutcome.Cancelled,
                                )
                            }
                        }
                        PinError.OtherApp -> {
                            _uiState.update {
                                it.copy(
                                    isWorking = false,
                                    lastPinOutcome = UpiPinCheckOutcome.OtherApp,
                                    shouldShowOtherApp = true,
                                )
                            }
                        }
                    }
                },
            )
        }
    }

    fun cancelPin() {
        clearPin()
        _uiState.update { it.copy(lastPinOutcome = UpiPinCheckOutcome.Cancelled) }
    }

    /** Frame 4c — force manual entry path. */
    fun accountOnOtherUpiApp() {
        clearPin()
        _uiState.update {
            it.copy(
                lastPinOutcome = UpiPinCheckOutcome.OtherApp,
                shouldShowOtherApp = true,
            )
        }
    }

    fun retryPin() {
        clearPin()
    }

    fun formattedBalance(paisa: Long): String = formattingService.formatInrFromPaisa(paisa)

    private suspend fun persistConsentFlag(autoUpdate: Boolean) {
        try {
            val persisted = repository.loadState()
            val updated = applyingConsent(autoUpdate)
            _uiState.update { it.copy(accounts = updated) }
            repository.saveState(persisted.copy(accounts = updated))
        } catch (error: Exception) {
            _uiState.update {
                it.copy(errorMessage = error.message ?: "Couldn’t save consent")
            }
        }
    }

    private suspend fun persistConsentAndBalance(paisa: Long, autoUpdate: Boolean) {
        try {
            val persisted = repository.loadState()
            val updated = _uiState.value.accounts.map { account ->
                if (account.isDedicated) {
                    account.copy(balance = paisa, consentAutoUpdate = autoUpdate)
                } else {
                    account
                }
            }
            _uiState.update { it.copy(accounts = updated) }
            repository.saveState(persisted.copy(accounts = updated))
        } catch (error: Exception) {
            _uiState.update {
                it.copy(errorMessage = error.message ?: "Couldn’t save balance")
            }
        }
    }

    private fun applyingConsent(autoUpdate: Boolean): List<Account> =
        _uiState.value.accounts.map { account ->
            if (account.isDedicated) account.copy(consentAutoUpdate = autoUpdate) else account
        }
}

data class ConsentUiState(
    val accounts: List<Account> = emptyList(),
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val resolvedBalancePaisa: Long? = null,
    val consentAutoUpdate: Boolean = false,
    val pinDigits: String = "",
    val lastPinOutcome: UpiPinCheckOutcome? = null,
    val manualRupeeDigits: String = "",
    val shouldShowFetchedBalance: Boolean = false,
    val shouldShowUpdateBalance: Boolean = false,
    val shouldShowWrongPin: Boolean = false,
    val shouldShowOtherApp: Boolean = false,
    val shouldContinueAfterBalance: Boolean = false,
) {
    val dedicatedAccount: Account?
        get() = accounts.singleOrNull { it.isDedicated }

    val dedicatedAccountTitle: String?
        get() = dedicatedAccount?.let { "${it.bankName} ${it.maskedNumber}" }

    val manualAmountPaisa: Long
        get() = ConsentService.paisaFromRupeeDigits(manualRupeeDigits)

    val canContinueManual: Boolean
        get() = !isWorking && ConsentService.canContinueManual(manualAmountPaisa)

    val canCheckPin: Boolean
        get() = !isWorking && ConsentService.isCompletePin(pinDigits)
}
