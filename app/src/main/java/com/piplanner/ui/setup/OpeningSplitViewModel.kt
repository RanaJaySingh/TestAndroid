package com.piplanner.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.FormattingService
import com.piplanner.domain.HistoryService
import com.piplanner.domain.OpeningSplitException
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
import java.util.UUID
import javax.inject.Inject

/**
 * View model for Opening split (frames 8 / 8b) — PRD R6, R22; Spec BR-2, BR-3.
 */
@HiltViewModel
class OpeningSplitViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val openingSplitService: OpeningSplitService,
) : ViewModel() {

    private val clock: () -> Instant = { Instant.now() }
    private val makeId: () -> String = { UUID.randomUUID().toString() }

    private var goals: List<Goal> = emptyList()
    private var openingBalance: Long = 0L

    private val _uiState = MutableStateFlow(OpeningSplitUiState())
    val uiState: StateFlow<OpeningSplitUiState> = _uiState.asStateFlow()

    /**
     * Configures the screen with goals / balance (and optional locked entry for read-only).
     * Call once when the destination is shown; safe to call again with the same inputs.
     */
    fun configure(
        goals: List<Goal>,
        openingBalance: Long,
        initialPercents: Map<String, Int>? = null,
        lockedEntry: HistoryEntry? = null,
    ) {
        this.goals = goals
        this.openingBalance = openingBalance

        val displayPercents = when {
            goals.size == 1 -> mapOf(goals.first().id to 100)
            lockedEntry != null -> lockedEntry.allocations.associate {
                it.goalId to (it.percentage * 100.0).toInt()
            }
            initialPercents != null -> goals.associate { goal ->
                goal.id to (initialPercents[goal.id] ?: 0)
            }
            else -> {
                val fromShares = goals.associate { goal ->
                    goal.id to (goal.shareOfNewCredits * 100.0).toInt()
                }
                if (fromShares.values.sum() == 100) fromShares else equalDisplayPercents(goals)
            }
        }

        _uiState.value = OpeningSplitUiState(
            goals = goals,
            openingBalance = openingBalance,
            formattedOpeningBalance = formattingService.formatInrFromPaisa(openingBalance),
            displayPercents = displayPercents,
            lockedEntry = lockedEntry,
            amountsByGoalId = computeAmounts(goals, openingBalance, displayPercents, lockedEntry),
            statusMessage = buildStatusMessage(
                goals = goals,
                displayPercents = displayPercents,
                lockedEntry = lockedEntry,
            ),
            canLock = canLock(
                goals = goals,
                displayPercents = displayPercents,
                lockedEntry = lockedEntry,
                isLocking = false,
            ),
        )
    }

    /** Factory for viewing a locked opening entry later (History) — always read-only. */
    fun configureReadOnly(entry: HistoryEntry, goals: List<Goal>) {
        val balance = entry.creditAmount ?: entry.newBalance ?: 0L
        val resolvedGoals = if (goals.isEmpty()) {
            entry.allocations.map { allocation ->
                Goal(
                    id = allocation.goalId,
                    name = allocation.goalName,
                    targetAmount = 0L,
                    startDate = entry.createdAt.take(10),
                    endDate = entry.createdAt.take(10),
                    savedAmount = allocation.amount,
                    shareOfNewCredits = allocation.percentage,
                    createdAt = entry.createdAt,
                    updatedAt = entry.createdAt,
                )
            }
        } else {
            goals
        }
        val percents = entry.allocations.associate {
            it.goalId to (it.percentage * 100.0).toInt()
        }
        configure(
            goals = resolvedGoals,
            openingBalance = balance,
            initialPercents = percents,
            lockedEntry = entry,
        )
        // History frame 12a uses BR-3 copy (distinct from setup lock caption).
        _uiState.update {
            it.copy(statusMessage = HistoryService.ORIGINAL_AMOUNTS_CAPTION)
        }
    }

    /** Loads a locked Opening balance entry from History for read-only viewing. */
    fun loadLockedOpeningFromHistory(entryId: String) {
        viewModelScope.launch {
            val state = repository.loadState()
            val entry = state.history.firstOrNull { it.id == entryId } ?: return@launch
            configureReadOnly(entry, state.goals)
        }
    }

    fun setDisplayPercent(goalId: String, percent: Int) {
        val state = _uiState.value
        if (state.isReadOnly || state.isSingleGoal) return
        val clamped = percent.coerceIn(0, 100)
        val nextPercents = state.displayPercents.toMutableMap().apply { put(goalId, clamped) }
        publishEditable(nextPercents, clearError = true)
    }

    fun requestLock() {
        if (!_uiState.value.canLock) return
        _uiState.update { it.copy(showConfirmLock = true) }
    }

    fun dismissConfirmLock() {
        _uiState.update { it.copy(showConfirmLock = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(shouldNavigateToGoals = false) }
    }

    /** Confirms lock: writes Opening balance History entry and signals navigation to Goals (9). */
    fun confirmLock() {
        val state = _uiState.value
        if (!state.canLock) return
        _uiState.update {
            it.copy(
                isLocking = true,
                showConfirmLock = false,
                errorMessage = null,
                canLock = false,
            )
        }

        viewModelScope.launch {
            try {
                val fractions = fractionMap(goals, _uiState.value.displayPercents)
                val now = clock()
                val entry = openingSplitService.createLockedOpeningEntry(
                    goals = goals,
                    openingBalance = openingBalance,
                    percentages = fractions,
                    id = makeId(),
                    createdAt = now.toString(),
                )
                var persisted = repository.loadState()
                if (persisted.goals.isEmpty()) {
                    persisted = persisted.copy(goals = goals)
                }
                val next = openingSplitService.applyOpeningLock(
                    state = persisted,
                    entry = entry,
                    nowIso = now.toString(),
                )
                repository.saveState(next)
                _uiState.update {
                    it.copy(
                        isLocking = false,
                        lockedEntry = entry,
                        amountsByGoalId = entry.allocations.associate { a -> a.goalId to a.amount },
                        statusMessage = OpeningSplitService.LOCKED_AMOUNTS_CAPTION,
                        canLock = false,
                        shouldNavigateToGoals = true,
                    )
                }
            } catch (error: OpeningSplitException) {
                _uiState.update {
                    it.copy(
                        isLocking = false,
                        errorMessage = error.message,
                        canLock = canLock(
                            goals = goals,
                            displayPercents = it.displayPercents,
                            lockedEntry = it.lockedEntry,
                            isLocking = false,
                        ),
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLocking = false,
                        errorMessage = error.message ?: "Couldn’t lock split",
                        canLock = canLock(
                            goals = goals,
                            displayPercents = it.displayPercents,
                            lockedEntry = it.lockedEntry,
                            isLocking = false,
                        ),
                    )
                }
            }
        }
    }

    fun formattedAmount(goalId: String): String {
        val paisa = _uiState.value.amountsByGoalId[goalId] ?: 0L
        return formattingService.formatInrFromPaisa(paisa)
    }

    private fun publishEditable(displayPercents: Map<String, Int>, clearError: Boolean) {
        _uiState.update { current ->
            current.copy(
                displayPercents = displayPercents,
                amountsByGoalId = computeAmounts(
                    goals = goals,
                    openingBalance = openingBalance,
                    displayPercents = displayPercents,
                    lockedEntry = current.lockedEntry,
                ),
                statusMessage = buildStatusMessage(
                    goals = goals,
                    displayPercents = displayPercents,
                    lockedEntry = current.lockedEntry,
                ),
                canLock = canLock(
                    goals = goals,
                    displayPercents = displayPercents,
                    lockedEntry = current.lockedEntry,
                    isLocking = current.isLocking,
                ),
                errorMessage = if (clearError) null else current.errorMessage,
            )
        }
    }

    private fun fractionMap(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
    ): Map<String, BigDecimal> {
        if (goals.size == 1) {
            return openingSplitService.singleGoalPercentages(goals.first().id)
        }
        return goals.associate { goal ->
            val percent = displayPercents[goal.id] ?: 0
            goal.id to BigDecimal.valueOf(percent.toLong())
                .divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
        }
    }

    private fun orderedFractions(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
    ): List<BigDecimal> {
        val map = fractionMap(goals, displayPercents)
        return goals.map { map[it.id] ?: BigDecimal.ZERO }
    }

    private fun computeAmounts(
        goals: List<Goal>,
        openingBalance: Long,
        displayPercents: Map<String, Int>,
        lockedEntry: HistoryEntry?,
    ): Map<String, Long> {
        if (lockedEntry != null) {
            return lockedEntry.allocations.associate { it.goalId to it.amount }
        }
        val amounts = openingSplitService.allocatePaisa(
            total = openingBalance,
            fractions = orderedFractions(goals, displayPercents),
        )
        return goals.mapIndexed { index, goal -> goal.id to amounts[index] }.toMap()
    }

    private fun buildStatusMessage(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
        lockedEntry: HistoryEntry?,
    ): String {
        if (lockedEntry != null) {
            return OpeningSplitService.LOCKED_AMOUNTS_CAPTION
        }
        if (goals.size == 1) {
            return "100% assigned to ${goals.first().name}."
        }
        return openingSplitService.shortfallMessage(orderedFractions(goals, displayPercents))
            ?: "Total 100%. Ready to lock."
    }

    private fun canLock(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
        lockedEntry: HistoryEntry?,
        isLocking: Boolean,
    ): Boolean {
        if (lockedEntry != null || isLocking || goals.isEmpty()) return false
        return openingSplitService.isValidHundredPercent(
            orderedFractions(goals, displayPercents),
        )
    }

    private fun equalDisplayPercents(goals: List<Goal>): Map<String, Int> {
        if (goals.isEmpty()) return emptyMap()
        val base = 100 / goals.size
        var remainder = 100 - (base * goals.size)
        return goals.associate { goal ->
            val extra = if (remainder > 0) 1 else 0
            if (remainder > 0) remainder -= 1
            goal.id to (base + extra)
        }
    }
}

data class OpeningSplitUiState(
    val goals: List<Goal> = emptyList(),
    val openingBalance: Long = 0L,
    val formattedOpeningBalance: String = "₹0",
    val displayPercents: Map<String, Int> = emptyMap(),
    val amountsByGoalId: Map<String, Long> = emptyMap(),
    val lockedEntry: HistoryEntry? = null,
    val isLocking: Boolean = false,
    val showConfirmLock: Boolean = false,
    val errorMessage: String? = null,
    val shouldNavigateToGoals: Boolean = false,
    val statusMessage: String = "",
    val canLock: Boolean = false,
) {
    val isSingleGoal: Boolean get() = goals.size == 1
    val isReadOnly: Boolean get() = lockedEntry != null
}
