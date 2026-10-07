package com.piplanner.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.ConsentService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.TransferException
import com.piplanner.domain.TransferPhase
import com.piplanner.domain.TransferService
import com.piplanner.domain.TransferValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Transfer between goals (frames 16 / 16a / 16b / 16c) — PRD R14, Spec BR-7.
 *
 * Optional nav args support Ask proposal pre-fill (16c):
 * [NAV_ARG_FROM_GOAL_ID], [NAV_ARG_TO_GOAL_ID], [NAV_ARG_AMOUNT_PAISA].
 */
@HiltViewModel
class TransferViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PiPlannerRepository,
    private val formattingService: FormattingService,
    private val transferService: TransferService,
) : ViewModel() {

    private val prefillFromGoalId: String =
        savedStateHandle.get<String>(NAV_ARG_FROM_GOAL_ID).orEmpty()
    private val prefillToGoalId: String =
        savedStateHandle.get<String>(NAV_ARG_TO_GOAL_ID).orEmpty()
    private val prefillAmountPaisa: Long? =
        savedStateHandle.get<String>(NAV_ARG_AMOUNT_PAISA)
            ?.takeIf { it.isNotBlank() }
            ?.toLongOrNull()

    private val _uiState = MutableStateFlow(
        TransferUiState(
            isLoading = true,
            fromGoalId = prefillFromGoalId.ifBlank { null },
            toGoalId = prefillToGoalId.ifBlank { null },
            amountPaisa = prefillAmountPaisa ?: 0L,
            amountRupeeDigits = prefillAmountPaisa
                ?.takeIf { it > 0L }
                ?.let { (it / FormattingService.PAISA_PER_RUPEE).toString() }
                .orEmpty(),
        ),
    )
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = repository.loadState()
            publish(
                goals = state.goals,
                fromGoalId = _uiState.value.fromGoalId,
                toGoalId = _uiState.value.toGoalId,
                amountRupeeDigits = _uiState.value.amountRupeeDigits,
                completed = false,
                clearError = true,
            )
        }
    }

    fun selectFromGoal(goalId: String) {
        val current = _uiState.value
        if (current.isMoving || current.phase == TransferPhase.Complete) return
        publish(
            goals = current.goals,
            fromGoalId = goalId,
            toGoalId = current.toGoalId,
            amountRupeeDigits = current.amountRupeeDigits,
            completed = false,
            clearError = true,
        )
    }

    fun selectToGoal(goalId: String) {
        val current = _uiState.value
        if (current.isMoving || current.phase == TransferPhase.Complete) return
        publish(
            goals = current.goals,
            fromGoalId = current.fromGoalId,
            toGoalId = goalId,
            amountRupeeDigits = current.amountRupeeDigits,
            completed = false,
            clearError = true,
        )
    }

    fun setAmountDigits(digits: String) {
        val current = _uiState.value
        if (current.isMoving || current.phase == TransferPhase.Complete) return
        val filtered = digits.filter { it.isDigit() }
        publish(
            goals = current.goals,
            fromGoalId = current.fromGoalId,
            toGoalId = current.toGoalId,
            amountRupeeDigits = filtered,
            completed = false,
            clearError = true,
        )
    }

    fun applyChip(amountPaisa: Long) {
        if (amountPaisa !in TransferService.AMOUNT_CHIP_PAISA) return
        val rupees = amountPaisa / FormattingService.PAISA_PER_RUPEE
        setAmountDigits(rupees.toString())
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeNavigateBack() {
        _uiState.update { it.copy(navigateBack = false) }
    }

    /** Applies a valid transfer: moves saved amounts and appends History (16a). */
    fun move() {
        val current = _uiState.value
        if (!current.canMove || current.isMoving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMoving = true, errorMessage = null) }
            try {
                val persisted = repository.loadState()
                val now = Instant.now().toString()
                val next = transferService.applyTransfer(
                    state = persisted,
                    fromGoalId = checkNotNull(current.fromGoalId),
                    toGoalId = checkNotNull(current.toGoalId),
                    amountPaisa = current.amountPaisa,
                    id = UUID.randomUUID().toString(),
                    createdAt = now,
                    nowIso = now,
                )
                repository.saveState(next)
                publish(
                    goals = next.goals,
                    fromGoalId = current.fromGoalId,
                    toGoalId = current.toGoalId,
                    amountRupeeDigits = current.amountRupeeDigits,
                    completed = true,
                    clearError = true,
                )
                _uiState.update {
                    it.copy(
                        isMoving = false,
                        navigateBack = true,
                        statusMessage = TransferService.COMPLETE_MESSAGE,
                    )
                }
            } catch (error: TransferException) {
                _uiState.update {
                    it.copy(
                        isMoving = false,
                        errorMessage = error.message,
                        canMove = false,
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isMoving = false,
                        errorMessage = error.message ?: "Couldn’t complete transfer",
                        canMove = false,
                    )
                }
            }
        }
    }

    fun formattedSaved(goal: Goal): String =
        formattingService.formatInrFromPaisa(goal.savedAmount)

    fun formattedAmount(paisa: Long): String =
        formattingService.formatInrFromPaisa(paisa)

    private fun publish(
        goals: List<Goal>,
        fromGoalId: String?,
        toGoalId: String?,
        amountRupeeDigits: String,
        completed: Boolean,
        clearError: Boolean,
    ) {
        val amountPaisa = ConsentService.paisaFromRupeeDigits(amountRupeeDigits)
        val phase = transferService.resolvePhase(
            goals = goals,
            fromGoalId = fromGoalId,
            toGoalId = toGoalId,
            amountPaisa = amountPaisa,
            completed = completed,
        )
        val validation = transferService.validate(
            goals = goals,
            fromGoalId = fromGoalId,
            toGoalId = toGoalId,
            amountPaisa = amountPaisa,
        )
        val canMove = validation is TransferValidation.Valid && !completed
        val preview = validation as? TransferValidation.Valid
        val status = when (phase) {
            TransferPhase.Select -> TransferService.SELECT_GOALS_MESSAGE
            TransferPhase.EnterAmount -> TransferService.ENTER_AMOUNT_MESSAGE
            TransferPhase.OverAmount -> TransferService.OVER_AMOUNT_MESSAGE
            TransferPhase.Preview -> TransferService.AFTER_TRANSFER_TITLE
            TransferPhase.Complete -> TransferService.COMPLETE_MESSAGE
        }
        _uiState.value = TransferUiState(
            isLoading = false,
            goals = goals,
            fromGoalId = fromGoalId,
            toGoalId = toGoalId,
            amountRupeeDigits = amountRupeeDigits,
            amountPaisa = amountPaisa,
            phase = phase,
            canMove = canMove,
            isMoving = false,
            statusMessage = status,
            afterFromSavedPaisa = preview?.afterFromSaved,
            afterToSavedPaisa = preview?.afterToSaved,
            errorMessage = if (clearError) null else _uiState.value.errorMessage,
            navigateBack = false,
            chipAmountsPaisa = TransferService.AMOUNT_CHIP_PAISA,
        )
    }

    companion object {
        const val NAV_ARG_FROM_GOAL_ID: String = "fromGoalId"
        const val NAV_ARG_TO_GOAL_ID: String = "toGoalId"
        const val NAV_ARG_AMOUNT_PAISA: String = "amountPaisa"
    }
}

data class TransferUiState(
    val isLoading: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val fromGoalId: String? = null,
    val toGoalId: String? = null,
    val amountRupeeDigits: String = "",
    val amountPaisa: Long = 0L,
    val phase: TransferPhase = TransferPhase.Select,
    val canMove: Boolean = false,
    val isMoving: Boolean = false,
    val statusMessage: String = "",
    val afterFromSavedPaisa: Long? = null,
    val afterToSavedPaisa: Long? = null,
    val errorMessage: String? = null,
    val navigateBack: Boolean = false,
    val chipAmountsPaisa: List<Long> = TransferService.AMOUNT_CHIP_PAISA,
) {
    val fromGoal: Goal? get() = goals.firstOrNull { it.id == fromGoalId }
    val toGoal: Goal? get() = goals.firstOrNull { it.id == toGoalId }
    val showPreview: Boolean
        get() = phase == TransferPhase.Preview || phase == TransferPhase.Complete
    val isOverAmount: Boolean get() = phase == TransferPhase.OverAmount
}
