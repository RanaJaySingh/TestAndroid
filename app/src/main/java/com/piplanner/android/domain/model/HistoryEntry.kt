package com.piplanner.android.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: HistoryEntryType,
    val amount: Long,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val splits: List<GoalSplit> = emptyList(),
    val description: String? = null,
    val isLocked: Boolean = false,
    val relatedGoalId: String? = null,
    val previousBalance: Long? = null,
    val newBalance: Long? = null
)

enum class HistoryEntryType {
    OPENING_BALANCE,
    NEW_CREDIT,
    CUSTOM_SPLIT,
    TYPED_ENTRY,
    TRANSFER,
    WITHDRAWAL,
    GOAL_DELETED
}

data class GoalSplit(
    val goalId: String,
    val goalName: String,
    val percentage: Int,
    val amount: Long
)
