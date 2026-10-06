package com.piplanner.android.data.local.dao

import androidx.room.*
import com.piplanner.android.data.local.entity.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<HistoryEntryEntity>>
    
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    suspend fun getAllEntriesSync(): List<HistoryEntryEntity>
    
    @Query("SELECT * FROM history_entries WHERE id = :id")
    suspend fun getEntryById(id: String): HistoryEntryEntity?
    
    @Query("SELECT * FROM history_entries WHERE relatedGoalId = :goalId ORDER BY timestamp DESC")
    fun getEntriesForGoal(goalId: String): Flow<List<HistoryEntryEntity>>
    
    @Query("SELECT * FROM history_entries WHERE type = :type ORDER BY timestamp DESC")
    fun getEntriesByType(type: String): Flow<List<HistoryEntryEntity>>
    
    @Query("SELECT * FROM history_entries WHERE isLocked = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestUnlockedEntry(): HistoryEntryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HistoryEntryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<HistoryEntryEntity>)
    
    @Update
    suspend fun updateEntry(entry: HistoryEntryEntity)
    
    @Query("UPDATE history_entries SET isLocked = 1 WHERE id = :entryId")
    suspend fun lockEntry(entryId: String)
    
    @Delete
    suspend fun deleteEntry(entry: HistoryEntryEntity)
    
    @Query("DELETE FROM history_entries")
    suspend fun deleteAllEntries()
    
    @Query("SELECT COUNT(*) FROM history_entries WHERE isLocked = 0")
    suspend fun getUnlockedEntryCount(): Int
}
