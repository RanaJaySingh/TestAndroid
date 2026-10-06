package com.piplanner.android.data.local.dao

import androidx.room.*
import com.piplanner.android.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>
    
    @Query("SELECT * FROM goals ORDER BY createdAt ASC")
    suspend fun getAllGoalsSync(): List<GoalEntity>
    
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: String): GoalEntity?
    
    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalByIdFlow(id: String): Flow<GoalEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>)
    
    @Update
    suspend fun updateGoal(goal: GoalEntity)
    
    @Query("UPDATE goals SET savedAmount = savedAmount + :amount WHERE id = :goalId")
    suspend fun addToSavedAmount(goalId: String, amount: Long)
    
    @Query("UPDATE goals SET savedAmount = savedAmount - :amount WHERE id = :goalId")
    suspend fun subtractFromSavedAmount(goalId: String, amount: Long)
    
    @Query("UPDATE goals SET sharePercentage = :percentage, updatedAt = :updatedAt WHERE id = :goalId")
    suspend fun updateSharePercentage(goalId: String, percentage: Int, updatedAt: Long)
    
    @Query("UPDATE goals SET pendingShareChange = :pendingShare WHERE id = :goalId")
    suspend fun setPendingShareChange(goalId: String, pendingShare: Int?)
    
    @Query("UPDATE goals SET sharePercentage = pendingShareChange, pendingShareChange = NULL WHERE pendingShareChange IS NOT NULL")
    suspend fun applyPendingShareChanges()
    
    @Delete
    suspend fun deleteGoal(goal: GoalEntity)
    
    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: String)
    
    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()
    
    @Query("SELECT COUNT(*) FROM goals")
    suspend fun getGoalCount(): Int
    
    @Query("SELECT SUM(savedAmount) FROM goals")
    suspend fun getTotalSaved(): Long?
}
