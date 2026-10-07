package com.piplanner.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.DedicatedAccountService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.WithdrawalException
import com.piplanner.domain.WithdrawalService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Withdrawal flow (frames 18 / 18a / 18b / 18c) — PRD R15, Spec BR-8.
 *
 * States: Proportional default · Edit · Invalid total · Goal below zero · Complete.
 */
@HiltViewModel
class WithdrawalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val withdrawalService: WithdrawalService,
    private val dedicatedAccountService: DedicatedAccountService,
) : ViewModel() {

    private val previousBalance: Long = checkNotNull(
        savedStateHandle.get<Long>(NAV_ARG_PREVIOUS_BALANCE),
    ) { "previousBalance required" }

    private val newBalance: Long = checkNotNull(
        savedStateHandle.get<Long>(NAV_ARG_NEW_BALANCE),
    ) { "newBalance required" }

    private val isTyped: Boolean = savedStateHandle.get<Boolean>(NAV_ARG_IS_TYPED) ?: false

    private val shortfall: Long = (previousBalance - newBalance).coerceAtLeast(0L)

    private val _uiState = MutableStateFlow(WithdrawalUiState(isLoading = true))
    val uiState: StateFlow<WithdrawalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = repository.loadState()
            val goals = state.goals
            if (goals.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phase = WithdrawalPhase.Invalid,
                        errorMessage = "Add a goal before recording a withdrawal.",
                        canSave = false,
                    )
                }
                return@launch
            }
            if (shortfall <= 0L) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phase = WithdrawalPhase.Invalid,
                        errorMessage = "Withdrawal requires a lower balance.",
                        canSave = false,
                    )
                }
                return@launch
            }
            val defaults = withdrawalService.defaultReductions(goals, shortfall)
            publish(
                goals = goals,
                reductions = defaults,
                hasEdited = false,
                phase = WithdrawalPhase.ProportionalDefault,
            )
        }
    }

    /** Starts the single edit pass (frame 18 edit-once). */
    fun beginEdit() {
        val current = _uiState.value
        if (current.isLocked || current.hasEditedOnce || current.isEditing) return
        _uiState.update {
            it.copy(
                isEditing = true,
                phase = WithdrawalPhase.Edit,
                errorMessage = null,
            )
        }
    }

    /**
     * Ends the edit pass. Keeps editing open when total/goal validation fails
     * so the user can fix amounts before Save.
     */
    fun finishEdit() {
        val current = _uiState.value
        if (!current.isEditing) return
        val valid = withdrawalService.canSaveAndLock(
            goals = current.goals,
            reductions = current.reductionsPaisa,
            shortfall = shortfall,
        )
        if (!valid) {
            val belowZero = withdrawalService.goalBelowZeroIds(
                current.goals,
                current.reductionsPaisa,
            )
            _uiState.update {
                it.copy(
                    phase = if (belowZero.isNotEmpty()) {
                        WithdrawalPhase.GoalBelowZero
                    } else {
                        WithdrawalPhase.InvalidTotal
                    },
                    statusMessage = withdrawalService.statusMessage(
                        goals = current.goals,
                        reductions = current.reductionsPaisa,
                        shortfall = shortfall,
                        formatting = formattingService,
                    ) ?: it.statusMessage,
                    canSave = false,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                isEditing = false,
                hasEditedOnce = true,
                phase = WithdrawalPhase.Edit,
                canSave = true,
                statusMessage = WithdrawalService.READY_MESSAGE,
            )
        }
    }

    fun setReductionRupees(goalId: String, rupeeDigits: String) {
        val current = _uiState.value
        if (!current.canEditAmounts) return
        val digits = rupeeDigits.filter { it.isDigit() }
        val rupees = digits.toLongOrNull() ?: 0L
        val paisa = rupees * FormattingService.PAISA_PER_RUPEE
        val next = current.reductionsPaisa.toMutableMap().apply { put(goalId, paisa) }
        val nextDigits = current.reductionRupeeDigits.toMutableMap().apply {
            put(goalId, digits)
        }
        publish(
            goals = current.goals,
            reductions = next,
            hasEdited = true,
            phase = WithdrawalPhase.Edit,
            isEditing = true,
            hasEditedOnce = current.hasEditedOnce,
            reductionRupeeDigits = nextDigits,
        )
    }

    fun saveAndLock() {
        val current = _uiState.value
        if (!current.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, canSave = false) }
            try {
                val state = repository.loadState()
                val dedicated = dedicatedAccountService.dedicatedAccount(state.accounts)
                    ?: throw WithdrawalException("accounts", "Dedicated savings account not found.")
                val next = withdrawalService.applyWithdrawal(
                    state = state,
                    previousBalance = previousBalance,
                    newBalance = newBalance,
                    reductions = current.reductionsPaisa,
                    dedicatedAccountId = dedicated.id,
                    nowIso = Instant.now().toString(),
                    isTyped = isTyped,
                )
                repository.saveState(next)
                val entry = next.history.lastOrNull {
                    it.type == com.piplanner.data.model.HistoryEntryType.Withdrawal
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isLocked = true,
                        isEditing = false,
                        hasEditedOnce = true,
                        phase = WithdrawalPhase.Complete,
                        canSave = false,
                        statusMessage = "Withdrawal saved. Locked amounts never change.",
                        historyEntryId = entry?.id,
                        navigateBack = true,
                    )
                }
            } catch (error: WithdrawalException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message,
                        canSave = withdrawalService.canSaveAndLock(
                            goals = current.goals,
                            reductions = current.reductionsPaisa,
                            shortfall = shortfall,
                        ),
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: error.toString(),
                        canSave = withdrawalService.canSaveAndLock(
                            goals = current.goals,
                            reductions = current.reductionsPaisa,
                            shortfall = shortfall,
                        ),
                    )
                }
            }
        }
    }

    fun consumeNavigateBack() {
        _uiState.update { it.copy(navigateBack = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun formattedSaved(goal: Goal): String =
        formattingService.formatInrFromPaisa(goal.savedAmount)

    fun formattedReduction(goalId: String): String {
        val amount = _uiState.value.reductionsPaisa[goalId] ?: 0L
        return formattingService.formatInrFromPaisa(amount)
    }

    fun formattedRemaining(goal: Goal): String {
        val reduction = _uiState.value.reductionsPaisa[goal.id] ?: 0L
        val remaining = (goal.savedAmount - reduction).coerceAtLeast(0L)
        return formattingService.formatInrFromPaisa(remaining)
    }

    private fun publish(
        goals: List<Goal>,
        reductions: Map<String, Long>,
        hasEdited: Boolean,
        phase: WithdrawalPhase,
        isEditing: Boolean = false,
        hasEditedOnce: Boolean = false,
        reductionRupeeDigits: Map<String, String>? = null,
    ) {
        val belowZero = withdrawalService.goalBelowZeroIds(goals, reductions)
        val exact = withdrawalService.isTotalExact(reductions, shortfall)
        val resolvedPhase = when {
            belowZero.isNotEmpty() -> WithdrawalPhase.GoalBelowZero
            !exact -> WithdrawalPhase.InvalidTotal
            hasEdited -> phase
            else -> WithdrawalPhase.ProportionalDefault
        }
        val status = withdrawalService.statusMessage(
            goals = goals,
            reductions = reductions,
            shortfall = shortfall,
            formatting = formattingService,
        ) ?: WithdrawalService.READY_MESSAGE

        // Proportional defaults are valid immediately; edit-once is optional before Save.
        val canSave = withdrawalService.canSaveAndLock(goals, reductions, shortfall) &&
            !_uiState.value.isSaving

        val digits = reductionRupeeDigits ?: goals.associate { goal ->
            val paisa = reductions[goal.id] ?: 0L
            goal.id to (paisa / FormattingService.PAISA_PER_RUPEE).toString()
        }

        _uiState.value = WithdrawalUiState(
            goals = goals,
            reductionsPaisa = reductions,
            reductionRupeeDigits = digits,
            shortfallPaisa = shortfall,
            previousBalancePaisa = previousBalance,
            newBalancePaisa = newBalance,
            formattedShortfall = formattingService.formatInrFromPaisa(shortfall),
            formattedPrevious = formattingService.formatInrFromPaisa(previousBalance),
            formattedNewBalance = formattingService.formatInrFromPaisa(newBalance),
            formattedTotalReductions = formattingService.formatInrFromPaisa(
                withdrawalService.totalReductions(reductions),
            ),
            statusMessage = status,
            caption = if (resolvedPhase == WithdrawalPhase.ProportionalDefault) {
                WithdrawalService.PROPORTIONAL_CAPTION
            } else {
                WithdrawalService.EDIT_ONCE_CAPTION
            },
            phase = resolvedPhase,
            canSave = canSave,
            isLoading = false,
            isSaving = false,
            isEditing = isEditing,
            hasEditedOnce = hasEditedOnce,
            isTyped = isTyped,
        )
    }

    companion object {
        const val NAV_ARG_PREVIOUS_BALANCE: String = "previousBalance"
        const val NAV_ARG_NEW_BALANCE: String = "newBalance"
        const val NAV_ARG_IS_TYPED: String = "isTyped"
    }
}

enum class WithdrawalPhase {
    ProportionalDefault,
    Edit,
    InvalidTotal,
    GoalBelowZero,
    Complete,
    Invalid,
}

data class WithdrawalUiState(
    val goals: List<Goal> = emptyList(),
    val reductionsPaisa: Map<String, Long> = emptyMap(),
    val reductionRupeeDigits: Map<String, String> = emptyMap(),
    val shortfallPaisa: Long = 0L,
    val previousBalancePaisa: Long = 0L,
    val newBalancePaisa: Long = 0L,
    val formattedShortfall: String = "₹0",
    val formattedPrevious: String = "₹0",
    val formattedNewBalance: String = "₹0",
    val formattedTotalReductions: String = "₹0",
    val statusMessage: String = "",
    val caption: String = WithdrawalService.PROPORTIONAL_CAPTION,
    val phase: WithdrawalPhase = WithdrawalPhase.ProportionalDefault,
    val canSave: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val hasEditedOnce: Boolean = false,
    val isLocked: Boolean = false,
    val isTyped: Boolean = false,
    val errorMessage: String? = null,
    val historyEntryId: String? = null,
    val navigateBack: Boolean = false,
) {
    val canStartEdit: Boolean
        get() = !isLocked && !isEditing && !hasEditedOnce && !isLoading

    val canEditAmounts: Boolean
        get() = !isLocked && isEditing && !isSaving
}
