package com.piplanner.data.model

import kotlinx.serialization.Serializable

/**
 * Marks a goal edit that is saved but applies at the next credit (PRD R11 / R24, Spec BR-4).
 * Earlier History stays frozen; UI shows toast 9c and edit-held info 13g until cleared.
 */
@Serializable
data class HeldGoalChange(
    val goalId: String,
    /** ISO-8601 Instant when the edit was saved. */
    val savedAt: String,
)
