package com.piplanner.android.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.domain.model.GoalSplit
import com.piplanner.android.domain.model.HistoryEntry
import com.piplanner.android.domain.model.HistoryEntryType
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CalculationUtils
import com.piplanner.android.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteGoalSheet(
    goalId: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val historyRepository = PiPlannerApplication.instance.historyRepository
    val scope = rememberCoroutineScope()
    
    var goal by remember { mutableStateOf<com.piplanner.android.domain.model.Goal?>(null) }
    var otherGoals by remember { mutableStateOf<List<com.piplanner.android.domain.model.Goal>>(emptyList()) }
    
    LaunchedEffect(goalId) {
        goal = goalRepository.getGoalById(goalId)
        otherGoals = goalRepository.getAllGoalsSync().filter { it.id != goalId }
    }
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Delete goal",
                onBackClick = onDismiss
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            goal?.let { currentGoal ->
                PiPlannerCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = StatusAmber,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Delete \"${currentGoal.name}\"?",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "This will release ${CurrencyFormatter.formatIndianRupees(currentGoal.savedAmount)} saved in this goal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        
                        if (otherGoals.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "The amount will be split equally across your remaining ${otherGoals.size} goal${if (otherGoals.size > 1) "s" else ""}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                val savedAmount = currentGoal.savedAmount
                                
                                if (otherGoals.isNotEmpty() && savedAmount > 0) {
                                    val amountPerGoal = savedAmount / otherGoals.size
                                    val remainder = savedAmount % otherGoals.size
                                    
                                    val splits = mutableListOf<GoalSplit>()
                                    
                                    otherGoals.forEachIndexed { index, otherGoal ->
                                        val amount = amountPerGoal + if (index < remainder) 1 else 0
                                        goalRepository.addToSavedAmount(otherGoal.id, amount)
                                        
                                        splits.add(
                                            GoalSplit(
                                                goalId = otherGoal.id,
                                                goalName = otherGoal.name,
                                                percentage = 100 / otherGoals.size,
                                                amount = amount
                                            )
                                        )
                                    }
                                    
                                    val newPercentages = CalculationUtils.distributeSplitEvenly(otherGoals.size)
                                    otherGoals.forEachIndexed { index, otherGoal ->
                                        goalRepository.updateSharePercentage(otherGoal.id, newPercentages[index])
                                    }
                                    
                                    val historyEntry = HistoryEntry(
                                        type = HistoryEntryType.GOAL_DELETED,
                                        amount = savedAmount,
                                        splits = splits,
                                        description = "Goal '${currentGoal.name}' deleted, ${CurrencyFormatter.formatIndianRupees(savedAmount)} redistributed",
                                        isLocked = true,
                                        relatedGoalId = goalId
                                    )
                                    historyRepository.insertEntry(historyEntry)
                                }
                                
                                goalRepository.deleteGoal(goalId)
                                onConfirm()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
