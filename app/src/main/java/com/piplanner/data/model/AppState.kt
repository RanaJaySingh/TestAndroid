package com.piplanner.data.model

import kotlinx.serialization.Serializable

/**
 * Aggregate demo state persisted by [com.piplanner.data.local.PersistenceService].
 * Fresh install / post-reset: empty collections.
 */
@Serializable
data class AppState(
    val accounts: List<Account> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val standingSplits: List<StandingSplit> = emptyList(),
    val hasCompletedSetup: Boolean = false,
) {
    companion object {
        val EMPTY: AppState = AppState()
    }
}
