package com.piplanner.android.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.pow

object CalculationUtils {
    const val DEFAULT_INFLATION_RATE = 7.0
    
    fun calculateInflationAdjustedTarget(
        baseAmount: Long,
        inflationRate: Double,
        startDate: LocalDate,
        endDate: LocalDate
    ): Long {
        val years = ChronoUnit.DAYS.between(startDate, endDate) / 365.0
        if (years <= 0) return baseAmount
        
        val inflatedAmount = baseAmount * (1 + inflationRate / 100).pow(years)
        return ceil(inflatedAmount).toLong()
    }
    
    fun calculateMonthlyNeeded(
        targetAmount: Long,
        savedSoFar: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Long {
        val remaining = targetAmount - savedSoFar
        if (remaining <= 0) return 0
        
        val months = ChronoUnit.MONTHS.between(startDate, endDate)
        if (months <= 0) return remaining
        
        return ceil(remaining.toDouble() / months).toLong()
    }
    
    fun calculateProgress(saved: Long, target: Long): Float {
        if (target <= 0) return 0f
        return (saved.toDouble() / target).coerceIn(0.0, 1.0).toFloat()
    }
    
    fun isOnTrack(
        saved: Long,
        target: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Boolean {
        val now = LocalDate.now()
        if (now >= endDate) return saved >= target
        
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toDouble()
        val elapsedDays = ChronoUnit.DAYS.between(startDate, now).toDouble()
        
        if (totalDays <= 0) return saved >= target
        
        val expectedProgress = elapsedDays / totalDays
        val actualProgress = calculateProgress(saved, target)
        
        return actualProgress >= expectedProgress * 0.9
    }
    
    fun validateSplitPercentages(percentages: List<Int>): Boolean {
        return percentages.sum() == 100 && percentages.all { it >= 0 }
    }
    
    fun distributeSplitEvenly(goalCount: Int): List<Int> {
        if (goalCount <= 0) return emptyList()
        if (goalCount == 1) return listOf(100)
        
        val basePercentage = 100 / goalCount
        val remainder = 100 % goalCount
        
        return List(goalCount) { index ->
            basePercentage + if (index < remainder) 1 else 0
        }
    }
    
    fun calculateSplitAmount(totalAmount: Long, percentage: Int): Long {
        return (totalAmount * percentage / 100.0).toLong()
    }
}
