package com.piplanner.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

/**
 * Shared contract — Spec §3.1 Goal.
 * [targetAmount] / [savedAmount] are Long paisa.
 * [inflationRate] defaults to 0.07; [shareOfNewCredits] is 0.0–1.0.
 * Dates: ISO-8601 LocalDate (`yyyy-MM-dd`); timestamps: ISO-8601 Instant.
 */
@Serializable
data class Goal(
    val id: String,
    val name: String,
    val targetAmount: Long,
    val startDate: String,
    val endDate: String,
    val inflationRate: Double = DEFAULT_INFLATION_RATE,
    val savedAmount: Long,
    val shareOfNewCredits: Double,
    val createdAt: String,
    val updatedAt: String,
) {
    @Transient
    private val startLocalDate: LocalDate = LocalDate.parse(startDate)

    @Transient
    private val endLocalDate: LocalDate = LocalDate.parse(endDate)

    /** Spec computed: targetAmount * (1 + inflationRate)^years */
    fun adjustedTarget(): Long {
        val days = ChronoUnit.DAYS.between(startLocalDate, endLocalDate).toDouble()
        val years = max(0.0, days / DAYS_PER_YEAR)
        return round(targetAmount * (1.0 + inflationRate).pow(years)).toLong()
    }

    /** Spec computed: (adjustedTarget - savedAmount) / monthsRemaining */
    fun monthlyNeed(now: Instant = Instant.now()): Long {
        val remaining = adjustedTarget() - savedAmount
        if (remaining <= 0L) return 0L
        val today = now.atZone(ZoneOffset.UTC).toLocalDate()
        val months = max(
            1.0,
            ChronoUnit.DAYS.between(today, endLocalDate).toDouble() / DAYS_PER_MONTH,
        )
        return ceil(remaining / months).toLong()
    }

    /** Spec computed: OnTrack | Behind(shortfall) */
    fun status(now: Instant = Instant.now()): GoalStatus {
        val need = monthlyNeed(now)
        val today = now.atZone(ZoneOffset.UTC).toLocalDate()
        val remainingMonths = max(
            1.0,
            ChronoUnit.DAYS.between(today, endLocalDate).toDouble() / DAYS_PER_MONTH,
        )
        val projected = savedAmount + round(need * remainingMonths).toLong()
        val shortfall = adjustedTarget() - projected
        return if (shortfall <= 0L) {
            GoalStatus.OnTrack
        } else {
            GoalStatus.Behind(shortfall = shortfall)
        }
    }

    companion object {
        const val DEFAULT_INFLATION_RATE: Double = 0.07
        private const val DAYS_PER_YEAR: Double = 365.25
        private const val DAYS_PER_MONTH: Double = 30.4375
    }
}
