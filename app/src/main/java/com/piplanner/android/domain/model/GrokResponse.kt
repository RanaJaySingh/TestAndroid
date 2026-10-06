package com.piplanner.android.domain.model

data class GrokResponse(
    val type: GrokResponseType,
    val message: String? = null,
    val proposedGoals: List<ProposedGoal>? = null,
    val transferSuggestion: TransferSuggestion? = null
)

enum class GrokResponseType {
    GOALS_PROPOSAL,
    TRANSFER_PROPOSAL,
    PLAIN_ANSWER,
    FOLLOW_UP_QUESTION,
    UNAVAILABLE
}

data class ProposedGoal(
    val name: String,
    val targetAmount: Long,
    val endDate: String,
    val suggestedShare: Int
)

data class TransferSuggestion(
    val fromGoalId: String,
    val toGoalId: String,
    val amount: Long,
    val reason: String
)
