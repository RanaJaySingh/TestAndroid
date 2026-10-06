package com.piplanner.android.ui.screens.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
fun TransferScreen(
    preselectedFromGoalId: String?,
    preselectedToGoalId: String?,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val historyRepository = PiPlannerApplication.instance.historyRepository
    val goals by goalRepository.getAllGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    
    var fromGoal by remember { mutableStateOf<Goal?>(null) }
    var toGoal by remember { mutableStateOf<Goal?>(null) }
    var amountText by remember { mutableStateOf("") }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    
    val amount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val isValid = fromGoal != null && toGoal != null && amount > 0 && 
                  fromGoal?.id != toGoal?.id && amount <= (fromGoal?.savedAmount ?: 0L)
    val isInsufficientFunds = fromGoal != null && amount > (fromGoal?.savedAmount ?: 0L)
    
    LaunchedEffect(goals, preselectedFromGoalId, preselectedToGoalId) {
        if (goals.isNotEmpty()) {
            preselectedFromGoalId?.let { id ->
                fromGoal = goals.find { it.id == id }
            }
            preselectedToGoalId?.let { id ->
                toGoal = goals.find { it.id == id }
            }
        }
    }
    
    if (showFromPicker) {
        GoalPickerDialog(
            goals = goals.filter { it.id != toGoal?.id },
            selectedGoal = fromGoal,
            onSelect = { 
                fromGoal = it
                showFromPicker = false
            },
            onDismiss = { showFromPicker = false },
            title = "Transfer from"
        )
    }
    
    if (showToPicker) {
        GoalPickerDialog(
            goals = goals.filter { it.id != fromGoal?.id },
            selectedGoal = toGoal,
            onSelect = {
                toGoal = it
                showToPicker = false
            },
            onDismiss = { showToPicker = false },
            title = "Transfer to"
        )
    }
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Transfer",
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
                    text = "From",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                
                GoalSelector(
                    goal = fromGoal,
                    placeholder = "Select goal",
                    onClick = { showFromPicker = true }
                )
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val temp = fromGoal
                            fromGoal = toGoal
                            toGoal = temp
                        },
                        enabled = fromGoal != null || toGoal != null
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = "Swap",
                            tint = NavyPrimary
                        )
                    }
                }
                
                Text(
                    text = "To",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                
                GoalSelector(
                    goal = toGoal,
                    placeholder = "Select goal",
                    onClick = { showToPicker = true }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelMedium,
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
                        { Text("Insufficient funds in ${fromGoal?.name}") }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1000L, 5000L, 10000L).forEach { chipAmount ->
                        SuggestionChip(
                            onClick = { amountText = chipAmount.toString() },
                            label = { Text(CurrencyFormatter.formatIndianRupees(chipAmount)) }
                        )
                    }
                }
                
                if (fromGoal != null && toGoal != null && amount > 0 && !isInsufficientFunds) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PiPlannerCard {
                        Column {
                            Text(
                                text = "After transfer",
                                style = MaterialTheme.typography.titleSmall
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = fromGoal?.name ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = CurrencyFormatter.formatIndianRupees((fromGoal?.savedAmount ?: 0L) - amount),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = toGoal?.name ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = CurrencyFormatter.formatIndianRupees((toGoal?.savedAmount ?: 0L) + amount),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
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
                    text = "Move ${if (amount > 0) CurrencyFormatter.formatIndianRupees(amount) else ""}",
                    onClick = {
                        scope.launch {
                            fromGoal?.let { from ->
                                toGoal?.let { to ->
                                    goalRepository.subtractFromSavedAmount(from.id, amount)
                                    goalRepository.addToSavedAmount(to.id, amount)
                                    
                                    val historyEntry = HistoryEntry(
                                        type = HistoryEntryType.TRANSFER,
                                        amount = amount,
                                        splits = listOf(
                                            GoalSplit(from.id, from.name, 0, -amount),
                                            GoalSplit(to.id, to.name, 0, amount)
                                        ),
                                        description = "Transfer from ${from.name} to ${to.name}",
                                        isLocked = true
                                    )
                                    historyRepository.insertEntry(historyEntry)
                                    
                                    onComplete()
                                }
                            }
                        }
                    },
                    enabled = isValid
                )
            }
        }
    }
}

@Composable
private fun GoalSelector(
    goal: Goal?,
    placeholder: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (goal != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${CurrencyFormatter.formatIndianRupees(goal.savedAmount)} saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            } else {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun GoalPickerDialog(
    goals: List<Goal>,
    selectedGoal: Goal?,
    onSelect: (Goal) -> Unit,
    onDismiss: () -> Unit,
    title: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                goals.forEach { goal ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(goal) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (goal.id == selectedGoal?.id) NavyPrimary.copy(alpha = 0.1f) else Surface,
                        border = if (goal.id == selectedGoal?.id) 
                            androidx.compose.foundation.BorderStroke(2.dp, NavyPrimary) 
                        else 
                            androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${CurrencyFormatter.formatIndianRupees(goal.savedAmount)} saved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
