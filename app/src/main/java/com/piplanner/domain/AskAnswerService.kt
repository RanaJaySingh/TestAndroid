package com.piplanner.domain

import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalStatus
import com.piplanner.data.model.StandingSplit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds Ask plain-text answers from live engine numbers (PRD R18 / frame 19a).
 */
@Singleton
class AskAnswerService @Inject constructor(
    private val formatting: FormattingService,
    private val goalsTabService: GoalsTabService,
) {

    fun plainAnswer(
        query: String,
        goals: List<Goal>,
        standingSplits: List<StandingSplit>,
        totalSavingsPaisa: Long,
    ): String {
        if (goals.isEmpty()) {
            return StubGrokService.DEFAULT_PLAIN_ANSWER
        }
        val lowered = query.lowercase()
        return when {
            lowered.contains("inflation") -> inflationAnswer(goals)
            lowered.contains("split") -> splitAnswer(goals, standingSplits)
            else -> overviewAnswer(goals, totalSavingsPaisa)
        }
    }

    fun proposalSummary(
        action: ProposedAction,
        goals: List<Goal>,
    ): String = when (action) {
        is ProposedAction.Transfer -> {
            val from = goals.firstOrNull { it.id == action.from }?.name ?: "From"
            val to = goals.firstOrNull { it.id == action.to }?.name ?: "To"
            "$from → $to · ${formatting.formatInrFromPaisa(action.amount)}"
        }
        is ProposedAction.AddGoal -> {
            val target = action.proposal.suggestedTarget?.let { formatting.formatInrFromPaisa(it) }
                ?: "—"
            "New goal ${action.proposal.name} · $target · " +
                "${displayPercent(action.proposal.sharePercentage)}% of new credits"
        }
        is ProposedAction.ChangeSplit -> {
            action.newSplit.joinToString(separator = " · ") { split ->
                val name = goals.firstOrNull { it.id == split.goalId }?.name ?: "Goal"
                "$name ${displayPercent(split.percentage)}%"
            }
        }
    }

    fun proposalTitle(action: ProposedAction): String = when (action) {
        is ProposedAction.Transfer -> "Suggested transfer"
        is ProposedAction.AddGoal -> "Suggested goal"
        is ProposedAction.ChangeSplit -> "Suggested standing split"
    }

    private fun overviewAnswer(goals: List<Goal>, totalSavingsPaisa: Long): String {
        val lines = mutableListOf(
            "Total savings ${formatting.formatInrFromPaisa(totalSavingsPaisa)} across ${goals.size} goals.",
        )
        goals.forEach { goal ->
            val status = goal.status()
            val statusText = when (status) {
                is GoalStatus.OnTrack -> goalsTabService.statusLabel(status)
                is GoalStatus.Behind ->
                    "${goalsTabService.statusLabel(status)} " +
                        formatting.formatInrFromPaisa(status.shortfall)
            }
            lines += "${goal.name}: saved ${formatting.formatInrFromPaisa(goal.savedAmount)} of " +
                "${formatting.formatInrFromPaisa(goal.adjustedTarget())}. " +
                "Needs ${formatting.formatInrFromPaisa(goal.monthlyNeed())}/month. $statusText."
        }
        return lines.joinToString(separator = "\n")
    }

    private fun inflationAnswer(goals: List<Goal>): String {
        val first = goals.first()
        val ratePct = displayPercent(first.inflationRate)
        return "PiPlanner defaults inflation to $ratePct% as a cautious estimate, not a guarantee. " +
            goals.joinToString(separator = " ") { goal ->
                "${goal.name}'s ${formatting.formatInrFromPaisa(goal.targetAmount)} becomes " +
                    "${formatting.formatInrFromPaisa(goal.adjustedTarget())} by ${goal.endDate}."
            }
    }

    private fun splitAnswer(
        goals: List<Goal>,
        standingSplits: List<StandingSplit>,
    ): String {
        val parts = if (standingSplits.isNotEmpty()) {
            standingSplits.map { split ->
                val name = goals.firstOrNull { it.id == split.goalId }?.name ?: "Goal"
                "$name ${displayPercent(split.percentage)}%"
            }
        } else {
            goals.map { "${it.name} ${displayPercent(it.shareOfNewCredits)}%" }
        }
        return "Changing the standing split only affects the next credit. " +
            "Saved money stays put. Current share: ${parts.joinToString(" · ")}. " +
            "Splits must still total 100%."
    }

    private fun displayPercent(fraction: Double): Int =
        kotlin.math.round(fraction * 100.0).toInt()
}
