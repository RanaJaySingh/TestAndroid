package com.piplanner.data.model

import kotlinx.serialization.Serializable

/**
 * Shared contract — Spec §3.1 HistoryEntry.
 * Optional monetary fields are Long paisa; [createdAt] is ISO-8601 Instant.
 */
@Serializable
data class HistoryEntry(
    val id: String,
    val type: HistoryEntryType,
    val createdAt: String,
    val isLocked: Boolean,
    val previousBalance: Long? = null,
    val newBalance: Long? = null,
    val creditAmount: Long? = null,
    val isTyped: Boolean = false,
    val fromGoalId: String? = null,
    val toGoalId: String? = null,
    val transferAmount: Long? = null,
    val withdrawalAmount: Long? = null,
    val deletedGoalName: String? = null,
    val releasedAmount: Long? = null,
    val allocations: List<GoalAllocation> = emptyList(),
)
