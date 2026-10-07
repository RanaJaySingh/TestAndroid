package com.piplanner.ui.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piplanner.data.model.Goal
import com.piplanner.data.repository.PiPlannerRepository
import com.piplanner.domain.AskAnswerService
import com.piplanner.domain.AskStandingSplitSeed
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
    private val standingSplitSeed: AskStandingSplitSeed,
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

    /**
     * Frame 19c — template tap while unavailable opens the matching fallback sheet
     * without calling Grok (iOS `selectUnavailableTemplate` / `openFallback` parity).
     */
    fun selectTemplate(template: String) {
        _uiState.update { it.copy(draftInput = template, statusMessage = null) }
        if (_uiState.value.phase == AskPhase.Unavailable || isGrokUnavailable()) {
            openFallbackForTemplate(template)
            return
        }
        submit()
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

    /** Confirm and Edit share the same sheet-open path (iOS PIP-63 parity). */
    fun confirmProposal() {
        val action = _uiState.value.proposedAction ?: return
        openSheet(forAction = action)
    }

    fun editProposal() {
        val action = _uiState.value.proposedAction ?: return
        openSheet(forAction = action)
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
        _uiState.update {
            it.copy(
                navigateStandingSplit = false,
                standingSplitPrefill = null,
            )
        }
    }

    /** Pending ChangeSplit percents for Standing split seed (consumed by nav). */
    fun takeStandingSplitPrefill(): List<com.piplanner.data.model.StandingSplit>? {
        val prefill = _uiState.value.standingSplitPrefill
        if (prefill != null) {
            _uiState.update { it.copy(standingSplitPrefill = null) }
        }
        return prefill
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

    /**
     * Opens the matching sheet for a proposal (Transfer / Goal form / Standing split).
     * Used by both Confirm and Edit — never returns to Input-only for Transfer/ChangeSplit.
     */
    private fun openSheet(forAction: ProposedAction) {
        when (forAction) {
            is ProposedAction.Transfer -> {
                val prefill = StubGrokService.transferPrefill(forAction) ?: return
                _uiState.update {
                    it.copy(
                        navigateTransfer = prefill,
                        standingSplitPrefill = null,
                        phase = AskPhase.Input,
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = null,
                    )
                }
            }
            is ProposedAction.ChangeSplit -> {
                standingSplitSeed.set(forAction.newSplit)
                _uiState.update {
                    it.copy(
                        navigateStandingSplit = true,
                        standingSplitPrefill = forAction.newSplit,
                        phase = AskPhase.Input,
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = null,
                    )
                }
            }
            is ProposedAction.AddGoal -> {
                _uiState.update {
                    it.copy(
                        phase = AskPhase.GoalForm,
                        formDraft = GoalFormDraft.fromProposal(forAction.proposal, now = clock()),
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = null,
                    )
                }
            }
        }
    }

    /**
     * Unavailable (19c) template → matching fallback without [GrokService.askQuestion].
     */
    private fun openFallbackForTemplate(template: String) {
        val lowered = template.lowercase()
        when {
            StubGrokService.looksLikeTransfer(lowered) -> {
                _uiState.update {
                    it.copy(
                        phase = AskPhase.Unavailable,
                        navigateTransfer = StubGrokService.DEMO_TRANSFER_PREFILL,
                        standingSplitPrefill = null,
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = null,
                    )
                }
            }
            StubGrokService.looksLikeAddGoal(lowered) -> {
                _uiState.update {
                    it.copy(
                        phase = AskPhase.GoalForm,
                        formDraft = GoalFormDraft.fromProposal(
                            StubGrokService.VACATION_PROPOSAL,
                            now = clock(),
                        ),
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = null,
                    )
                }
            }
            StubGrokService.looksLikeChangeSplit(lowered) || lowered.contains("split") -> {
                standingSplitSeed.set(StubGrokService.DEMO_CHANGE_SPLIT)
                _uiState.update {
                    it.copy(
                        phase = AskPhase.Unavailable,
                        navigateStandingSplit = true,
                        standingSplitPrefill = StubGrokService.DEMO_CHANGE_SPLIT,
                        proposedAction = null,
                        plainAnswer = null,
                        statusMessage = null,
                    )
                }
            }
            else -> {
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
    /** Optional seed when opening Standing split from a ChangeSplit proposal (19b). */
    val standingSplitPrefill: List<com.piplanner.data.model.StandingSplit>? = null,
    val suggestionChips: List<String> = StubGrokService.ASK_CHIPS,
    val fallbackTemplates: List<String> = StubGrokService.FALLBACK_TEMPLATES,
)
