package com.piplanner.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.piplanner.android.domain.model.Account
import com.piplanner.android.domain.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val bankName: String,
    val accountNumber: String,
    val maskedNumber: String,
    val balance: Long,
    val type: String,
    val isDedicatedSavings: Boolean
)

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        bankName = bankName,
        accountNumber = accountNumber,
        maskedNumber = maskedNumber,
        balance = balance,
        type = AccountType.valueOf(type),
        isDedicatedSavings = isDedicatedSavings
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        bankName = bankName,
        accountNumber = accountNumber,
        maskedNumber = maskedNumber,
        balance = balance,
        type = type.name,
        isDedicatedSavings = isDedicatedSavings
    )
}
