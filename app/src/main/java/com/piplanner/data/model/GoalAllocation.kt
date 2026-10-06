package com.piplanner.data.model

import kotlinx.serialization.Serializable

/**
 * Shared contract — Spec §3.1 GoalAllocation.
 * [amount] is Long paisa; [percentage] is 0.0–1.0; [goalName] is a snapshot.
 */
@Serializable
data class GoalAllocation(
    val goalId: String,
    val goalName: String,
    val amount: Long,
    val percentage: Double,
)
