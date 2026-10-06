package com.piplanner.android.ui.screens.assign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AutoAwesome
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignCreditScreen(
    newAmount: Long,
    onBack: () -> Unit,
    onSaveAndLock: () -> Unit,
    onNewGoal: () -> Unit
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val historyRepository = PiPlannerApplication.instance.historyRepository
    val accountRepository = PiPlannerApplication.instance.accountRepository
    
    val goals by goalRepository.getAllGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    
    var percentages by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var useStandingSplit by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showCountUp by remember { mutableStateOf(false) }
    
    LaunchedEffect(goals) {
        if (percentages.isEmpty() && goals.isNotEmpty()) {
            percentages = goals.associate { it.id to it.sharePercentage }
        }
    }
    
    val totalPercentage = percentages.values.sum()
    val isValid = totalPercentage == 100
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Assign now",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        if (showCountUp) {
            SavedConfirmation(
                amount = newAmount,
                onComplete = onSaveAndLock
            )
        } else {
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
                    PiPlannerCard {
                        Column {
                            Text(
                                text = "New amount",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "+${CurrencyFormatter.formatIndianRupees(newAmount)}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = StatusGreen
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Split across goals",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        TextButton(onClick = { useStandingSplit = !useStandingSplit }) {
                            Text(
                                text = if (useStandingSplit) "Custom split" else "Standing split",
                                color = NavyPrimary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (goals.isEmpty()) {
                        PiPlannerCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No goals yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Create a goal to assign this credit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                PrimaryButton(
                                    text = "Create goal",
                                    onClick = onNewGoal
                                )
                            }
                        }
                    } else {
                        goals.forEach { goal ->
                            val percentage = if (useStandingSplit) {
                                goal.sharePercentage
                            } else {
                                percentages[goal.id] ?: goal.sharePercentage
                            }
                            val splitAmount = CalculationUtils.calculateSplitAmount(newAmount, percentage)
                            
                            GoalSplitCard(
                                goal = goal,
                                percentage = percentage,
                                splitAmount = splitAmount,
                                onPercentageChange = if (!useStandingSplit) { newPercentage ->
                                    percentages = percentages + (goal.id to newPercentage)
                                } else null,
                                isEditable = !useStandingSplit && goals.size > 1
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        if (!useStandingSplit && goals.size > 1) {
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total must equal 100%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusRed
                                )
                            }
                        }
                    }
                }
                
                if (goals.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface)
                            .padding(24.dp)
                    ) {
                        PrimaryButton(
                            text = if (isSaving) "Saving..." else "Save and lock",
                            onClick = {
                                if (!isSaving && (useStandingSplit || isValid)) {
                                    isSaving = true
                                    scope.launch {
                                        val splits = mutableListOf<GoalSplit>()
                                        
                                        for (goal in goals) {
                                            val percentage = if (useStandingSplit) {
                                                goal.sharePercentage
                                            } else {
                                                percentages[goal.id] ?: goal.sharePercentage
                                            }
                                            val splitAmount = CalculationUtils.calculateSplitAmount(newAmount, percentage)
                                            
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
                                        
                                        val dedicatedAccount = accountRepository.getDedicatedSavingsAccountSync()
                                        dedicatedAccount?.let { account ->
                                            accountRepository.updateBalance(account.id, account.balance + newAmount)
                                        }
                                        
                                        val historyEntry = HistoryEntry(
                                            type = if (useStandingSplit) HistoryEntryType.NEW_CREDIT else HistoryEntryType.CUSTOM_SPLIT,
                                            amount = newAmount,
                                            splits = splits,
                                            description = if (useStandingSplit) "New credit - Standing split" else "New credit - Custom split",
                                            isLocked = true,
                                            previousBalance = dedicatedAccount?.balance,
                                            newBalance = (dedicatedAccount?.balance ?: 0L) + newAmount
                                        )
                                        historyRepository.insertEntry(historyEntry)
                                        
                                        showCountUp = true
                                    }
                                }
                            },
                            enabled = !isSaving && (useStandingSplit || isValid)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalSplitCard(
    goal: Goal,
    percentage: Int,
    splitAmount: Long,
    onPercentageChange: ((Int) -> Unit)?,
    isEditable: Boolean
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
                    text = "+${CurrencyFormatter.formatIndianRupees(splitAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = StatusGreen
                )
            }
            
            if (isEditable && onPercentageChange != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (percentage > 0) onPercentageChange(percentage - 5) },
                        modifier = Modifier.size(32.dp)
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
                        modifier = Modifier.width(64.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
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
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            } else {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NavyPrimary
                )
            }
        }
    }
}

@Composable
private fun SavedConfirmation(
    amount: Long,
    onComplete: () -> Unit
) {
    var displayAmount by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(amount) {
        val steps = 20
        val increment = amount / steps
        repeat(steps) { i ->
            kotlinx.coroutines.delay(50)
            displayAmount = ((i + 1) * increment).coerceAtMost(amount)
        }
        displayAmount = amount
        kotlinx.coroutines.delay(1000)
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = StatusGreen,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Saved",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "+${CurrencyFormatter.formatIndianRupees(displayAmount)}",
                style = MaterialTheme.typography.displayMedium,
                color = StatusGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Locked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
