package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
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
    fun defaultInflation_isSevenPercent() {
        assertThat(GoalValidationService.DEFAULT_INFLATION_RATE).isEqualTo(0.07)
    }

    @Test
    fun adjustedTarget_updatesWithInflation() {
        val target = 10_000_000L // ₹1,00,000
        val at7 = validation.adjustedTargetPaisa(
            targetPaisa = target,
            inflationRate = 0.07,
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
        assertThat(at7).isGreaterThan(at0)
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
