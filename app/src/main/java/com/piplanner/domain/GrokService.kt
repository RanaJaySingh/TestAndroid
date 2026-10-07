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

/** Spec §3.3 — ProposedAction (Ask tab; stubbed for contract completeness). */
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
 * Spec §3.3 — GrokService (stub).
 * Real Grok/AI integration is out of scope (PIP-42).
 */
interface GrokService {
    /** Spec: stubbed goal proposals based on input text. */
    fun analyzeGoals(input: String): Result<List<GoalProposal>>

    /** Goal-chat helper: proposals or a clarification follow-up (5a). */
    fun analyzeGoalInput(input: String): Result<GrokGoalAnalysis>

    /** Spec: stubbed answers for Ask queries. */
    fun askQuestion(query: String): Result<AskResponse>
}

/**
 * Deterministic Grok stub for demo + unit tests (PRD R5 / R19, Spec §3.3 / §5.3).
 */
class StubGrokService(
    /** When true, all analyze / ask calls fail with [GrokError.Unavailable] (frame 5c). */
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
        return Result.success(
            AskResponse.PlainAnswer(
                text = "Your plan assigns every rupee to a named goal. " +
                    "Ask again after setup for live numbers.",
            ),
        )
    }

    companion object {
        /** Design / demo happy-path proposal: Car 60%, Emergency Fund 40%. */
        val HAPPY_PATH_PROPOSALS: List<GoalProposal> = listOf(
            GoalProposal(
                id = DemoData.DEMO_CAR_GOAL_ID,
                name = "Car",
                sharePercentage = 0.6,
                suggestedTarget = 50_000_000L,
            ),
            GoalProposal(
                id = DemoData.DEMO_EMERGENCY_GOAL_ID,
                name = "Emergency Fund",
                sharePercentage = 0.4,
                suggestedTarget = 20_000_000L,
            ),
        )

        const val CHECKED_BY_LABEL: String = "Checked by PiPlanner. Estimate."

        val FOLLOW_UP_QUESTIONS: List<String> = listOf(
            "What are you saving for? For example: a car, emergency fund, or a trip.",
            "Roughly how much do you want for each goal, and what share of new savings should each get?",
        )

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
    }
}
