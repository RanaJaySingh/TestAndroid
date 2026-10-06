package com.piplanner.android.domain.model

import java.time.LocalDate
import java.util.UUID

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Long,
    val targetWithInflation: Long,
    val savedAmount: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val inflationRate: Double = 7.0,
    val sharePercentage: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pendingShareChange: Int? = null
) {
    val progress: Float
        get() = if (targetWithInflation > 0) {
            (savedAmount.toFloat() / targetWithInflation).coerceIn(0f, 1f)
        } else 0f
    
    val isOnTrack: Boolean
        get() = com.piplanner.android.util.CalculationUtils.isOnTrack(
            savedAmount, targetWithInflation, startDate, endDate
        )
    
    val needsPerMonth: Long
        get() = com.piplanner.android.util.CalculationUtils.calculateMonthlyNeeded(
            targetWithInflation, savedAmount, LocalDate.now(), endDate
        )
    
    val remaining: Long
        get() = (targetWithInflation - savedAmount).coerceAtLeast(0)
}
