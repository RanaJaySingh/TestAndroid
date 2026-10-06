package com.piplanner.android.ui.screens.goals

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
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingSplitSheet(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val goals by goalRepository.getAllGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    
    var percentages by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    LaunchedEffect(goals) {
        if (percentages.isEmpty() && goals.isNotEmpty()) {
            percentages = goals.associate { it.id to it.sharePercentage }
        }
    }
    
    val totalPercentage = percentages.values.sum()
    val isValid = totalPercentage == 100
    val hasChanges = goals.any { goal ->
        val current = percentages[goal.id] ?: goal.sharePercentage
        current != goal.sharePercentage
    }
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Standing split",
                onBackClick = onDismiss
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
                    text = "Default split for new credits",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This split will be used automatically when new money arrives in your savings account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                goals.forEach { goal ->
                    val percentage = percentages[goal.id] ?: goal.sharePercentage
                    
                    StandingSplitGoalCard(
                        goal = goal,
                        percentage = percentage,
                        onPercentageChange = { newPercentage ->
                            percentages = percentages + (goal.id to newPercentage)
                        },
                        isEditable = goals.size > 1
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total must equal 100%",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRed
                    )
                }
                
                if (hasChanges) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PiPlannerCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Changes will apply to the next credit. Existing saved amounts won't change.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
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
                    text = "Save standing split",
                    onClick = {
                        scope.launch {
                            for (goal in goals) {
                                val newPercentage = percentages[goal.id] ?: goal.sharePercentage
                                if (newPercentage != goal.sharePercentage) {
                                    goalRepository.setPendingShareChange(goal.id, newPercentage)
                                }
                            }
                            onSave()
                        }
                    },
                    enabled = isValid
                )
            }
        }
    }
}

@Composable
private fun StandingSplitGoalCard(
    goal: Goal,
    percentage: Int,
    onPercentageChange: (Int) -> Unit,
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
                    text = "Needs ${CurrencyFormatter.formatIndianRupees(goal.needsPerMonth)}/month",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                if (goal.pendingShareChange != null && goal.pendingShareChange != goal.sharePercentage) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Currently ${goal.sharePercentage}% → ${goal.pendingShareChange}% at next credit",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusAmber
                    )
                }
            }
            
            if (isEditable) {
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
                    text = "100%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NavyPrimary
                )
            }
        }
    }
}
