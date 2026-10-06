package com.piplanner.android.ui.screens.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.domain.model.Goal
import com.piplanner.android.domain.model.GrokResponse
import com.piplanner.android.domain.model.GrokResponseType
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(
    onTransferConfirm: (String, String) -> Unit
) {
    val grokService = PiPlannerApplication.instance.grokService
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val goals by goalRepository.getAllGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var response by remember { mutableStateOf<GrokResponse?>(null) }
    
    val quickChips = listOf(
        "How are my goals doing?",
        "Transfer between goals",
        "Add a new goal"
    )
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(title = "Ask")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Ask Grok",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Get help managing your savings goals",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Powered by Grok",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Quick actions",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickChips.forEach { chip ->
                        SuggestionChip(
                            onClick = {
                                inputText = chip
                            },
                            label = { Text(chip, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
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
                
                response?.let { grokResponse ->
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    when (grokResponse.type) {
                        GrokResponseType.PLAIN_ANSWER -> {
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
                                        text = "Grok's answer",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = grokResponse.message ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        GrokResponseType.TRANSFER_PROPOSAL -> {
                            grokResponse.transferSuggestion?.let { suggestion ->
                                val fromGoal = goals.find { it.id == suggestion.fromGoalId }
                                val toGoal = goals.find { it.id == suggestion.toGoalId }
                                
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
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = "Move ${CurrencyFormatter.formatIndianRupees(suggestion.amount)} from ${fromGoal?.name ?: "Unknown"} to ${toGoal?.name ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = suggestion.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { response = null },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Edit")
                                        }
                                        
                                        Button(
                                            onClick = {
                                                onTransferConfirm(suggestion.fromGoalId, suggestion.toGoalId)
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
                        
                        GrokResponseType.UNAVAILABLE -> {
                            PiPlannerCard {
                                Text(
                                    text = grokResponse.message ?: "Grok is temporarily unavailable.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusAmber
                                )
                            }
                        }
                        
                        else -> {}
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What would you like to do?") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isProcessing) {
                                isProcessing = true
                                response = null
                                
                                scope.launch {
                                    response = grokService.handleAskQuery(inputText, goals)
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) NavyPrimary else TextSecondary
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = CardBorder
                ),
                singleLine = true
            )
        }
    }
}
