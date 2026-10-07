package com.piplanner.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.CreditEntryException
import com.piplanner.domain.CreditEntryService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalHeldChangeService
import com.piplanner.domain.OpeningSplitService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import javax.inject.Inject

/**
 * View model for open / locked New credit History entry (frames 13 / 13a–13g / 13t).
 */
@HiltViewModel
class CreditEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val creditEntryService: CreditEntryService,
    private val openingSplitService: OpeningSplitService,
    private val goalHeldChangeService: GoalHeldChangeService,
) : ViewModel() {

    private val entryId: String = checkNotNull(savedStateHandle[NAV_ARG_ENTRY_ID]) {
        "credit entry id required"
    }

    private val _uiState = MutableStateFlow(CreditEntryUiState(isLoading = true))
    val uiState: StateFlow<CreditEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = repository.loadState()
            val entry = state.history.firstOrNull { it.id == entryId }
            if (entry == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Credit entry not found.",
                    )
                }
                return@launch
            }
            applyEntry(entry, state.goals)
        }
    }

    fun setDisplayPercent(goalId: String, percent: Int) {
        val current = _uiState.value
        val entry = current.entry ?: return
        if (current.isLocked || current.isSingleGoal) return
        val clamped = percent.coerceIn(0, 100)
        val nextPercents = current.displayPercents + (goalId to clamped)
        rebuildEditable(entry, current.goals, nextPercents, current.useThisSplitForStanding)
    }

    fun setUseThisSplitForStanding(checked: Boolean) {
        _uiState.update { it.copy(useThisSplitForStanding = checked) }
    }

    fun saveAndLock() {
        val current = _uiState.value
        if (!current.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                var state = repository.loadState()
                state = creditEntryService.applyCreditLock(
                    state = state,
                    entryId = entryId,
                    percentages = fractionMap(current.goals, current.displayPercents, current.isSingleGoal),
                    useThisSplitForStanding = current.useThisSplitForStanding,
                    nowIso = Instant.now().toString(),
                )
                state = goalHeldChangeService.clearHeldChanges(state)
                repository.saveState(state)
                val locked = state.history.first { it.id == entryId }
                applyEntry(locked, state.goals)
                _uiState.update { it.copy(isSaving = false, navigateBack = true) }
            } catch (error: CreditEntryException) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = error.message)
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: error.toString(),
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

    fun formattedSavedSoFar(goal: Goal): String =
        formattingService.formatInrFromPaisa(goal.savedAmount)

    fun formattedAmount(goalId: String): String {
        val amount = _uiState.value.amountsByGoalId[goalId] ?: 0L
        return formattingService.formatInrFromPaisa(amount)
    }

    private fun applyEntry(entry: HistoryEntry, goals: List<Goal>) {
        val displayPercents = when {
            goals.size == 1 -> mapOf(goals.first().id to 100)
            else -> {
                val fromAllocations = entry.allocations.associate {
                    it.goalId to (it.percentage * 100.0).toInt()
                }.toMutableMap()
                goals.forEach { goal ->
                    if (goal.id !in fromAllocations) fromAllocations[goal.id] = 0
                }
                fromAllocations
            }
        }
        rebuildEditable(
            entry = entry,
            goals = goals,
            displayPercents = displayPercents,
            useThisSplit = false,
        )
    }

    private fun rebuildEditable(
        entry: HistoryEntry,
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
        useThisSplit: Boolean,
    ) {
        val creditAmount = entry.creditAmount ?: 0L
        val fractions = orderedFractions(goals, displayPercents, goals.size == 1)
        val amounts = if (entry.isLocked) {
            entry.allocations.associate { it.goalId to it.amount }
        } else {
            val allocated = openingSplitService.allocatePaisa(creditAmount, fractions)
            goals.mapIndexed { index, goal -> goal.id to allocated[index] }.toMap()
        }
        val canSave = creditEntryService.canSaveAndLock(entry, fractions) &&
            !_uiState.value.isSaving

        _uiState.value = CreditEntryUiState(
            entry = entry,
            goals = goals,
            displayPercents = displayPercents,
            amountsByGoalId = amounts,
            useThisSplitForStanding = useThisSplit,
            formattedCreditAmount = formattingService.formatInrFromPaisa(creditAmount),
            formattedPrevious = entry.previousBalance?.let(formattingService::formatInrFromPaisa),
            formattedNewBalance = entry.newBalance?.let(formattingService::formatInrFromPaisa),
            statusMessage = statusMessage(entry, goals, fractions),
            canSave = canSave,
            isLoading = false,
            isSaving = false,
        )
    }

    private fun statusMessage(
        entry: HistoryEntry,
        goals: List<Goal>,
        fractions: List<BigDecimal>,
    ): String {
        if (entry.isLocked) return OpeningSplitService.LOCKED_AMOUNTS_CAPTION
        if (goals.size == 1) {
            return "100% assigned to ${goals.first().name}."
        }
        return openingSplitService.shortfallMessage(fractions)
            ?: "Total 100%. Ready to save and lock."
    }

    private fun orderedFractions(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
        isSingleGoal: Boolean,
    ): List<BigDecimal> {
        val map = fractionMap(goals, displayPercents, isSingleGoal)
        return goals.map { map[it.id] ?: BigDecimal.ZERO }
    }

    private fun fractionMap(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
        isSingleGoal: Boolean,
    ): Map<String, BigDecimal> {
        if (isSingleGoal && goals.isNotEmpty()) {
            return openingSplitService.singleGoalPercentages(goals.first().id)
        }
        return goals.associate { goal ->
            val percent = displayPercents[goal.id] ?: 0
            goal.id to BigDecimal.valueOf(percent.toLong())
                .divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        }
    }

    companion object {
        const val NAV_ARG_ENTRY_ID: String = "entryId"
    }
}

data class CreditEntryUiState(
    val entry: HistoryEntry? = null,
    val goals: List<Goal> = emptyList(),
    val displayPercents: Map<String, Int> = emptyMap(),
    val amountsByGoalId: Map<String, Long> = emptyMap(),
    val useThisSplitForStanding: Boolean = false,
    val formattedCreditAmount: String = "₹0",
    val formattedPrevious: String? = null,
    val formattedNewBalance: String? = null,
    val statusMessage: String = "",
    val canSave: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val navigateBack: Boolean = false,
) {
    val isLocked: Boolean get() = entry?.isLocked == true
    val isTyped: Boolean get() = entry?.isTyped == true
    val isSingleGoal: Boolean get() = goals.size == 1
}
