package com.piplanner.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalStatus
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalHeldChangeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Goal detail (frame 14) — metrics, From History, held-edit banner (13g).
 */
@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val heldChangeService: GoalHeldChangeService,
) : ViewModel() {

    val goalId: String = checkNotNull(savedStateHandle[NAV_ARG_GOAL_ID]) {
        "goalId nav arg required"
    }

    val uiState: StateFlow<GoalDetailUiState> = repository.observeState()
        .map { state ->
            val goal = state.goals.firstOrNull { it.id == goalId }
            if (goal == null) {
                GoalDetailUiState(goalId = goalId, missing = true)
            } else {
                val status = goal.status()
                GoalDetailUiState(
                    goalId = goalId,
                    missing = false,
                    goal = goal,
                    formattedSaved = formattingService.formatInrFromPaisa(goal.savedAmount),
                    statusLabel = statusLabel(status),
                    formattedAdjustedTarget = formattingService.formatInrFromPaisa(goal.adjustedTarget()),
                    formattedMonthlyNeed = formattingService.formatInrFromPaisa(goal.monthlyNeed()),
                    inflationPercentLabel = "${(goal.inflationRate * 100.0).toInt()}%",
                    sharePercentLabel = "${(goal.shareOfNewCredits * 100.0).toInt()}%",
                    relatedHistory = heldChangeService.relatedHistoryEntries(state.history, goalId),
                    showHeldEditInfo = heldChangeService.hasHeldChange(state, goalId),
                    heldEditInfoMessage = heldChangeService.heldInfoMessage(),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GoalDetailUiState(goalId = goalId),
        )

    fun formatHistoryAmount(entry: HistoryEntry): String {
        val paisa = entry.creditAmount
            ?: entry.transferAmount
            ?: entry.withdrawalAmount
            ?: entry.releasedAmount
            ?: entry.allocations.firstOrNull { it.goalId == goalId }?.amount
            ?: 0L
        return formattingService.formatInrFromPaisa(paisa)
    }

    private fun statusLabel(status: GoalStatus): String {
        return when (status) {
            is GoalStatus.OnTrack -> "On track"
            is GoalStatus.Behind ->
                "Behind · ${formattingService.formatInrFromPaisa(status.shortfall)}"
        }
    }

    companion object {
        const val NAV_ARG_GOAL_ID: String = "goalId"
    }
}

data class GoalDetailUiState(
    val goalId: String,
    val missing: Boolean = false,
    val goal: Goal? = null,
    val formattedSaved: String = "₹0",
    val statusLabel: String = "",
    val formattedAdjustedTarget: String = "₹0",
    val formattedMonthlyNeed: String = "₹0",
    val inflationPercentLabel: String = "7%",
    val sharePercentLabel: String = "0%",
    val relatedHistory: List<HistoryEntry> = emptyList(),
    val showHeldEditInfo: Boolean = false,
    val heldEditInfoMessage: String = GoalHeldChangeService.HELD_INFO_MESSAGE,
)
