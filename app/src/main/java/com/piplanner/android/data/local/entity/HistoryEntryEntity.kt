package com.piplanner.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.piplanner.android.domain.model.GoalSplit
import com.piplanner.android.domain.model.HistoryEntry
import com.piplanner.android.domain.model.HistoryEntryType
import java.time.LocalDateTime

@Entity(tableName = "history_entries")
data class HistoryEntryEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val amount: Long,
    val timestamp: String,
    val splitsJson: String,
    val description: String?,
    val isLocked: Boolean,
    val relatedGoalId: String?,
    val previousBalance: Long?,
    val newBalance: Long?
)

fun HistoryEntryEntity.toDomain(): HistoryEntry {
    return HistoryEntry(
        id = id,
        type = HistoryEntryType.valueOf(type),
        amount = amount,
        timestamp = LocalDateTime.parse(timestamp),
        splits = parseSplitsJson(splitsJson),
        description = description,
        isLocked = isLocked,
        relatedGoalId = relatedGoalId,
        previousBalance = previousBalance,
        newBalance = newBalance
    )
}

fun HistoryEntry.toEntity(): HistoryEntryEntity {
    return HistoryEntryEntity(
        id = id,
        type = type.name,
        amount = amount,
        timestamp = timestamp.toString(),
        splitsJson = splits.toSplitsJson(),
        description = description,
        isLocked = isLocked,
        relatedGoalId = relatedGoalId,
        previousBalance = previousBalance,
        newBalance = newBalance
    )
}

private fun parseSplitsJson(json: String): List<GoalSplit> {
    if (json.isEmpty() || json == "[]") return emptyList()
    
    return try {
        val splits = mutableListOf<GoalSplit>()
        val items = json.removeSurrounding("[", "]").split("},{")
        
        for (item in items) {
            val cleanItem = item.removePrefix("{").removeSuffix("}")
            val parts = cleanItem.split(",")
            
            var goalId = ""
            var goalName = ""
            var percentage = 0
            var amount = 0L
            
            for (part in parts) {
                val keyValue = part.split(":")
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim().removeSurrounding("\"")
                    val value = keyValue[1].trim().removeSurrounding("\"")
                    
                    when (key) {
                        "goalId" -> goalId = value
                        "goalName" -> goalName = value
                        "percentage" -> percentage = value.toIntOrNull() ?: 0
                        "amount" -> amount = value.toLongOrNull() ?: 0L
                    }
                }
            }
            
            if (goalId.isNotEmpty()) {
                splits.add(GoalSplit(goalId, goalName, percentage, amount))
            }
        }
        
        splits
    } catch (e: Exception) {
        emptyList()
    }
}

private fun List<GoalSplit>.toSplitsJson(): String {
    if (isEmpty()) return "[]"
    
    return "[" + joinToString(",") { split ->
        """{"goalId":"${split.goalId}","goalName":"${split.goalName}","percentage":${split.percentage},"amount":${split.amount}}"""
    } + "]"
}
