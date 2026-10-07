package com.piplanner.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.FormattingService
import com.piplanner.domain.HistoryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Read-only History detail for Transfer / Withdrawal / GoalDeleted (and any locked non-credit).
 * Frame 12a caption: [HistoryService.ORIGINAL_AMOUNTS_CAPTION].
 */
@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: PiPlannerRepository,
    private val historyService: HistoryService,
    private val formattingService: FormattingService,
) : ViewModel() {

    val entryId: String = checkNotNull(savedStateHandle[NAV_ARG_ENTRY_ID]) {
        "entryId nav arg required"
    }

    val uiState: StateFlow<HistoryDetailUiState> = repository.observeState()
        .map { state ->
            val entry = state.history.firstOrNull { it.id == entryId }
            if (entry == null) {
                HistoryDetailUiState(entryId = entryId, missing = true)
            } else {
                toUiState(entry)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryDetailUiState(entryId = entryId),
        )

    private fun toUiState(entry: HistoryEntry): HistoryDetailUiState {
        return HistoryDetailUiState(
            entryId = entry.id,
            missing = false,
            entry = entry,
            typeLabel = historyService.typeLabel(entry.type),
            typeIcon = historyService.typeIcon(entry.type),
            formattedAmount = historyService.formattedPrimaryAmount(entry),
            dateLabel = entry.createdAt.take(10),
            caption = HistoryService.ORIGINAL_AMOUNTS_CAPTION,
            isReadOnly = historyService.isReadOnly(entry),
            allocationRows = entry.allocations.map { allocation ->
                HistoryAllocationRowUi(
                    goalId = allocation.goalId,
                    goalName = allocation.goalName,
                    formattedAmount = formattingService.formatInrFromPaisa(allocation.amount),
                    percentLabel = "${(allocation.percentage * 100.0).toInt()}%",
                )
            },
            detailLines = detailLines(entry),
        )
    }

    private fun detailLines(entry: HistoryEntry): List<String> {
        return when (entry.type) {
            HistoryEntryType.Transfer -> listOfNotNull(
                entry.fromGoalId?.let { "From goal: $it" },
                entry.toGoalId?.let { "To goal: $it" },
            )
            HistoryEntryType.Withdrawal -> listOfNotNull(
                entry.previousBalance?.let {
                    "Previous ${formattingService.formatInrFromPaisa(it)}"
                },
                entry.newBalance?.let {
                    "Balance now ${formattingService.formatInrFromPaisa(it)}"
                },
            )
            HistoryEntryType.GoalDeleted -> listOfNotNull(
                entry.deletedGoalName?.let { "Deleted: $it" },
            )
            HistoryEntryType.OpeningBalance,
            HistoryEntryType.NewCredit,
            -> emptyList()
        }
    }

    companion object {
        const val NAV_ARG_ENTRY_ID: String = "entryId"
    }
}

data class HistoryAllocationRowUi(
    val goalId: String,
    val goalName: String,
    val formattedAmount: String,
    val percentLabel: String,
)

data class HistoryDetailUiState(
    val entryId: String,
    val missing: Boolean = false,
    val entry: HistoryEntry? = null,
    val typeLabel: String = "",
    val typeIcon: String = "",
    val formattedAmount: String = "",
    val dateLabel: String = "",
    val caption: String = HistoryService.ORIGINAL_AMOUNTS_CAPTION,
    val isReadOnly: Boolean = true,
    val allocationRows: List<HistoryAllocationRowUi> = emptyList(),
    val detailLines: List<String> = emptyList(),
)
