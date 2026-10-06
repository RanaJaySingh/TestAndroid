package com.piplanner.android.data.local.dao

import androidx.room.*
import com.piplanner.android.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): AccountEntity?
    
    @Query("SELECT * FROM accounts WHERE isDedicatedSavings = 1 LIMIT 1")
    fun getDedicatedSavingsAccount(): Flow<AccountEntity?>
    
    @Query("SELECT * FROM accounts WHERE isDedicatedSavings = 1 LIMIT 1")
    suspend fun getDedicatedSavingsAccountSync(): AccountEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)
    
    @Update
    suspend fun updateAccount(account: AccountEntity)
    
    @Query("UPDATE accounts SET isDedicatedSavings = 0")
    suspend fun clearDedicatedSavings()
    
    @Query("UPDATE accounts SET isDedicatedSavings = 1 WHERE id = :accountId")
    suspend fun setDedicatedSavings(accountId: String)
    
    @Query("UPDATE accounts SET balance = :balance WHERE id = :accountId")
    suspend fun updateBalance(accountId: String, balance: Long)
    
    @Delete
    suspend fun deleteAccount(account: AccountEntity)
    
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
