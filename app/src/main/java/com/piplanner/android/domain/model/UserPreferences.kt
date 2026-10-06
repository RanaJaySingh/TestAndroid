package com.piplanner.android.domain.model

data class UserPreferences(
    val userName: String = "Rahul",
    val hasCompletedSetup: Boolean = false,
    val autoBalanceUpdates: Boolean = true,
    val lastSyncTimestamp: Long? = null,
    val defaultInflationRate: Double = 7.0
)
