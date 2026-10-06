package com.piplanner.android.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.domain.model.Goal
import com.piplanner.android.domain.model.GoalSplit
import com.piplanner.android.domain.model.HistoryEntry
import com.piplanner.android.domain.model.HistoryEntryType
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CalculationUtils
import com.piplanner.android.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningSplitScreen(
    onBack: () -> Unit,
    onLockSplit: () -> Unit
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val historyRepository = PiPlannerApplication.instance.historyRepository
    val preferencesDataStore = PiPlannerApplication.instance.preferencesDataStore
    
    val goals by goalRepository.getAllGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    
    var percentages by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    LaunchedEffect(goals) {
        if (percentages.isEmpty() && goals.isNotEmpty()) {
            percentages = goals.associate { it.id to it.sharePercentage }
        }
    }
    
    val totalPercentage = percentages.values.sum()
    val isValid = totalPercentage == 100
    val openingBalance = dedicatedAccount?.balance ?: 0L
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Opening split",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Split your opening balance across goals",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Opening balance: ${CurrencyFormatter.formatIndianRupees(openingBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                goals.forEach { goal ->
                    val percentage = percentages[goal.id] ?: goal.sharePercentage
                    val splitAmount = CalculationUtils.calculateSplitAmount(openingBalance, percentage)
                    
                    GoalSplitItem(
                        goal = goal,
                        percentage = percentage,
                        splitAmount = splitAmount,
                        onPercentageChange = { newPercentage ->
                            percentages = percentages + (goal.id to newPercentage)
                        },
                        singleGoal = goals.size == 1
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = "$totalPercentage%",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isValid) StatusGreen else StatusRed
                    )
                }
                
                if (!isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total must equal 100%",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRed
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(24.dp)
            ) {
                PrimaryButton(
                    text = "Lock this split",
                    onClick = {
                        scope.launch {
                            val splits = mutableListOf<GoalSplit>()
                            
                            for (goal in goals) {
                                val percentage = percentages[goal.id] ?: goal.sharePercentage
                                val splitAmount = CalculationUtils.calculateSplitAmount(openingBalance, percentage)
                                
                                goalRepository.updateSharePercentage(goal.id, percentage)
                                goalRepository.addToSavedAmount(goal.id, splitAmount)
                                
                                splits.add(
                                    GoalSplit(
                                        goalId = goal.id,
                                        goalName = goal.name,
                                        percentage = percentage,
                                        amount = splitAmount
                                    )
                                )
                            }
                            
                            val historyEntry = HistoryEntry(
                                type = HistoryEntryType.OPENING_BALANCE,
                                amount = openingBalance,
                                splits = splits,
                                description = "Opening balance",
                                isLocked = true,
                                newBalance = openingBalance
                            )
                            historyRepository.insertEntry(historyEntry)
                            
                            preferencesDataStore.setSetupCompleted(true)
                            preferencesDataStore.updateLastSyncTimestamp(System.currentTimeMillis())
                            
                            onLockSplit()
                        }
                    },
                    enabled = isValid
                )
            }
        }
    }
}

@Composable
private fun GoalSplitItem(
    goal: Goal,
    percentage: Int,
    splitAmount: Long,
    onPercentageChange: (Int) -> Unit,
    singleGoal: Boolean
) {
    PiPlannerCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Needs ${CurrencyFormatter.formatIndianRupees(goal.needsPerMonth)}/month",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = CurrencyFormatter.formatIndianRupees(splitAmount),
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyPrimary
                )
            }
            
            if (singleGoal) {
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NavyPrimary
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (percentage > 0) onPercentageChange(percentage - 5) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    OutlinedTextField(
                        value = percentage.toString(),
                        onValueChange = { newValue ->
                            val newPercentage = newValue.filter { it.isDigit() }.toIntOrNull() ?: 0
                            onPercentageChange(newPercentage.coerceIn(0, 100))
                        },
                        modifier = Modifier.width(72.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        suffix = { Text("%") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = CardBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    IconButton(
                        onClick = { if (percentage < 100) onPercentageChange(percentage + 5) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}
