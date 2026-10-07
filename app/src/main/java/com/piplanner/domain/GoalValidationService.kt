package com.piplanner.domain

import com.piplanner.data.model.Goal
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

/** Spec §3.4 — ValidationErrorCode. */
enum class ValidationErrorCode {
    EmptyName,
    ZeroTarget,
    EndNotAfterStart,
    SplitNotHundred,
    AmountExceedsSaved,
    GoalBelowZero,
    NoDedicatedAccount,
}

/** Spec §3.4 — ValidationError. */
data class ValidationError(
    val field: String,
    val message: String,
    val code: ValidationErrorCode,
)

/**
 * Pure validation + inflation helpers for Goal form / chat (PRD R5, Spec §3.4).
 */
@Singleton
class GoalValidationService @Inject constructor(
    private val openingSplitService: OpeningSplitService,
) {

    fun isNameValid(name: String): Boolean = name.trim().isNotEmpty()

    fun isTargetValid(targetPaisa: Long): Boolean = targetPaisa > 0L

    fun areDatesValid(start: LocalDate, end: LocalDate): Boolean = end.isAfter(start)

    /** Save enabled only when name non-empty, target > ₹0, and end after start. */
    fun canSave(
        name: String,
        targetPaisa: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Boolean {
        return isNameValid(name) &&
            isTargetValid(targetPaisa) &&
            areDatesValid(startDate, endDate)
    }

    /** Spec §3.4 ValidationError list for invalid form drafts. */
    fun validationErrors(
        name: String,
        targetPaisa: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        if (!isNameValid(name)) {
            errors += ValidationError(
                field = "name",
                message = "Name is required.",
                code = ValidationErrorCode.EmptyName,
            )
        }
        if (!isTargetValid(targetPaisa)) {
            errors += ValidationError(
                field = "targetAmount",
                message = "Target must be greater than ₹0.",
                code = ValidationErrorCode.ZeroTarget,
            )
        }
        if (!areDatesValid(startDate, endDate)) {
            errors += ValidationError(
                field = "endDate",
                message = "End date must be after start date.",
                code = ValidationErrorCode.EndNotAfterStart,
            )
        }
        return errors
    }

    /** Continue into Opening split when defined goals’ shares sum to 100%. */
    fun canContinueWithDefinedGoals(goals: List<Goal>): Boolean {
        if (goals.isEmpty()) return false
        val fractions = goals.map { BigDecimal.valueOf(it.shareOfNewCredits) }
        return openingSplitService.isValidHundredPercent(fractions)
    }

    fun splitShortfallMessage(goals: List<Goal>): String? {
        val fractions = goals.map { BigDecimal.valueOf(it.shareOfNewCredits) }
        return openingSplitService.shortfallMessage(fractions)
    }

    /** `targetAmount * (1 + inflationRate)^years` — same rule as [Goal.adjustedTarget]. */
    fun adjustedTargetPaisa(
        targetPaisa: Long,
        inflationRate: Double,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long {
        val days = ChronoUnit.DAYS.between(startDate, endDate).toDouble()
        val years = max(0.0, days / DAYS_PER_YEAR)
        return round(targetPaisa * (1.0 + inflationRate).pow(years)).toLong()
    }

    /** `(adjustedTarget - savedAmount) / monthsRemaining` */
    fun monthlyNeedPaisa(
        adjustedTarget: Long,
        savedAmount: Long,
        endDate: LocalDate,
        asOf: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): Long {
        val remaining = max(0L, adjustedTarget - savedAmount)
        if (remaining <= 0L) return 0L
        val months = max(
            1.0,
            ChronoUnit.DAYS.between(asOf, endDate).toDouble() / DAYS_PER_MONTH,
        )
        return ceil(remaining / months).toLong()
    }

    /** Parses whole-rupee digit string into paisa. Non-digits ignored; empty → 0. */
    fun paisaFromRupeeDigits(digits: String): Long {
        return ConsentService.paisaFromRupeeDigits(digits)
    }

    /** Display percent 0…100 from a 0.0–1.0 fraction. */
    fun displayPercent(fromFraction: Double): Int {
        return round(fromFraction * 100.0).toInt()
    }

    /** Builds a Goal from validated form fields (saved starts at ₹0 in create). */
    fun makeGoal(
        id: String = UUID.randomUUID().toString(),
        name: String,
        targetPaisa: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        inflationRate: Double = DEFAULT_INFLATION_RATE,
        shareOfNewCredits: Double,
        savedAmount: Long = 0L,
        now: Instant = Instant.now(),
    ): Goal {
        val iso = now.toString()
        return Goal(
            id = id,
            name = name.trim(),
            targetAmount = targetPaisa,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            inflationRate = inflationRate,
            savedAmount = savedAmount,
            shareOfNewCredits = shareOfNewCredits,
            createdAt = iso,
            updatedAt = iso,
        )
    }

    /** Converts confirmed proposals into Goals with default dates / inflation. */
    fun goalsFromProposals(
        proposals: List<GoalProposal>,
        now: Instant = Instant.now(),
    ): List<Goal> {
        val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val end = start.plusYears(1)
        return proposals.map { proposal ->
            makeGoal(
                id = proposal.id,
                name = proposal.name,
                targetPaisa = proposal.suggestedTarget ?: 1_000_000L,
                startDate = start,
                endDate = end,
                inflationRate = DEFAULT_INFLATION_RATE,
                shareOfNewCredits = proposal.sharePercentage,
                savedAmount = 0L,
                now = now,
            )
        }
    }

    companion object {
        /** Default inflation rate — 5% (PIP-110; was 7%). */
        const val DEFAULT_INFLATION_RATE: Double = Goal.DEFAULT_INFLATION_RATE

        /** Inclusive percent bounds for the Inflation sheet typed rate. */
        const val MIN_INFLATION_PERCENT: Int = 0
        const val MAX_INFLATION_PERCENT: Int = 30

        /**
         * Parses a typed whole-percent string (e.g. `"5"`) into a 0.0–1.0 rate.
         * Returns null when empty, non-numeric, non-integer, or outside
         * [MIN_INFLATION_PERCENT]…[MAX_INFLATION_PERCENT].
         */
        fun parseInflationPercentInput(raw: String): Double? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val percent = trimmed.toIntOrNull() ?: return null
            if (percent < MIN_INFLATION_PERCENT || percent > MAX_INFLATION_PERCENT) return null
            return percent / 100.0
        }

        /** Whole-percent display string for [rate] (0.0–1.0). */
        fun inflationPercentText(rate: Double): String =
            displayPercentStatic(rate).toString()

        private fun displayPercentStatic(fromFraction: Double): Int =
            round(fromFraction * 100.0).toInt()

        /** Proposal / defined-goals footer label (frame 5b). */
        const val CHECKED_BY_LABEL: String = StubGrokService.CHECKED_BY_LABEL

        /** Max vague-input follow-ups before forcing the form (PRD R5 / frame 5a). */
        const val MAX_FOLLOW_UPS: Int = 2

        private const val DAYS_PER_YEAR: Double = 365.25
        private const val DAYS_PER_MONTH: Double = 30.4375
    }
}
