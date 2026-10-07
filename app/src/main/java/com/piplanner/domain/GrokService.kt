package com.piplanner.domain

import com.piplanner.data.model.StandingSplit
import com.piplanner.util.DemoData

/**
 * Spec §3.3 — GoalProposal.
 * [sharePercentage] is 0.0–1.0; [suggestedTarget] is Long paisa.
 */
data class GoalProposal(
    val id: String,
    val name: String,
    val sharePercentage: Double,
    val suggestedTarget: Long? = null,
)

/** Spec §3.3 — ProposedAction (Ask tab). */
sealed class ProposedAction {
    data class AddGoal(val proposal: GoalProposal) : ProposedAction()
    data class Transfer(val from: String, val to: String, val amount: Long) : ProposedAction()
    data class ChangeSplit(val newSplit: List<StandingSplit>) : ProposedAction()
}

/** Spec §3.3 — AskResponse. */
sealed class AskResponse {
    data class PlainAnswer(val text: String) : AskResponse()
    data class ActionProposal(val action: ProposedAction) : AskResponse()
}

/** Richer analysis outcome used by Goal chat (frames 5 / 5a / 5b). */
sealed class GrokGoalAnalysis {
    data class Proposals(val proposals: List<GoalProposal>) : GrokGoalAnalysis()
    data class NeedsClarification(val question: String) : GrokGoalAnalysis()
}

/** Spec §3.3 — GrokError. */
enum class GrokError {
    Unavailable,
    InvalidDraft,
    RateLimited,
}

class GrokException(val error: GrokError) : Exception(error.name)

/**
 * Prefill payload for Transfer opened from an Ask proposal (frame 16c).
 */
data class TransferAskPrefill(
    val fromGoalId: String,
    val toGoalId: String,
    val amountPaisa: Long,
)

/**
 * Spec §3.3 — GrokService (stub).
 * Real Grok/AI integration is out of scope (PIP-64).
 */
interface GrokService {
    /** Spec: stubbed goal proposals based on input text. */
    fun analyzeGoals(input: String): Result<List<GoalProposal>>

    /** Goal-chat helper: proposals or a clarification follow-up (5a). */
    fun analyzeGoalInput(input: String): Result<GrokGoalAnalysis>

    /** Spec: stubbed answers for Ask queries (R18 / R19). */
    fun askQuestion(query: String): Result<AskResponse>
}

/**
 * Deterministic Grok stub for demo + unit tests (PRD R5 / R18 / R19, Spec §3.3 / §5.3).
 */
class StubGrokService(
    /** When true, all analyze / ask calls fail with [GrokError.Unavailable] (frames 5c / 19c). */
    val isUnavailable: Boolean = false,
) : GrokService {

    override fun analyzeGoals(input: String): Result<List<GoalProposal>> {
        val result = analyzeGoalInput(input)
        val analysis = result.getOrElse { return Result.failure(it) }
        return when (analysis) {
            is GrokGoalAnalysis.Proposals -> Result.success(analysis.proposals)
            is GrokGoalAnalysis.NeedsClarification ->
                Result.failure(GrokException(GrokError.InvalidDraft))
        }
    }

    override fun analyzeGoalInput(input: String): Result<GrokGoalAnalysis> {
        if (isUnavailable) {
            return Result.failure(GrokException(GrokError.Unavailable))
        }
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return Result.success(
                GrokGoalAnalysis.NeedsClarification(question = FOLLOW_UP_QUESTIONS[0]),
            )
        }
        if (isVague(trimmed)) {
            return Result.success(
                GrokGoalAnalysis.NeedsClarification(question = FOLLOW_UP_QUESTIONS[0]),
            )
        }
        return Result.success(GrokGoalAnalysis.Proposals(HAPPY_PATH_PROPOSALS))
    }

    override fun askQuestion(query: String): Result<AskResponse> {
        if (isUnavailable) {
            return Result.failure(GrokException(GrokError.Unavailable))
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(GrokException(GrokError.InvalidDraft))
        }
        val lowered = trimmed.lowercase()

        // Frame 19d — invalid draft triggers (never returned as a displayable proposal).
        if (isInvalidDraftQuery(lowered)) {
            return Result.failure(GrokException(GrokError.InvalidDraft))
        }

        // Frame 19a — questions / chips that change nothing stay plain answers.
        if (isPlainQuestion(lowered, trimmed)) {
            return Result.success(
                AskResponse.PlainAnswer(text = DEFAULT_PLAIN_ANSWER),
            )
        }

        // Frame 16c / 19b — Transfer action sentence.
        if (looksLikeTransfer(lowered)) {
            val prefill = DEMO_TRANSFER_PREFILL
            return Result.success(
                AskResponse.ActionProposal(
                    action = ProposedAction.Transfer(
                        from = prefill.fromGoalId,
                        to = prefill.toGoalId,
                        amount = prefill.amountPaisa,
                    ),
                ),
            )
        }

        // Frame 13f / 19b — Add goal action sentence.
        if (looksLikeAddGoal(lowered)) {
            return Result.success(
                AskResponse.ActionProposal(
                    action = ProposedAction.AddGoal(proposal = VACATION_PROPOSAL),
                ),
            )
        }

        // Frame 19b — Change standing split action sentence.
        if (looksLikeChangeSplit(lowered)) {
            return Result.success(
                AskResponse.ActionProposal(
                    action = ProposedAction.ChangeSplit(newSplit = DEMO_CHANGE_SPLIT),
                ),
            )
        }

        // Frame 19a — plain-text answer (engine numbers filled by AskViewModel when goals exist).
        return Result.success(
            AskResponse.PlainAnswer(text = DEFAULT_PLAIN_ANSWER),
        )
    }

    companion object {
        /**
         * Design / demo happy-path proposal (PIP-66 / A7): Car 60%, Emergency Fund 40%.
         * Names and targets match [DemoData] persona seeding.
         */
        val HAPPY_PATH_PROPOSALS: List<GoalProposal> = listOf(
            GoalProposal(
                id = DemoData.DEMO_CAR_GOAL_ID,
                name = DemoData.HAPPY_PATH_CAR_NAME,
                sharePercentage = 0.6,
                suggestedTarget = 50_000_000L,
            ),
            GoalProposal(
                id = DemoData.DEMO_EMERGENCY_GOAL_ID,
                name = DemoData.HAPPY_PATH_EMERGENCY_NAME,
                sharePercentage = 0.4,
                suggestedTarget = 20_000_000L,
            ),
        )

        /** Frame 13f — Vacation goal draft from Ask. */
        val VACATION_PROPOSAL: GoalProposal = GoalProposal(
            id = "cccccccc-cccc-cccc-cccc-cccccccccccc",
            name = "Vacation",
            sharePercentage = 0.2,
            suggestedTarget = 5_000_000L,
        )

        /**
         * Demo Transfer proposal (frame 16c): Car → Emergency Fund · ₹5,000.
         * Matches iOS StubGrokService / PIP-56 Ask prefill contract.
         */
        val DEMO_TRANSFER_PREFILL: TransferAskPrefill = TransferAskPrefill(
            fromGoalId = DemoData.DEMO_CAR_GOAL_ID,
            toGoalId = DemoData.DEMO_EMERGENCY_GOAL_ID,
            amountPaisa = 500_000L,
        )

        /** Stub standing-split flip for Ask change-split proposals. */
        val DEMO_CHANGE_SPLIT: List<StandingSplit> = listOf(
            StandingSplit(goalId = DemoData.DEMO_CAR_GOAL_ID, percentage = 0.5),
            StandingSplit(goalId = DemoData.DEMO_EMERGENCY_GOAL_ID, percentage = 0.5),
        )

        const val CHECKED_BY_LABEL: String = "Checked by PiPlanner. Estimate."

        const val DEFAULT_PLAIN_ANSWER: String =
            "Your plan assigns every rupee to a named goal. " +
                "Ask again after setup for live numbers."

        /** Design frame 19 suggestion chips. */
        val ASK_CHIPS: List<String> = listOf(
            "What happens if I change the split?",
            "Why is inflation 5%?",
        )

        /** Frame 19c template sentences when Grok is unavailable (iOS AskService parity). */
        val FALLBACK_TEMPLATES: List<String> = listOf(
            "Transfer ₹5,000 from Car to Emergency Fund",
            "Add a ₹50,000 vacation by March",
            "Change the standing split",
        )

        val FOLLOW_UP_QUESTIONS: List<String> = listOf(
            "What are you saving for? For example: a car, emergency fund, or a trip.",
            "Roughly how much do you want for each goal, and what share of new savings should each get?",
        )

        const val INVALID_DRAFT_RETRY_PROMPT: String =
            "I couldn't turn that into a safe draft. Try once more with a clearer action, " +
                "or use the Goal form."

        private val CONCRETE_TOKENS: List<String> = listOf(
            "car", "emergency", "fund", "house", "home", "trip", "vacation", "wedding",
        )

        private val FILLER_ONLY: Set<String> = setOf(
            "goals", "goal", "save", "savings", "money", "something", "idk",
            "help", "plan", "please", "stuff", "things",
        )

        /** Vague when short / filler-only, or lacks concrete goal nouns from the demo set. */
        fun isVague(input: String): Boolean {
            val lowered = input.lowercase()
            if (CONCRETE_TOKENS.any { lowered.contains(it) }) {
                return false
            }
            val tokens = lowered
                .split(Regex("[^a-z]+"))
                .filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return true
            if (tokens.size <= 4 && tokens.all { it in FILLER_ONLY }) {
                return true
            }
            return CONCRETE_TOKENS.none { lowered.contains(it) }
        }

        /** Follow-up copy for the n-th clarification (0-based), cycling the stub list. */
        fun followUpQuestion(at: Int): String {
            val safe = maxOf(at, 0) % FOLLOW_UP_QUESTIONS.size
            return FOLLOW_UP_QUESTIONS[safe]
        }

        /** Maps a Transfer ProposedAction to [TransferAskPrefill] (Ask → Transfer). */
        fun transferPrefill(from: ProposedAction): TransferAskPrefill? {
            val action = from as? ProposedAction.Transfer ?: return null
            return TransferAskPrefill(
                fromGoalId = action.from,
                toGoalId = action.to,
                amountPaisa = action.amount,
            )
        }

        /** True for informational chips / questions that must not become proposal cards. */
        fun isPlainQuestion(lowered: String, original: String): Boolean {
            if (original.contains('?')) return true
            if (ASK_CHIPS.any { it.equals(original.trim(), ignoreCase = true) }) return true
            val starters = listOf("what ", "why ", "how ", "am i", "is my", "can i", "does ")
            return starters.any { lowered.startsWith(it) }
        }

        fun looksLikeTransfer(lowered: String): Boolean =
            lowered.contains("transfer") ||
                lowered.contains("move ") ||
                lowered.startsWith("move") ||
                (lowered.contains("from") && lowered.contains("to") &&
                    (lowered.contains("₹") || lowered.contains("rs") || lowered.contains("rupee")))

        fun looksLikeAddGoal(lowered: String): Boolean =
            (lowered.contains("add") &&
                (lowered.contains("goal") || lowered.contains("vacation") ||
                    lowered.contains("trip") || lowered.contains("fund"))) ||
                (lowered.contains("vacation") &&
                    (lowered.contains("add") || lowered.contains("create") ||
                        lowered.contains("new")))

        fun looksLikeChangeSplit(lowered: String): Boolean =
            lowered.contains("standing split") ||
                (lowered.contains("change") && lowered.contains("split") &&
                    !lowered.startsWith("what") && !lowered.contains("happen")) ||
                (lowered.contains("split") &&
                    (lowered.contains("50/50") || lowered.contains("50 %") ||
                        lowered.contains("to 50")))

        /**
         * Queries that intentionally fail draft validation (19d).
         * Includes nonsense action shapes the stub refuses to show.
         */
        fun isInvalidDraftQuery(lowered: String): Boolean {
            if (lowered.contains("invalid draft") || lowered.contains("bad draft")) {
                return true
            }
            // Action-shaped but missing required pieces → invalid, never shown as a card.
            if (lowered.contains("transfer") && !lowered.contains("to") && !lowered.contains("from")) {
                if (lowered.length < 12) return true
            }
            if (lowered == "do something" || lowered == "fix it" || lowered == "make a draft") {
                return true
            }
            return false
        }
    }
}
