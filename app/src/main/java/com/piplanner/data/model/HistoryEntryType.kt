package com.piplanner.data.model

import kotlinx.serialization.Serializable

/**
 * Shared contract — Spec §3.1 HistoryEntryType.
 */
@Serializable
enum class HistoryEntryType {
    OpeningBalance,
    NewCredit,
    Transfer,
    Withdrawal,
    GoalDeleted,
}
