package com.piplanner.android.data.repository

import com.piplanner.android.data.local.dao.GoalDao
import com.piplanner.android.data.local.entity.toEntity
import com.piplanner.android.data.local.entity.toDomain
import com.piplanner.android.domain.model.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepository(private val goalDao: GoalDao) {
    
    fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getAllGoalsSync(): List<Goal> {
        return goalDao.getAllGoalsSync().map { it.toDomain() }
    }
    
    suspend fun getGoalById(id: String): Goal? {
        return goalDao.getGoalById(id)?.toDomain()
    }
    
    fun getGoalByIdFlow(id: String): Flow<Goal?> {
        return goalDao.getGoalByIdFlow(id).map { it?.toDomain() }
    }
    
    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal.toEntity())
    }
    
    suspend fun insertGoals(goals: List<Goal>) {
        goalDao.insertGoals(goals.map { it.toEntity() })
    }
    
    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal.toEntity())
    }
    
    suspend fun addToSavedAmount(goalId: String, amount: Long) {
        goalDao.addToSavedAmount(goalId, amount)
    }
    
    suspend fun subtractFromSavedAmount(goalId: String, amount: Long) {
        goalDao.subtractFromSavedAmount(goalId, amount)
    }
    
    suspend fun updateSharePercentage(goalId: String, percentage: Int) {
        goalDao.updateSharePercentage(goalId, percentage, System.currentTimeMillis())
    }
    
    suspend fun setPendingShareChange(goalId: String, pendingShare: Int?) {
        goalDao.setPendingShareChange(goalId, pendingShare)
    }
    
    suspend fun applyPendingShareChanges() {
        goalDao.applyPendingShareChanges()
    }
    
    suspend fun deleteGoal(goalId: String) {
        goalDao.deleteGoalById(goalId)
    }
    
    suspend fun deleteAllGoals() {
        goalDao.deleteAllGoals()
    }
    
    suspend fun getGoalCount(): Int {
        return goalDao.getGoalCount()
    }
    
    suspend fun getTotalSaved(): Long {
        return goalDao.getTotalSaved() ?: 0L
    }
}
