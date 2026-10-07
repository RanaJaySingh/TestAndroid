package com.piplanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.HistoryNavigationTarget
import com.piplanner.domain.HistoryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * History tab (frames 12 / 12a) — newest-first list, open vs locked navigation.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val historyService: HistoryService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeState().collect { state ->
                apply(state.history)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            apply(repository.loadState().history)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onEntryClick(entryId: String) {
        val entry = _uiState.value.entries.firstOrNull { it.id == entryId } ?: return
        when (val target = historyService.navigationTarget(entry)) {
            is HistoryNavigationTarget.CreditEntry ->
                _uiState.update { it.copy(navigateToCreditEntryId = target.entryId) }
            is HistoryNavigationTarget.OpeningBalanceReadOnly ->
                _uiState.update { it.copy(navigateToOpeningEntryId = target.entryId) }
            is HistoryNavigationTarget.LockedDetail ->
                _uiState.update { it.copy(navigateToLockedDetailId = target.entryId) }
        }
    }

    fun consumeCreditEntryNavigation() {
        _uiState.update { it.copy(navigateToCreditEntryId = null) }
    }

    fun consumeOpeningEntryNavigation() {
        _uiState.update { it.copy(navigateToOpeningEntryId = null) }
    }

    fun consumeLockedDetailNavigation() {
        _uiState.update { it.copy(navigateToLockedDetailId = null) }
    }

    private fun apply(history: List<HistoryEntry>) {
        val ordered = historyService.entriesNewestFirst(history)
        val rows = ordered.map { entry ->
            HistoryRowUi(
                id = entry.id,
                type = entry.type,
                typeLabel = historyService.typeLabel(entry.type),
                typeIcon = historyService.typeIcon(entry.type),
                subtitle = historyService.rowSubtitle(entry),
                formattedAmount = historyService.formattedPrimaryAmount(entry),
                isLocked = entry.isLocked || entry.type == HistoryEntryType.OpeningBalance,
                isOpenAssignable = historyService.isOpenAssignable(entry),
                showLockIcon = entry.isLocked || entry.type == HistoryEntryType.OpeningBalance,
            )
        }
        _uiState.update {
            it.copy(
                entries = ordered,
                rows = rows,
                isEmpty = rows.isEmpty(),
            )
        }
    }
}

data class HistoryRowUi(
    val id: String,
    val type: HistoryEntryType,
    val typeLabel: String,
    val typeIcon: String,
    val subtitle: String,
    val formattedAmount: String,
    val isLocked: Boolean,
    val isOpenAssignable: Boolean,
    val showLockIcon: Boolean,
)

data class HistoryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val rows: List<HistoryRowUi> = emptyList(),
    val isEmpty: Boolean = true,
    val isLoading: Boolean = false,
    val navigateToCreditEntryId: String? = null,
    val navigateToOpeningEntryId: String? = null,
    val navigateToLockedDetailId: String? = null,
)
