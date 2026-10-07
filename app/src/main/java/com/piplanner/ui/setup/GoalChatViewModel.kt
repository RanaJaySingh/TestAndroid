package com.piplanner.ui.setup

import androidx.lifecycle.ViewModel
import com.piplanner.data.model.Goal
import com.piplanner.domain.FormattingService
import com.piplanner.domain.GoalProposal
import com.piplanner.domain.GoalValidationService
import com.piplanner.domain.GrokError
import com.piplanner.domain.GrokException
import com.piplanner.domain.GrokGoalAnalysis
import com.piplanner.domain.GrokService
import com.piplanner.domain.StubGrokService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/** UI phase for Goal chat / form (frames 5, 5a, 5b, 5c, 6). */
enum class GoalChatPhase {
    Chat,
    FollowUp,
    Proposal,
    Form,
    Unavailable,
    GoalsDefined,
}

/** Chat bubble for the Goal chat transcript. */
data class GoalChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
) {
    enum class Role { User, Assistant }
}

/**
 * View model for Goal chat → proposal / follow-up / form → goals defined (PRD R5).
 */
@HiltViewModel
class GoalChatViewModel @Inject constructor(
    private val grok: GrokService,
    private val formatting: FormattingService,
    private val validation: GoalValidationService,
) : ViewModel() {

    private val clock: () -> Instant = { Instant.now() }

    private val _uiState = MutableStateFlow(GoalChatUiState())
    val uiState: StateFlow<GoalChatUiState> = _uiState.asStateFlow()

    init {
        applyUnavailableIfNeeded()
    }

    /** Clears chat/form state when re-entering Goal chat from balance entry. */
    fun reset() {
        _uiState.value = GoalChatUiState()
        applyUnavailableIfNeeded()
    }

    fun setDraftInput(value: String) {
        _uiState.update { it.copy(draftInput = value) }
    }

    fun updateFormDraft(transform: (GoalFormDraft) -> GoalFormDraft) {
        _uiState.update { it.copy(formDraft = transform(it.formDraft)) }
    }

    fun setShowInflationPopup(show: Boolean) {
        _uiState.update { it.copy(showInflationPopup = show) }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(shouldNavigateToOpeningSplit = false) }
    }

    fun formatInr(paisa: Long): String = formatting.formatInrFromPaisa(paisa)

    fun displayPercent(fraction: Double): Int = validation.displayPercent(fraction)

    /** Exposed for Goal form / inflation popup live math. */
    fun validationService(): GoalValidationService = validation

    // MARK: - Chat actions

    fun sendDraft() {
        val text = _uiState.value.draftInput.trim()
        if (text.isEmpty()) return
        _uiState.update {
            it.copy(
                messages = it.messages + GoalChatMessage(role = GoalChatMessage.Role.User, text = text),
                draftInput = "",
                errorMessage = null,
            )
        }
        handleAnalysis(text)
    }

    fun useFormPath() {
        _uiState.update {
            it.copy(
                editingGoalId = null,
                formDraft = GoalFormDraft.blank(
                    remainingShare = remainingShareFraction(it.definedGoals),
                    now = clock(),
                ),
                phase = GoalChatPhase.Form,
            )
        }
    }

    fun confirmProposals() {
        val proposals = _uiState.value.proposals
        if (proposals.isEmpty()) return
        val goals = validation.goalsFromProposals(proposals, now = clock())
        _uiState.update {
            it.copy(
                definedGoals = goals,
                proposals = emptyList(),
                phase = GoalChatPhase.GoalsDefined,
                messages = it.messages + GoalChatMessage(
                    role = GoalChatMessage.Role.Assistant,
                    text = "Goals confirmed. Adjust shares if needed, then Continue when they total 100%.",
                ),
            )
        }
    }

    fun editProposals() {
        val first = _uiState.value.proposals.firstOrNull()
        if (first == null) {
            useFormPath()
            return
        }
        _uiState.update {
            it.copy(
                editingGoalId = first.id,
                formDraft = GoalFormDraft.fromProposal(first, now = clock()),
                phase = GoalChatPhase.Form,
            )
        }
    }

    fun editDefinedGoal(goal: Goal) {
        _uiState.update {
            it.copy(
                editingGoalId = goal.id,
                formDraft = GoalFormDraft.fromGoal(goal),
                phase = GoalChatPhase.Form,
            )
        }
    }

    fun cancelForm() {
        _uiState.update { state ->
            val nextPhase = when {
                state.definedGoals.isNotEmpty() -> GoalChatPhase.GoalsDefined
                state.proposals.isNotEmpty() -> GoalChatPhase.Proposal
                isGrokUnavailable() -> GoalChatPhase.Unavailable
                state.followUpCount >= GoalValidationService.MAX_FOLLOW_UPS ->
                    if (state.followUpCount > 0) GoalChatPhase.FollowUp else GoalChatPhase.Chat
                else -> GoalChatPhase.Chat
            }
            state.copy(
                formDraft = GoalFormDraft(),
                editingGoalId = null,
                phase = nextPhase,
            )
        }
    }

    /** @return true when the form saved successfully. */
    fun saveForm(): Boolean {
        val state = _uiState.value
        if (!state.formDraft.canSave(validation)) return false
        val goal = state.formDraft.makeGoal(
            id = state.editingGoalId ?: UUID.randomUUID().toString(),
            validation = validation,
            now = clock(),
        )
        val editingId = state.editingGoalId
        val nextDefined = when {
            editingId != null && state.definedGoals.any { it.id == editingId } -> {
                state.definedGoals.map { if (it.id == editingId) goal else it }
            }
            editingId != null && state.proposals.any { it.id == editingId } -> {
                val next = validation.goalsFromProposals(state.proposals, now = clock()).toMutableList()
                val idx = next.indexOfFirst { it.id == editingId }
                if (idx >= 0) next[idx] = goal else next.add(goal)
                next
            }
            else -> state.definedGoals + goal
        }
        _uiState.update {
            it.copy(
                definedGoals = nextDefined,
                proposals = emptyList(),
                editingGoalId = null,
                formDraft = GoalFormDraft(),
                phase = GoalChatPhase.GoalsDefined,
            )
        }
        return true
    }

    fun updateShare(goalId: String, displayPercent: Int) {
        val clamped = min(max(displayPercent, 0), 100)
        _uiState.update { state ->
            state.copy(
                definedGoals = state.definedGoals.map { goal ->
                    if (goal.id == goalId) {
                        goal.copy(shareOfNewCredits = clamped / 100.0)
                    } else {
                        goal
                    }
                },
            )
        }
    }

    fun continueToOpeningSplit() {
        if (!canContinue(_uiState.value.definedGoals)) return
        _uiState.update { it.copy(shouldNavigateToOpeningSplit = true) }
    }

    fun canSend(state: GoalChatUiState = _uiState.value): Boolean {
        return state.draftInput.trim().isNotEmpty() &&
            state.phase != GoalChatPhase.Form &&
            state.phase != GoalChatPhase.Unavailable
    }

    fun canContinue(goals: List<Goal> = _uiState.value.definedGoals): Boolean {
        return validation.canContinueWithDefinedGoals(goals)
    }

    fun continueDisabledReason(goals: List<Goal> = _uiState.value.definedGoals): String? {
        return validation.splitShortfallMessage(goals)
    }

    // MARK: - Private

    private fun applyUnavailableIfNeeded() {
        if (!isGrokUnavailable()) return
        _uiState.update {
            it.copy(
                phase = GoalChatPhase.Unavailable,
                messages = listOf(
                    GoalChatMessage(
                        role = GoalChatMessage.Role.Assistant,
                        text = "Grok is unavailable right now. You can still define goals with a form.",
                    ),
                ),
            )
        }
    }

    private fun isGrokUnavailable(): Boolean {
        val result = grok.analyzeGoalInput("car")
        val error = (result.exceptionOrNull() as? GrokException)?.error
        return error == GrokError.Unavailable
    }

    private fun handleAnalysis(text: String) {
        val result = grok.analyzeGoalInput(text)
        val failure = result.exceptionOrNull() as? GrokException
        if (failure != null) {
            if (failure.error == GrokError.Unavailable) {
                _uiState.update {
                    it.copy(
                        phase = GoalChatPhase.Unavailable,
                        messages = it.messages + GoalChatMessage(
                            role = GoalChatMessage.Role.Assistant,
                            text = "Grok is unavailable. Use a form to add your goals.",
                        ),
                    )
                }
            } else {
                presentFollowUpOrForm(StubGrokService.FOLLOW_UP_QUESTIONS[0])
            }
            return
        }
        when (val analysis = result.getOrNull()) {
            is GrokGoalAnalysis.Proposals -> {
                _uiState.update {
                    it.copy(
                        proposals = analysis.proposals,
                        phase = GoalChatPhase.Proposal,
                        messages = it.messages + GoalChatMessage(
                            role = GoalChatMessage.Role.Assistant,
                            text = "Here’s a suggested plan. Edit or Confirm when you’re ready.",
                        ),
                    )
                }
            }
            is GrokGoalAnalysis.NeedsClarification -> {
                presentFollowUpOrForm(analysis.question)
            }
            null -> presentFollowUpOrForm(StubGrokService.FOLLOW_UP_QUESTIONS[0])
        }
    }

    private fun presentFollowUpOrForm(preferredQuestion: String) {
        val state = _uiState.value
        if (state.followUpCount >= GoalValidationService.MAX_FOLLOW_UPS) {
            _uiState.update {
                it.copy(
                    messages = it.messages + GoalChatMessage(
                        role = GoalChatMessage.Role.Assistant,
                        text = "Let’s use the form so you can enter goals precisely.",
                    ),
                )
            }
            useFormPath()
            return
        }
        val question = StubGrokService.followUpQuestion(at = state.followUpCount)
        val prompt = if (state.followUpCount < StubGrokService.FOLLOW_UP_QUESTIONS.size) {
            question
        } else {
            preferredQuestion
        }
        _uiState.update {
            it.copy(
                followUpPrompt = prompt,
                followUpCount = it.followUpCount + 1,
                phase = GoalChatPhase.FollowUp,
                messages = it.messages + GoalChatMessage(
                    role = GoalChatMessage.Role.Assistant,
                    text = prompt,
                ),
            )
        }
    }

    private fun remainingShareFraction(definedGoals: List<Goal>): Double {
        val used = definedGoals.sumOf { it.shareOfNewCredits }
        val remaining = 1.0 - used
        return if (remaining > 0) remaining else 0.5
    }
}

data class GoalChatUiState(
    val phase: GoalChatPhase = GoalChatPhase.Chat,
    val messages: List<GoalChatMessage> = listOf(
        GoalChatMessage(
            role = GoalChatMessage.Role.Assistant,
            text = "Describe the goals you’re saving for. I’ll suggest a split you can edit or confirm.",
        ),
    ),
    val draftInput: String = "",
    val proposals: List<GoalProposal> = emptyList(),
    val definedGoals: List<Goal> = emptyList(),
    val followUpCount: Int = 0,
    val followUpPrompt: String? = null,
    val errorMessage: String? = null,
    val shouldNavigateToOpeningSplit: Boolean = false,
    val formDraft: GoalFormDraft = GoalFormDraft(),
    val showInflationPopup: Boolean = false,
    val editingGoalId: String? = null,
) {
    val checkedByLabel: String = GoalValidationService.CHECKED_BY_LABEL
}

/** Editable Goal form state (frame 6) with live inflation math. */
data class GoalFormDraft(
    val name: String = "",
    /** Digit-only rupee string for the target field. */
    val targetRupeeDigits: String = "",
    val startDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
    val endDate: LocalDate = LocalDate.now(ZoneOffset.UTC).plusYears(1),
    val inflationRate: Double = GoalValidationService.DEFAULT_INFLATION_RATE,
    /** Share of new credits 0.0–1.0 */
    val shareOfNewCredits: Double = 0.5,
    /** Create path locks saved at ₹0. */
    val savedAmount: Long = 0L,
) {
    fun targetPaisa(validation: GoalValidationService): Long =
        validation.paisaFromRupeeDigits(targetRupeeDigits)

    fun canSave(validation: GoalValidationService): Boolean =
        validation.canSave(
            name = name,
            targetPaisa = targetPaisa(validation),
            startDate = startDate,
            endDate = endDate,
        )

    fun adjustedTargetPaisa(validation: GoalValidationService): Long =
        validation.adjustedTargetPaisa(
            targetPaisa = targetPaisa(validation),
            inflationRate = inflationRate,
            startDate = startDate,
            endDate = endDate,
        )

    fun monthlyNeedPaisa(validation: GoalValidationService): Long =
        validation.monthlyNeedPaisa(
            adjustedTarget = adjustedTargetPaisa(validation),
            savedAmount = savedAmount,
            endDate = endDate,
        )

    fun inflationPercentDisplay(validation: GoalValidationService): Int =
        validation.displayPercent(inflationRate)

    fun sharePercentDisplay(validation: GoalValidationService): Int =
        validation.displayPercent(shareOfNewCredits)

    fun makeGoal(
        id: String,
        validation: GoalValidationService,
        now: Instant = Instant.now(),
    ): Goal {
        return validation.makeGoal(
            id = id,
            name = name,
            targetPaisa = targetPaisa(validation),
            startDate = startDate,
            endDate = endDate,
            inflationRate = inflationRate,
            shareOfNewCredits = shareOfNewCredits,
            savedAmount = savedAmount,
            now = now,
        )
    }

    companion object {
        fun blank(remainingShare: Double, now: Instant = Instant.now()): GoalFormDraft {
            val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
            return GoalFormDraft(
                startDate = start,
                endDate = start.plusYears(1),
                shareOfNewCredits = remainingShare,
                inflationRate = GoalValidationService.DEFAULT_INFLATION_RATE,
            )
        }

        fun fromProposal(proposal: GoalProposal, now: Instant = Instant.now()): GoalFormDraft {
            val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
            return GoalFormDraft(
                name = proposal.name,
                targetRupeeDigits = proposal.suggestedTarget?.let { (it / 100L).toString() }.orEmpty(),
                startDate = start,
                endDate = start.plusYears(1),
                inflationRate = GoalValidationService.DEFAULT_INFLATION_RATE,
                shareOfNewCredits = proposal.sharePercentage,
                savedAmount = 0L,
            )
        }

        fun fromGoal(goal: Goal): GoalFormDraft {
            return GoalFormDraft(
                name = goal.name,
                targetRupeeDigits = (goal.targetAmount / 100L).toString(),
                startDate = LocalDate.parse(goal.startDate),
                endDate = LocalDate.parse(goal.endDate),
                inflationRate = goal.inflationRate,
                shareOfNewCredits = goal.shareOfNewCredits,
                savedAmount = goal.savedAmount,
            )
        }
    }
}
