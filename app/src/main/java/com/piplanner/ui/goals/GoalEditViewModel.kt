package com.piplanner.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalEditDraft
import com.piplanner.domain.GoalHeldChangeException
import com.piplanner.domain.GoalHeldChangeService
import com.piplanner.domain.GoalValidationService
import com.piplanner.ui.setup.GoalFormDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Goal edit (frame 6e) — saved amount locked; save records held change (toast 9c).
 */
@HiltViewModel
class GoalEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val heldChangeService: GoalHeldChangeService,
    val goalValidationService: GoalValidationService,
) : ViewModel() {

    val goalId: String = checkNotNull(savedStateHandle[GoalDetailViewModel.NAV_ARG_GOAL_ID]) {
        "goalId nav arg required"
    }

    private val clock: () -> Instant = { Instant.now() }

    private val _uiState = MutableStateFlow(GoalEditUiState())
    val uiState: StateFlow<GoalEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            val state = repository.loadState()
            val goal = state.goals.firstOrNull { it.id == goalId }
            if (goal == null) {
                _uiState.value = GoalEditUiState(missing = true, goalId = goalId)
                return@launch
            }
            _uiState.value = GoalEditUiState(
                goalId = goalId,
                missing = false,
                draft = GoalFormDraft.fromGoal(goal),
                formattedSavedLocked = formattingService.formatInrFromPaisa(goal.savedAmount),
                isSaving = false,
                errorMessage = null,
                showSavedToast = false,
                toastMessage = heldChangeService.toastMessage(),
                shouldNavigateBack = false,
            )
        }
    }

    fun updateDraft(transform: (GoalFormDraft) -> GoalFormDraft) {
        _uiState.update { current ->
            if (current.missing || current.isSaving) return@update current
            val nextDraft = transform(current.draft)
            // Saved amount is locked — ignore any draft mutation of savedAmount.
            current.copy(
                draft = nextDraft.copy(savedAmount = current.draft.savedAmount),
                errorMessage = null,
            )
        }
    }

    fun setShowInflationPopup(show: Boolean) {
        _uiState.update { it.copy(showInflationPopup = show) }
    }

    fun consumeToast() {
        _uiState.update { it.copy(showSavedToast = false) }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(shouldNavigateBack = false) }
    }

    fun save() {
        val current = _uiState.value
        if (current.missing || current.isSaving) return
        val draft = current.draft
        if (!draft.canSave(goalValidationService)) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val now = clock()
                val persisted = repository.loadState()
                val editDraft = GoalEditDraft(
                    name = draft.name,
                    targetAmount = draft.targetPaisa(goalValidationService),
                    startDate = draft.startDate.toString(),
                    endDate = draft.endDate.toString(),
                    inflationRate = draft.inflationRate,
                    shareOfNewCredits = draft.shareOfNewCredits,
                )
                val next = heldChangeService.applyHeldEdit(
                    state = persisted,
                    goalId = goalId,
                    draft = editDraft,
                    nowIso = now.toString(),
                )
                repository.saveState(next)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        showSavedToast = true,
                        toastMessage = heldChangeService.toastMessage(),
                        shouldNavigateBack = true,
                    )
                }
            } catch (error: GoalHeldChangeException) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = error.message)
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Couldn’t save changes",
                    )
                }
            }
        }
    }

    fun formatInr(paisa: Long): String = formattingService.formatInrFromPaisa(paisa)
}

data class GoalEditUiState(
    val goalId: String = "",
    val missing: Boolean = false,
    val draft: GoalFormDraft = GoalFormDraft(),
    val formattedSavedLocked: String = "₹0",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showInflationPopup: Boolean = false,
    val showSavedToast: Boolean = false,
    val toastMessage: String = GoalHeldChangeService.TOAST_MESSAGE,
    val shouldNavigateBack: Boolean = false,
) {
    fun canSave(validation: GoalValidationService): Boolean =
        !missing && !isSaving && draft.canSave(validation)
}
