package com.piplanner.android.data.repository

import com.piplanner.android.data.local.dao.HistoryDao
import com.piplanner.android.data.local.entity.toEntity
import com.piplanner.android.data.local.entity.toDomain
import com.piplanner.android.domain.model.HistoryEntry
import com.piplanner.android.domain.model.HistoryEntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val historyDao: HistoryDao) {
    
    fun getAllEntries(): Flow<List<HistoryEntry>> {
        return historyDao.getAllEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getAllEntriesSync(): List<HistoryEntry> {
        return historyDao.getAllEntriesSync().map { it.toDomain() }
    }
    
    suspend fun getEntryById(id: String): HistoryEntry? {
        return historyDao.getEntryById(id)?.toDomain()
    }
    
    fun getEntriesForGoal(goalId: String): Flow<List<HistoryEntry>> {
        return historyDao.getEntriesForGoal(goalId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getEntriesByType(type: HistoryEntryType): Flow<List<HistoryEntry>> {
        return historyDao.getEntriesByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getLatestUnlockedEntry(): HistoryEntry? {
        return historyDao.getLatestUnlockedEntry()?.toDomain()
    }
    
    suspend fun insertEntry(entry: HistoryEntry) {
        historyDao.insertEntry(entry.toEntity())
    }
    
    suspend fun updateEntry(entry: HistoryEntry) {
        historyDao.updateEntry(entry.toEntity())
    }
    
    suspend fun lockEntry(entryId: String) {
        historyDao.lockEntry(entryId)
    }
    
    suspend fun deleteAllEntries() {
        historyDao.deleteAllEntries()
    }
    
    suspend fun hasUnlockedEntries(): Boolean {
        return historyDao.getUnlockedEntryCount() > 0
    }
}
