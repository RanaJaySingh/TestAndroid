package com.piplanner.ui.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.AskAnswerService
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalValidationService
import com.piplanner.domain.GrokError
import com.piplanner.domain.GrokException
import com.piplanner.domain.GrokService
import com.piplanner.domain.ProposedAction
import com.piplanner.domain.StubGrokService
import com.piplanner.domain.TransferAskPrefill
import com.piplanner.ui.setup.GoalFormDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Ask tab UI phases — frames 19 / 19a / 19b / 19c / 19d. */
enum class AskPhase {
    Input,
    PlainAnswer,
    Proposal,
    Unavailable,
    InvalidDraft,
    GoalForm,
}

/**
 * View model for Ask tab (PRD R18 / R19, Spec §3.3 / §5.2 Ask).
 */
@HiltViewModel
class AskViewModel @Inject constructor(
    private val repository: PiPlannerRepository,
    private val grok: GrokService,
    private val askAnswers: AskAnswerService,
    private val formatting: FormattingService,
    private val validation: GoalValidationService,
) : ViewModel() {

    private val clock: () -> Instant = { Instant.now() }

    private val _uiState = MutableStateFlow(AskUiState())
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeState().collect { state ->
                _uiState.update {
                    it.copy(
                        goals = state.goals,
                        standingSplits = state.standingSplits,
                        totalSavingsPaisa = state.accounts
                            .firstOrNull { account -> account.isDedicated }
                            ?.balance
                            ?: state.goals.sumOf { goal -> goal.savedAmount },
                    )
                }
            }
        }
        if (isGrokUnavailable()) {
            _uiState.update {
                it.copy(
                    phase = AskPhase.Unavailable,
                    statusMessage = "Grok is unavailable. Use a form, standing-split sliders, or a template sentence.",
                )
            }
        }
    }

    fun setDraftInput(value: String) {
        _uiState.update { it.copy(draftInput = value) }
    }

    fun selectChip(chip: String) {
        _uiState.update { it.copy(draftInput = chip) }
        submit()
    }

    fun selectTemplate(template: String) {
        _uiState.update {
            it.copy(
                draftInput = template,
                phase = AskPhase.Input,
                statusMessage = null,
            )
        }
    }

    fun submit() {
        val query = _uiState.value.draftInput.trim()
        if (query.isEmpty()) return

        if (isGrokUnavailable()) {
            _uiState.update {
                it.copy(
                    phase = AskPhase.Unavailable,
                    statusMessage = "Grok is unavailable. Use a form, standing-split sliders, or a template sentence.",
                    plainAnswer = null,
                    proposedAction = null,
                )
            }
            return
        }

        val result = grok.askQuestion(query)
        val failure = result.exceptionOrNull() as? GrokException
        if (failure != null) {
            handleFailure(failure.error)
            return
        }
        when (val response = result.getOrNull()) {
            is com.piplanner.domain.AskResponse.PlainAnswer -> {
                val state = _uiState.value
                val text = askAnswers.plainAnswer(
                    query = query,
                    goals = state.goals,
                    standingSplits = state.standingSplits,
                    totalSavingsPaisa = state.totalSavingsPaisa,
                ).ifBlank { response.text }
                _uiState.update {
                    it.copy(
                        phase = AskPhase.PlainAnswer,
                        plainAnswer = text,
                        proposedAction = null,
                        proposalTitle = "",
                        proposalBody = "",
                        statusMessage = null,
                    )
                }
            }
            is com.piplanner.domain.AskResponse.ActionProposal -> {
                presentProposal(response.action)
            }
            null -> handleFailure(GrokError.InvalidDraft)
        }
    }

    fun confirmProposal() {
        val action = _uiState.value.proposedAction ?: return
        when (action) {
            is ProposedAction.Transfer -> {
                val prefill = StubGrokService.transferPrefill(action) ?: return
                _uiState.update {
                    it.copy(
                        navigateTransfer = prefill,
                        phase = AskPhase.Input,
                        proposedAction = null,
                        plainAnswer = null,
                    )
                }
            }
            is ProposedAction.ChangeSplit -> {
                _uiState.update {
                    it.copy(
                        navigateStandingSplit = true,
                        phase = AskPhase.Input,
                        proposedAction = null,
                        plainAnswer = null,
                    )
                }
            }
            is ProposedAction.AddGoal -> {
                _uiState.update {
                    it.copy(
                        phase = AskPhase.GoalForm,
                        formDraft = GoalFormDraft.fromProposal(action.proposal, now = clock()),
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = null,
                    )
                }
            }
        }
    }

    fun editProposal() {
        val action = _uiState.value.proposedAction
        when (action) {
            is ProposedAction.AddGoal -> {
                _uiState.update {
                    it.copy(
                        phase = AskPhase.GoalForm,
                        formDraft = GoalFormDraft.fromProposal(action.proposal, now = clock()),
                        proposedAction = null,
                        plainAnswer = null,
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        phase = AskPhase.Input,
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = "Edit your ask and send again.",
                    )
                }
            }
        }
    }

    fun useFormPath() {
        _uiState.update {
            it.copy(
                phase = AskPhase.GoalForm,
                formDraft = GoalFormDraft.blank(remainingShare = 0.2, now = clock()),
                proposedAction = null,
                plainAnswer = null,
                statusMessage = null,
            )
        }
    }

    fun openStandingSplitFallback() {
        _uiState.update { it.copy(navigateStandingSplit = true) }
    }

    fun updateFormDraft(transform: (GoalFormDraft) -> GoalFormDraft) {
        _uiState.update { it.copy(formDraft = transform(it.formDraft)) }
    }

    fun setShowInflationPopup(show: Boolean) {
        _uiState.update { it.copy(showInflationPopup = show) }
    }

    fun saveForm(): Boolean {
        val draft = _uiState.value.formDraft
        if (!draft.canSave(validation)) return false
        viewModelScope.launch {
            val state = repository.loadState()
            val goal = draft.makeGoal(
                id = UUID.randomUUID().toString(),
                validation = validation,
                now = clock(),
            )
            val nextGoals = state.goals + goal
            repository.saveState(state.copy(goals = nextGoals))
            _uiState.update {
                it.copy(
                    phase = AskPhase.Input,
                    statusMessage = "Goal saved. Standing split may need an update.",
                    formDraft = GoalFormDraft.blank(remainingShare = 0.0, now = clock()),
                    navigateStandingSplit = nextGoals.size > 1,
                )
            }
        }
        return true
    }

    fun cancelForm() {
        _uiState.update {
            it.copy(
                phase = if (isGrokUnavailable()) AskPhase.Unavailable else AskPhase.Input,
                formDraft = GoalFormDraft.blank(remainingShare = 0.2, now = clock()),
            )
        }
    }

    fun consumeTransferNavigation() {
        _uiState.update { it.copy(navigateTransfer = null) }
    }

    fun consumeStandingSplitNavigation() {
        _uiState.update { it.copy(navigateStandingSplit = false) }
    }

    fun formatInr(paisa: Long): String = formatting.formatInrFromPaisa(paisa)

    fun validationService(): GoalValidationService = validation

    private fun presentProposal(action: ProposedAction) {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                phase = AskPhase.Proposal,
                proposedAction = action,
                proposalTitle = askAnswers.proposalTitle(action),
                proposalBody = askAnswers.proposalSummary(action, state.goals),
                plainAnswer = null,
                statusMessage = null,
                checkedByLabel = StubGrokService.CHECKED_BY_LABEL,
            )
        }
    }

    private fun handleFailure(error: GrokError) {
        when (error) {
            GrokError.Unavailable -> {
                _uiState.update {
                    it.copy(
                        phase = AskPhase.Unavailable,
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = "Grok is unavailable. Use a form, standing-split sliders, or a template sentence.",
                    )
                }
            }
            GrokError.InvalidDraft, GrokError.RateLimited -> {
                val alreadyAsked = _uiState.value.invalidRetryUsed
                if (alreadyAsked) {
                    // 19d — after one retry, open Goal form; invalid drafts never shown.
                    _uiState.update {
                        it.copy(
                            phase = AskPhase.GoalForm,
                            formDraft = GoalFormDraft.blank(remainingShare = 0.2, now = clock()),
                            proposedAction = null,
                            plainAnswer = null,
                            invalidRetryUsed = false,
                            statusMessage = "Still couldn't make a draft. Use the Goal form.",
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            phase = AskPhase.InvalidDraft,
                            proposedAction = null,
                            plainAnswer = null,
                            invalidRetryUsed = true,
                            statusMessage = StubGrokService.INVALID_DRAFT_RETRY_PROMPT,
                        )
                    }
                }
            }
        }
    }

    private fun isGrokUnavailable(): Boolean {
        val probe = grok.askQuestion("How am I doing?")
        val error = (probe.exceptionOrNull() as? GrokException)?.error
        return error == GrokError.Unavailable
    }
}

data class AskUiState(
    val phase: AskPhase = AskPhase.Input,
    val draftInput: String = "",
    val plainAnswer: String? = null,
    val proposedAction: ProposedAction? = null,
    val proposalTitle: String = "",
    val proposalBody: String = "",
    val checkedByLabel: String = StubGrokService.CHECKED_BY_LABEL,
    val statusMessage: String? = null,
    val invalidRetryUsed: Boolean = false,
    val formDraft: GoalFormDraft = GoalFormDraft.blank(remainingShare = 0.2),
    val showInflationPopup: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val standingSplits: List<com.piplanner.data.model.StandingSplit> = emptyList(),
    val totalSavingsPaisa: Long = 0L,
    val navigateTransfer: TransferAskPrefill? = null,
    val navigateStandingSplit: Boolean = false,
    val suggestionChips: List<String> = StubGrokService.ASK_CHIPS,
    val fallbackTemplates: List<String> = StubGrokService.FALLBACK_TEMPLATES,
)
