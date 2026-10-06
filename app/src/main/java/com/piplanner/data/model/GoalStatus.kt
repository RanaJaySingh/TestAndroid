package com.piplanner.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared contract — Spec §3.1 Goal.status: OnTrack | Behind(shortfall).
 * [shortfall] is in paisa.
 */
@Serializable
sealed class GoalStatus {
    @Serializable
    @SerialName("OnTrack")
    data object OnTrack : GoalStatus()

    @Serializable
    @SerialName("Behind")
    data class Behind(
        val shortfall: Long,
    ) : GoalStatus()
}
