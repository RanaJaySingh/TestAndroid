package com.piplanner.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.DeleteGoalException
import com.piplanner.domain.DeleteGoalService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalValidationService
import com.piplanner.domain.OpeningSplitService
import com.piplanner.domain.StandingSplitService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject

/**
 * Delete goal flow — frames 17 / 17a–17e (PRD R13, Spec BR-9).
 * States: Reassign default, Reassign edit, Confirm, Only-goal gate.
 * Reassignment percents follow iOS PIP-53: one Edit→Done pass ([hasEditedOnce]), then locked.
 */
@HiltViewModel
class DeleteGoalViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val deleteGoalService: DeleteGoalService,
    private val openingSplitService: OpeningSplitService,
    private val standingSplitService: StandingSplitService,
    private val goalValidationService: GoalValidationService,
) : ViewModel() {

    private val clock: () -> Instant = { Instant.now() }
    private val makeId: () -> String = { UUID.randomUUID().toString() }

    private var configuredGoalId: String? = null

    private val _uiState = MutableStateFlow(DeleteGoalUiState())
    val uiState: StateFlow<DeleteGoalUiState> = _uiState.asStateFlow()

    /**
     * Loads the goal to delete from persistence.
     * Safe to call again with the same id when the destination is shown.
     */
    fun configure(goalId: String) {
        if (configuredGoalId == goalId &&
            _uiState.value.phase != DeleteGoalPhase.Loading &&
            _uiState.value.phase != DeleteGoalPhase.MissingGoal
        ) {
            return
        }
        configuredGoalId = goalId
        _uiState.value = DeleteGoalUiState(phase = DeleteGoalPhase.Loading)
        viewModelScope.launch {
            val state = repository.loadState()
            val deleted = state.goals.firstOrNull { it.id == goalId }
            if (deleted == null) {
                _uiState.value = DeleteGoalUiState(
                    phase = DeleteGoalPhase.MissingGoal,
                    errorMessage = "Goal not found.",
                )
                return@launch
            }
            val destinations = state.goals.filter { it.id != goalId }
            publishConfigured(
                deletedGoal = deleted,
                allGoals = state.goals,
                destinations = destinations,
                replacement = null,
                resetStandingToEqual = false,
                phase = if (destinations.isEmpty()) {
                    DeleteGoalPhase.OnlyGoalGate
                } else {
                    DeleteGoalPhase.ReassignDefault
                },
            )
        }
    }

    /** Starts the single Edit→Done reassignment pass (BR-9 / iOS hasEditedOnce). */
    fun beginEditPercents() {
        val state = _uiState.value
        if (!state.canStartEdit) return
        _uiState.update {
            it.copy(
                isEditingPercents = true,
                phase = DeleteGoalPhase.ReassignEdit,
                errorMessage = null,
            )
        }
    }

    /**
     * Ends the edit pass and locks percents until Confirm (iOS PIP-53 `finishEdit`).
     * Rejects Done when percents do not sum to 100% — keeps editing, does not set [hasEditedOnce].
     */
    fun finishEditPercents() {
        val state = _uiState.value
        if (!state.isEditingPercents) return
        val fractions = orderedFractions(state.destinationGoals, state.displayPercents)
        if (!openingSplitService.isValidHundredPercent(fractions)) {
            _uiState.update {
                it.copy(
                    errorMessage = openingSplitService.shortfallMessage(fractions)
                        ?: "Reassignment must total 100%.",
                    statusMessage = buildStatusMessage(it.destinationGoals, it.displayPercents),
                    canConfirm = false,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                isEditingPercents = false,
                hasEditedOnce = true,
                phase = DeleteGoalPhase.ReassignEdit,
                errorMessage = null,
                canConfirm = canConfirm(
                    destinations = it.destinationGoals,
                    displayPercents = it.displayPercents,
                    isConfirming = it.isConfirming,
                ),
                statusMessage = buildStatusMessage(it.destinationGoals, it.displayPercents),
            )
        }
    }

    fun setDisplayPercent(goalId: String, percent: Int) {
        val state = _uiState.value
        if (!state.canEditPercents) return
        val clamped = percent.coerceIn(0, 100)
        val next = state.displayPercents.toMutableMap().apply { put(goalId, clamped) }
        _uiState.update { current ->
            current.copy(
                displayPercents = next,
                amountsByGoalId = computeAmounts(current.deletedGoal, current.destinationGoals, next),
                statusMessage = buildStatusMessage(current.destinationGoals, next),
                canConfirm = canConfirm(
                    destinations = current.destinationGoals,
                    displayPercents = next,
                    isConfirming = current.isConfirming,
                ),
                phase = DeleteGoalPhase.ReassignEdit,
                errorMessage = null,
            )
        }
    }

    fun requestConfirm() {
        if (!_uiState.value.canConfirm) return
        _uiState.update {
            it.copy(phase = DeleteGoalPhase.Confirm, showConfirmDialog = true)
        }
    }

    fun dismissConfirm() {
        _uiState.update {
            it.copy(
                showConfirmDialog = false,
                phase = if (it.hasEditedOnce || it.isEditingPercents) {
                    DeleteGoalPhase.ReassignEdit
                } else {
                    DeleteGoalPhase.ReassignDefault
                },
            )
        }
    }

    fun confirmDelete() {
        val state = _uiState.value
        if (!state.canConfirm) return
        _uiState.update {
            it.copy(
                isConfirming = true,
                showConfirmDialog = false,
                errorMessage = null,
                canConfirm = false,
            )
        }

        viewModelScope.launch {
            try {
                val deleted = state.deletedGoal
                    ?: throw DeleteGoalException("goalId", "Goal not found.")
                val destinations = state.destinationGoals
                val fractions = fractionMap(destinations, state.displayPercents)
                val now = clock().toString()
                val persisted = repository.loadState()
                val next = deleteGoalService.applyDelete(
                    state = persisted,
                    deletedGoalId = deleted.id,
                    percentages = fractions,
                    nowIso = now,
                    replacementGoal = state.replacementGoal,
                    resetStandingSplitToEqual = state.resetStandingToEqual,
                    entryId = makeId(),
                )
                repository.saveState(next)
                _uiState.update {
                    it.copy(
                        isConfirming = false,
                        phase = DeleteGoalPhase.Completed,
                        shouldNavigateBack = true,
                    )
                }
            } catch (error: DeleteGoalException) {
                _uiState.update {
                    it.copy(
                        isConfirming = false,
                        errorMessage = error.message,
                        canConfirm = canConfirm(
                            destinations = it.destinationGoals,
                            displayPercents = it.displayPercents,
                            isConfirming = false,
                        ),
                        phase = when {
                            it.destinationGoals.isEmpty() -> DeleteGoalPhase.OnlyGoalGate
                            it.hasEditedOnce || it.isEditingPercents -> DeleteGoalPhase.ReassignEdit
                            else -> DeleteGoalPhase.ReassignDefault
                        },
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isConfirming = false,
                        errorMessage = error.message ?: "Couldn’t delete goal",
                        canConfirm = canConfirm(
                            destinations = it.destinationGoals,
                            displayPercents = it.displayPercents,
                            isConfirming = false,
                        ),
                    )
                }
            }
        }
    }

    fun updateReplacementDraft(draft: ReplacementGoalDraft) {
        _uiState.update { it.copy(replacementDraft = draft, errorMessage = null) }
    }

    /** Frame 17e / 17d — create a replacement (or mid-delete) goal, then reassign. */
    fun saveReplacementGoal() {
        val state = _uiState.value
        val draft = state.replacementDraft
        val deleted = state.deletedGoal ?: return
        val start = runCatching { LocalDate.parse(draft.startDate) }.getOrNull()
        val end = runCatching { LocalDate.parse(draft.endDate) }.getOrNull()
        if (start == null || end == null ||
            !goalValidationService.canSave(draft.name, draft.targetPaisa, start, end)
        ) {
            _uiState.update {
                it.copy(errorMessage = "Enter a valid name, target, and dates for the new goal.")
            }
            return
        }

        val now = clock()
        val createdAt = now.toString()
        val replacement = Goal(
            id = makeId(),
            name = draft.name.trim(),
            targetAmount = draft.targetPaisa,
            startDate = start.toString(),
            endDate = end.toString(),
            inflationRate = draft.inflationRate,
            savedAmount = 0L,
            shareOfNewCredits = 0.0,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

        val priorDestinations = state.allGoals.filter { it.id != deleted.id }
        val destinations = priorDestinations + replacement
        publishConfigured(
            deletedGoal = deleted,
            allGoals = state.allGoals,
            destinations = destinations,
            replacement = replacement,
            resetStandingToEqual = true, // 17d — may reset standing split to equal
            phase = DeleteGoalPhase.ReassignDefault,
        )
    }

    fun showCreateReplacement() {
        _uiState.update {
            it.copy(
                showCreateForm = true,
                replacementDraft = ReplacementGoalDraft.default(clock()),
            )
        }
    }

    fun hideCreateReplacement() {
        if (_uiState.value.isOnlyGoalGate) return
        _uiState.update { it.copy(showCreateForm = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(shouldNavigateBack = false) }
    }

    fun formattedAmount(goalId: String): String {
        val paisa = _uiState.value.amountsByGoalId[goalId] ?: 0L
        return formattingService.formatInrFromPaisa(paisa)
    }

    private fun publishConfigured(
        deletedGoal: Goal,
        allGoals: List<Goal>,
        destinations: List<Goal>,
        replacement: Goal?,
        resetStandingToEqual: Boolean,
        phase: DeleteGoalPhase,
    ) {
        val displayPercents = deleteGoalService.equalReassignmentDisplayPercents(destinations)
        val standingPreview = if (resetStandingToEqual || destinations.isNotEmpty()) {
            standingSplitService.resetToEqual(destinations.map { it.id })
                .associate { it.goalId to (it.percentage * 100.0).toInt() }
        } else {
            emptyMap()
        }
        _uiState.value = DeleteGoalUiState(
            phase = phase,
            deletedGoal = deletedGoal,
            allGoals = allGoals,
            destinationGoals = destinations,
            replacementGoal = replacement,
            resetStandingToEqual = resetStandingToEqual,
            releasedAmount = deletedGoal.savedAmount,
            formattedReleasedAmount = formattingService.formatInrFromPaisa(deletedGoal.savedAmount),
            displayPercents = displayPercents,
            amountsByGoalId = computeAmounts(deletedGoal, destinations, displayPercents),
            statusMessage = buildStatusMessage(destinations, displayPercents),
            canConfirm = canConfirm(
                destinations = destinations,
                displayPercents = displayPercents,
                isConfirming = false,
            ),
            showCreateForm = phase == DeleteGoalPhase.OnlyGoalGate,
            replacementDraft = ReplacementGoalDraft.default(clock()),
            previewStandingSplits = standingPreview,
            isEditingPercents = false,
            hasEditedOnce = false,
        )
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
        deletedGoal: Goal?,
        destinations: List<Goal>,
        displayPercents: Map<String, Int>,
    ): Map<String, Long> {
        if (deletedGoal == null || destinations.isEmpty()) return emptyMap()
        val amounts = openingSplitService.allocatePaisa(
            total = deletedGoal.savedAmount,
            fractions = orderedFractions(destinations, displayPercents),
        )
        return destinations.mapIndexed { index, goal -> goal.id to amounts[index] }.toMap()
    }

    private fun buildStatusMessage(
        destinations: List<Goal>,
        displayPercents: Map<String, Int>,
    ): String {
        if (destinations.isEmpty()) {
            return "Create a replacement goal before confirming delete."
        }
        if (destinations.size == 1) {
            return "100% moves to ${destinations.first().name}."
        }
        return openingSplitService.shortfallMessage(orderedFractions(destinations, displayPercents))
            ?: "Total 100%. Ready to confirm."
    }

    private fun canConfirm(
        destinations: List<Goal>,
        displayPercents: Map<String, Int>,
        isConfirming: Boolean,
    ): Boolean {
        if (isConfirming || destinations.isEmpty()) return false
        return openingSplitService.isValidHundredPercent(
            orderedFractions(destinations, displayPercents),
        )
    }
}

enum class DeleteGoalPhase {
    Loading,
    ReassignDefault,
    ReassignEdit,
    Confirm,
    OnlyGoalGate,
    Completed,
    MissingGoal,
}

data class ReplacementGoalDraft(
    val name: String = "",
    val targetRupeesText: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val inflationRate: Double = Goal.DEFAULT_INFLATION_RATE,
) {
    val targetPaisa: Long
        get() {
            val digits = targetRupeesText.filter { it.isDigit() }
            if (digits.isEmpty()) return 0L
            return digits.toLongOrNull()?.times(100L) ?: 0L
        }

    companion object {
        fun default(now: Instant): ReplacementGoalDraft {
            val today = now.atZone(ZoneOffset.UTC).toLocalDate()
            return ReplacementGoalDraft(
                startDate = today.toString(),
                endDate = today.plusYears(1).toString(),
            )
        }
    }
}

data class DeleteGoalUiState(
    val phase: DeleteGoalPhase = DeleteGoalPhase.Loading,
    val deletedGoal: Goal? = null,
    val allGoals: List<Goal> = emptyList(),
    val destinationGoals: List<Goal> = emptyList(),
    val replacementGoal: Goal? = null,
    val resetStandingToEqual: Boolean = false,
    val releasedAmount: Long = 0L,
    val formattedReleasedAmount: String = "₹0",
    val displayPercents: Map<String, Int> = emptyMap(),
    val amountsByGoalId: Map<String, Long> = emptyMap(),
    val previewStandingSplits: Map<String, Int> = emptyMap(),
    val statusMessage: String = "",
    val canConfirm: Boolean = false,
    val isConfirming: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val showCreateForm: Boolean = false,
    val replacementDraft: ReplacementGoalDraft = ReplacementGoalDraft(),
    val errorMessage: String? = null,
    val shouldNavigateBack: Boolean = false,
    /** True only during the single Edit→Done reassignment pass. */
    val isEditingPercents: Boolean = false,
    /** After Done, percents stay locked until Confirm (iOS hasEditedOnce). */
    val hasEditedOnce: Boolean = false,
) {
    val isOnlyGoalGate: Boolean get() = phase == DeleteGoalPhase.OnlyGoalGate

    /** Percents are writable only inside the one Edit→Done pass. */
    val canEditPercents: Boolean
        get() = isEditingPercents &&
            !hasEditedOnce &&
            destinationGoals.size > 1 &&
            !isConfirming &&
            phase != DeleteGoalPhase.Completed &&
            phase != DeleteGoalPhase.MissingGoal &&
            phase != DeleteGoalPhase.OnlyGoalGate

    /** Show Edit when multi-destination equal default has not used its one pass yet. */
    val canStartEdit: Boolean
        get() = !hasEditedOnce &&
            !isEditingPercents &&
            destinationGoals.size > 1 &&
            !isConfirming &&
            phase != DeleteGoalPhase.Completed &&
            phase != DeleteGoalPhase.MissingGoal &&
            phase != DeleteGoalPhase.OnlyGoalGate &&
            phase != DeleteGoalPhase.Confirm
}
