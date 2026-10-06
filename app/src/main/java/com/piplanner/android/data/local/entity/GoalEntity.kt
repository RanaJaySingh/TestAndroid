package com.piplanner.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.piplanner.android.domain.model.Goal
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val targetAmount: Long,
    val targetWithInflation: Long,
    val savedAmount: Long,
    val startDate: String,
    val endDate: String,
    val inflationRate: Double,
    val sharePercentage: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val pendingShareChange: Int?
)

fun GoalEntity.toDomain(): Goal {
    return Goal(
        id = id,
        name = name,
        targetAmount = targetAmount,
        targetWithInflation = targetWithInflation,
        savedAmount = savedAmount,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        inflationRate = inflationRate,
        sharePercentage = sharePercentage,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pendingShareChange = pendingShareChange
    )
}

fun Goal.toEntity(): GoalEntity {
    return GoalEntity(
        id = id,
        name = name,
        targetAmount = targetAmount,
        targetWithInflation = targetWithInflation,
        savedAmount = savedAmount,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        inflationRate = inflationRate,
        sharePercentage = sharePercentage,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pendingShareChange = pendingShareChange
    )
}
