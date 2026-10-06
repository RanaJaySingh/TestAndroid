package com.piplanner.android.data.repository

import com.piplanner.android.data.local.dao.AccountDao
import com.piplanner.android.data.local.entity.toEntity
import com.piplanner.android.data.local.entity.toDomain
import com.piplanner.android.domain.model.Account
import com.piplanner.android.domain.model.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepository(private val accountDao: AccountDao) {
    
    fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getDedicatedSavingsAccount(): Flow<Account?> {
        return accountDao.getDedicatedSavingsAccount().map { entity ->
            entity?.toDomain()
        }
    }
    
    suspend fun getDedicatedSavingsAccountSync(): Account? {
        return accountDao.getDedicatedSavingsAccountSync()?.toDomain()
    }
    
    suspend fun getAccountById(id: String): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }
    
    suspend fun insertAccount(account: Account) {
        accountDao.insertAccount(account.toEntity())
    }
    
    suspend fun insertAccounts(accounts: List<Account>) {
        accountDao.insertAccounts(accounts.map { it.toEntity() })
    }
    
    suspend fun setDedicatedSavings(accountId: String) {
        accountDao.clearDedicatedSavings()
        accountDao.setDedicatedSavings(accountId)
    }
    
    suspend fun updateBalance(accountId: String, balance: Long) {
        accountDao.updateBalance(accountId, balance)
    }
    
    suspend fun deleteAllAccounts() {
        accountDao.deleteAllAccounts()
    }
    
    suspend fun initializeDemoAccounts() {
        val demoAccounts = listOf(
            Account(
                id = "hdfc_4821",
                bankName = "HDFC",
                accountNumber = "XXXX4821",
                maskedNumber = "4821",
                balance = 100000,
                type = AccountType.SAVINGS,
                isDedicatedSavings = false
            ),
            Account(
                id = "sbi_7730",
                bankName = "SBI",
                accountNumber = "XXXX7730",
                maskedNumber = "7730",
                balance = 72000,
                type = AccountType.SPENDING,
                isDedicatedSavings = false
            )
        )
        insertAccounts(demoAccounts)
    }
}
