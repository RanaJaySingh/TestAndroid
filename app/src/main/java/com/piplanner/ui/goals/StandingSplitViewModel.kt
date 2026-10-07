package com.piplanner.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.StandingSplitException
import com.piplanner.domain.StandingSplitService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * View model for Standing split (frame 15) — PRD R12, R22, R24; Spec BR-2.
 *
 * States: Multi-goal edit · One goal skip · Valid · Invalid.
 */
@HiltViewModel
class StandingSplitViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val standingSplitService: StandingSplitService,
) : ViewModel() {

    private val clock: () -> Instant = { Instant.now() }

    private val _uiState = MutableStateFlow(StandingSplitUiState())
    val uiState: StateFlow<StandingSplitUiState> = _uiState.asStateFlow()

    /**
     * Loads goals from persistence and configures the editor.
     * With one goal, applies 100% automatically and signals skip (no % UI).
     */
    fun load() {
        viewModelScope.launch {
            val state = repository.loadState()
            configure(
                goals = state.goals,
                standingDisplaySeed = standingSplitService.defaultDisplayPercents(
                    goals = state.goals,
                    standingSplits = state.standingSplits,
                ),
            )
        }
    }

    /**
     * Configures from explicit goals (tests / callers that already have state).
     * Triggers one-goal skip when [goals].size ≤ 1.
     */
    fun configure(
        goals: List<Goal>,
        standingDisplaySeed: Map<String, Int>? = null,
    ) {
        if (standingSplitService.shouldSkipUi(goals)) {
            if (goals.size == 1) {
                applySingleGoalSkip(goals)
            } else {
                _uiState.value = StandingSplitUiState(
                    goals = emptyList(),
                    statusMessage = "Add a goal before setting a standing split.",
                    shouldSkip = true,
                    canSave = false,
                )
            }
            return
        }

        val displayPercents = standingDisplaySeed
            ?: standingSplitService.defaultDisplayPercents(goals, emptyList())
        publishEditable(goals, displayPercents, clearError = true)
    }

    fun setDisplayPercent(goalId: String, percent: Int) {
        val state = _uiState.value
        if (state.shouldSkip || state.isSingleGoal || state.isSaving) return
        val clamped = percent.coerceIn(0, 100)
        val nextPercents = state.displayPercents.toMutableMap().apply { put(goalId, clamped) }
        publishEditable(state.goals, nextPercents, clearError = true)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeNavigation() {
        _uiState.update {
            it.copy(
                shouldNavigateBack = false,
                shouldSkip = false,
            )
        }
    }

    /** Saves standing split when total is 100%; next credit will use these percentages. */
    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return
        _uiState.update {
            it.copy(isSaving = true, errorMessage = null, canSave = false)
        }

        viewModelScope.launch {
            try {
                val fractions = standingSplitService.displayPercentsToFractions(
                    goals = state.goals,
                    displayPercents = state.displayPercents,
                )
                val persisted = repository.loadState()
                val next = standingSplitService.applyStandingSplit(
                    state = persisted,
                    percentages = fractions,
                    nowIso = clock().toString(),
                )
                repository.saveState(next)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        canSave = false,
                        shouldNavigateBack = true,
                        statusMessage = StandingSplitService.READY_MESSAGE,
                    )
                }
            } catch (error: StandingSplitException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message,
                        canSave = standingSplitService.isValidHundredPercent(
                            orderedFractions(it.goals, it.displayPercents),
                        ),
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Couldn’t save standing split",
                        canSave = standingSplitService.isValidHundredPercent(
                            orderedFractions(it.goals, it.displayPercents),
                        ),
                    )
                }
            }
        }
    }

    private fun applySingleGoalSkip(goals: List<Goal>) {
        _uiState.value = StandingSplitUiState(
            goals = goals,
            displayPercents = mapOf(goals.first().id to 100),
            statusMessage = "100% assigned to ${goals.first().name}.",
            shouldSkip = true,
            canSave = false,
            caption = StandingSplitService.SAVED_MONEY_STAYS_PUT,
        )
        viewModelScope.launch {
            try {
                val persisted = repository.loadState()
                val withGoal = if (persisted.goals.isEmpty()) {
                    persisted.copy(goals = goals)
                } else {
                    persisted
                }
                val next = standingSplitService.applySingleGoalSkip(
                    state = withGoal,
                    nowIso = clock().toString(),
                )
                repository.saveState(next)
                _uiState.update { it.copy(shouldNavigateBack = true) }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Couldn’t apply standing split",
                        shouldSkip = true,
                    )
                }
            }
        }
    }

    private fun publishEditable(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
        clearError: Boolean,
    ) {
        val fractions = orderedFractions(goals, displayPercents)
        val valid = standingSplitService.isValidHundredPercent(fractions)
        _uiState.value = StandingSplitUiState(
            goals = goals,
            displayPercents = displayPercents,
            caption = StandingSplitService.SAVED_MONEY_STAYS_PUT,
            statusMessage = standingSplitService.shortfallMessage(fractions)
                ?: StandingSplitService.READY_MESSAGE,
            canSave = valid && !goals.isEmpty(),
            isSaving = false,
            shouldSkip = false,
            errorMessage = if (clearError) null else _uiState.value.errorMessage,
        )
    }

    private fun orderedFractions(
        goals: List<Goal>,
        displayPercents: Map<String, Int>,
    ): List<java.math.BigDecimal> {
        val map = standingSplitService.displayPercentsToFractions(goals, displayPercents)
        return goals.map { map[it.id] ?: java.math.BigDecimal.ZERO }
    }
}

data class StandingSplitUiState(
    val goals: List<Goal> = emptyList(),
    val displayPercents: Map<String, Int> = emptyMap(),
    val caption: String = StandingSplitService.SAVED_MONEY_STAYS_PUT,
    val statusMessage: String = "",
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val shouldSkip: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    val errorMessage: String? = null,
) {
    val isSingleGoal: Boolean get() = goals.size == 1
    val isValid: Boolean get() = canSave
    val isInvalid: Boolean get() = !shouldSkip && goals.size >= 2 && !canSave && !isSaving
}
