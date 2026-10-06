package com.piplanner.android.domain.model

data class Account(
    val id: String,
    val bankName: String,
    val accountNumber: String,
    val maskedNumber: String,
    val balance: Long,
    val type: AccountType,
    val isDedicatedSavings: Boolean = false
)

enum class AccountType {
    SAVINGS,
    SPENDING
}

fun Account.displayName(): String {
    return "$type · $bankName ••${maskedNumber.takeLast(4)}"
}
