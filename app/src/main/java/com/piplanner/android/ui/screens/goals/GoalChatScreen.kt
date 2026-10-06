package com.piplanner.android.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.domain.model.Goal
import com.piplanner.android.domain.model.GrokResponseType
import com.piplanner.android.domain.model.ProposedGoal
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CalculationUtils
import com.piplanner.android.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalChatScreen(
    onBack: () -> Unit,
    onUseForm: () -> Unit,
    onGoalsConfirmed: () -> Unit
) {
    val grokService = PiPlannerApplication.instance.grokService
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val scope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var proposedGoals by remember { mutableStateOf<List<ProposedGoal>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Plan your goals",
                onBackClick = onBack,
                stepIndicator = { StepIndicator(currentStep = 3, totalSteps = 3) }
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "What are you planning for?",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Tell us your goals and we'll help you plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "For example: ₹5,00,000 for a car by Sep 2028, plus ₹1,00,000 emergency fund by next year",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    minLines = 4,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextActionButton(
                        text = "Use a form",
                        onClick = onUseForm
                    )
                }
                
                if (isProcessing) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Grok is thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusRed
                    )
                }
                
                proposedGoals?.let { goals ->
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    GrokProposalCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Grok's proposal",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        goals.forEach { goal ->
                            ProposedGoalItem(goal)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onUseForm,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Edit")
                            }
                            
                            Button(
                                onClick = {
                                    scope.launch {
                                        val domainGoals = goals.map { proposed ->
                                            val targetDate = parseTargetDate(proposed.endDate)
                                            val targetWithInflation = CalculationUtils.calculateInflationAdjustedTarget(
                                                proposed.targetAmount,
                                                7.0,
                                                LocalDate.now(),
                                                targetDate
                                            )
                                            
                                            Goal(
                                                id = UUID.randomUUID().toString(),
                                                name = proposed.name,
                                                targetAmount = proposed.targetAmount,
                                                targetWithInflation = targetWithInflation,
                                                savedAmount = 0,
                                                startDate = LocalDate.now(),
                                                endDate = targetDate,
                                                inflationRate = 7.0,
                                                sharePercentage = proposed.suggestedShare
                                            )
                                        }
                                        
                                        goalRepository.insertGoals(domainGoals)
                                        onGoalsConfirmed()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
            
            if (proposedGoals == null) {
                PrimaryButton(
                    text = "Continue",
                    onClick = {
                        if (inputText.isNotBlank()) {
                            isProcessing = true
                            errorMessage = null
                            
                            scope.launch {
                                val response = grokService.parseGoalInput(inputText)
                                isProcessing = false
                                
                                when (response.type) {
                                    GrokResponseType.GOALS_PROPOSAL -> {
                                        proposedGoals = response.proposedGoals
                                    }
                                    GrokResponseType.UNAVAILABLE -> {
                                        errorMessage = response.message
                                    }
                                    else -> {
                                        errorMessage = "Could not parse your goals. Please use the form instead."
                                    }
                                }
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isProcessing
                )
            }
        }
    }
}

@Composable
private fun ProposedGoalItem(goal: ProposedGoal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "${CurrencyFormatter.formatIndianRupees(goal.targetAmount)} by ${goal.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        Text(
            text = "${goal.suggestedShare}%",
            style = MaterialTheme.typography.titleMedium,
            color = NavyPrimary
        )
    }
}

private fun parseTargetDate(dateStr: String): LocalDate {
    return try {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        LocalDate.parse("1 $dateStr", DateTimeFormatter.ofPattern("d MMMM yyyy"))
            .withDayOfMonth(1)
            .plusMonths(1)
            .minusDays(1)
    } catch (e: Exception) {
        LocalDate.now().plusYears(1)
    }
}
