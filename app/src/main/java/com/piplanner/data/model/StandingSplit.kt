package com.piplanner.data.model

import kotlinx.serialization.Serializable

/**
 * Shared contract — Spec §3.1 StandingSplit.
 * [percentage] is 0.0–1.0; all splits must sum to 1.0.
 */
@Serializable
data class StandingSplit(
    val goalId: String,
    val percentage: Double,
)
