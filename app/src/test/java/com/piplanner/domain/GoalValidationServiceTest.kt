package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.Goal
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GoalValidationServiceTest {

    private val validation = GoalValidationService(OpeningSplitService())
    private val start: LocalDate = LocalDate.ofInstant(
        Instant.ofEpochSecond(1_700_000_000L),
        ZoneOffset.UTC,
    )
    private val end: LocalDate = start.plusYears(1)
    private val now: Instant = Instant.ofEpochSecond(1_700_000_000L)

    @Test
    fun saveDisabled_forEmptyNameZeroTargetInvalidDates() {
        assertThat(
            validation.canSave(
                name = "   ",
                targetPaisa = 100_000L,
                startDate = start,
                endDate = end,
            ),
        ).isFalse()
        assertThat(
            validation.canSave(
                name = "Car",
                targetPaisa = 0L,
                startDate = start,
                endDate = end,
            ),
        ).isFalse()
        assertThat(
            validation.canSave(
                name = "Car",
                targetPaisa = 100_000L,
                startDate = end,
                endDate = start,
            ),
        ).isFalse()
        assertThat(
            validation.canSave(
                name = "Car",
                targetPaisa = 100_000L,
                startDate = start,
                endDate = end,
            ),
        ).isTrue()
    }

    @Test
    fun validationErrorCodes() {
        val errors = validation.validationErrors(
            name = "",
            targetPaisa = 0L,
            startDate = end,
            endDate = start,
        )
        assertThat(errors.map { it.code }).containsExactly(
            ValidationErrorCode.EmptyName,
            ValidationErrorCode.ZeroTarget,
            ValidationErrorCode.EndNotAfterStart,
        )
    }

    @Test
    fun defaultInflation_isFivePercent() {
        assertThat(GoalValidationService.DEFAULT_INFLATION_RATE).isEqualTo(0.05)
        assertThat(Goal.DEFAULT_INFLATION_RATE).isEqualTo(0.05)
    }

    @Test
    fun adjustedTarget_updatesWithInflation() {
        val target = 10_000_000L // ₹1,00,000
        val at5 = validation.adjustedTargetPaisa(
            targetPaisa = target,
            inflationRate = 0.05,
            startDate = start,
            endDate = end,
        )
        val at0 = validation.adjustedTargetPaisa(
            targetPaisa = target,
            inflationRate = 0.0,
            startDate = start,
            endDate = end,
        )
        assertThat(at0).isEqualTo(target)
        assertThat(at5).isGreaterThan(at0)
    }

    @Test
    fun parseInflationPercentInput_acceptsBoundsAndRejectsInvalid() {
        assertThat(GoalValidationService.parseInflationPercentInput("5")).isEqualTo(0.05)
        assertThat(GoalValidationService.parseInflationPercentInput("0")).isEqualTo(0.0)
        assertThat(GoalValidationService.parseInflationPercentInput("30")).isEqualTo(0.30)
        assertThat(GoalValidationService.parseInflationPercentInput("")).isNull()
        assertThat(GoalValidationService.parseInflationPercentInput("abc")).isNull()
        assertThat(GoalValidationService.parseInflationPercentInput("5.5")).isNull()
        assertThat(GoalValidationService.parseInflationPercentInput("-1")).isNull()
        assertThat(GoalValidationService.parseInflationPercentInput("31")).isNull()
    }

    @Test
    fun continueRequiresHundredPercentShares() {
        val goals = validation.goalsFromProposals(StubGrokService.HAPPY_PATH_PROPOSALS, now = now)
        assertThat(validation.canContinueWithDefinedGoals(goals)).isTrue()
        assertThat(validation.splitShortfallMessage(goals)).isNull()

        val broken = goals.toMutableList().also {
            it[0] = it[0].copy(shareOfNewCredits = 0.5)
        }
        assertThat(validation.canContinueWithDefinedGoals(broken)).isFalse()
        assertThat(validation.splitShortfallMessage(broken)).isNotNull()
        assertThat(validation.canContinueWithDefinedGoals(emptyList())).isFalse()
    }

    @Test
    fun maxFollowUps_isTwo() {
        assertThat(GoalValidationService.MAX_FOLLOW_UPS).isEqualTo(2)
    }

    @Test
    fun goalsFromProposals_useDefaultInflationAndZeroSaved() {
        val goals = validation.goalsFromProposals(StubGrokService.HAPPY_PATH_PROPOSALS, now = now)
        assertThat(goals).hasSize(2)
        assertThat(goals[0].inflationRate).isEqualTo(GoalValidationService.DEFAULT_INFLATION_RATE)
        assertThat(goals[0].savedAmount).isEqualTo(0L)
        assertThat(goals[0].shareOfNewCredits).isEqualTo(0.6)
        assertThat(goals[1].shareOfNewCredits).isEqualTo(0.4)
    }

    @Test
    fun paisaFromRupeeDigits() {
        assertThat(validation.paisaFromRupeeDigits("")).isEqualTo(0L)
        assertThat(validation.paisaFromRupeeDigits("100000")).isEqualTo(10_000_000L)
        assertThat(validation.paisaFromRupeeDigits("12a3")).isEqualTo(1_2300L)
    }
}
