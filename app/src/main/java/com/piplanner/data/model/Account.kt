package com.piplanner.data.model

import kotlinx.serialization.Serializable

/**
 * Shared contract — Spec §3.1 Account.
 * [balance] is stored in paisa (Long); display via FormattingService as INR rupees.
 */
@Serializable
data class Account(
    val id: String,
    val bankName: String,
    val maskedNumber: String,
    val balance: Long,
    val isDedicated: Boolean,
    val isPaytmLinked: Boolean,
    val consentAutoUpdate: Boolean,
)
