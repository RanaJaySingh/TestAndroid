package com.piplanner.android.ui.screens.withdrawal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.piplanner.android.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val historyRepository = PiPlannerApplication.instance.historyRepository
    
    val goals by goalRepository.getAllGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    
    var amountText by remember { mutableStateOf("") }
    var withdrawalPercentages by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    
    val amount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val totalSaved = goals.sumOf { it.savedAmount }
    val isInsufficientFunds = amount > totalSaved
    
    LaunchedEffect(goals) {
        if (withdrawalPercentages.isEmpty() && goals.isNotEmpty()) {
            val equalShare = 100f / goals.size
            withdrawalPercentages = goals.associate { it.id to equalShare }
        }
    }
    
    val totalPercentage = withdrawalPercentages.values.sum()
    val isValidSplit = totalPercentage in 99.9f..100.1f
    val isValid = amount > 0 && !isInsufficientFunds && isValidSplit
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Record a withdrawal",
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How much did you take out?",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Text(
                    text = "Your savings balance went down by",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                OutlinedTextField(
                    value = if (amount > 0) CurrencyFormatter.formatIndianNumber(amount) else "",
                    onValueChange = { newValue ->
                        amountText = newValue.filter { it.isDigit() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Text(
                            "₹",
                            style = MaterialTheme.typography.titleLarge,
                            color = NavyPrimary
                        )
                    },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isInsufficientFunds,
                    supportingText = if (isInsufficientFunds) {
                        { Text("Amount exceeds total saved (${CurrencyFormatter.formatIndianRupees(totalSaved)})") }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (amount > 0 && goals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Choose where goals it comes from",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    goals.forEach { goal ->
                        val percentage = withdrawalPercentages[goal.id] ?: 0f
                        val withdrawAmount = (amount * percentage / 100).toLong()
                        
                        PiPlannerCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goal.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatIndianRupees(goal.savedAmount)} saved",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    
                                    if (withdrawAmount > 0) {
                                        Text(
                                            text = "-${CurrencyFormatter.formatIndianRupees(withdrawAmount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StatusRed
                                        )
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Slider(
                                        value = percentage,
                                        onValueChange = { newValue ->
                                            withdrawalPercentages = withdrawalPercentages + (goal.id to newValue)
                                        },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.width(100.dp),
                                        colors = SliderDefaults.colors(
                                            thumbColor = NavyPrimary,
                                            activeTrackColor = NavyPrimary
                                        )
                                    )
                                    
                                    Text(
                                        text = "${percentage.toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(48.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${totalPercentage.toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isValidSplit) StatusGreen else StatusRed
                        )
                    }
                    
                    if (!isValidSplit) {
                        Text(
                            text = "Percentages must total 100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusRed
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(24.dp)
            ) {
                PrimaryButton(
                    text = "Save and lock",
                    onClick = {
                        scope.launch {
                            val splits = mutableListOf<GoalSplit>()
                            
                            goals.forEach { goal ->
                                val percentage = withdrawalPercentages[goal.id] ?: 0f
                                val withdrawAmount = (amount * percentage / 100).toLong()
                                
                                if (withdrawAmount > 0) {
                                    goalRepository.subtractFromSavedAmount(goal.id, withdrawAmount)
                                    
                                    splits.add(
                                        GoalSplit(
                                            goalId = goal.id,
                                            goalName = goal.name,
                                            percentage = percentage.toInt(),
                                            amount = withdrawAmount
                                        )
                                    )
                                }
                            }
                            
                            dedicatedAccount?.let { account ->
                                accountRepository.updateBalance(account.id, account.balance - amount)
                            }
                            
                            val historyEntry = HistoryEntry(
                                type = HistoryEntryType.WITHDRAWAL,
                                amount = amount,
                                splits = splits,
                                description = "Withdrawal recorded",
                                isLocked = true
                            )
                            historyRepository.insertEntry(historyEntry)
                            
                            onComplete()
                        }
                    },
                    enabled = isValid
                )
            }
        }
    }
}
