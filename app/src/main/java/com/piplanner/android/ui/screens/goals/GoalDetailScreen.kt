package com.piplanner.android.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CurrencyFormatter
import com.piplanner.android.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTransfer: () -> Unit
) {
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val historyRepository = PiPlannerApplication.instance.historyRepository
    
    val goal by goalRepository.getGoalByIdFlow(goalId).collectAsStateWithLifecycle(initialValue = null)
    val historyEntries by historyRepository.getEntriesForGoal(goalId).collectAsStateWithLifecycle(initialValue = emptyList())
    
    goal?.let { currentGoal ->
        Scaffold(
            topBar = {
                PiPlannerTopBar(
                    title = currentGoal.name,
                    onBackClick = onBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PiPlannerCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.titleMedium
                            )
                            StatusChip(isOnTrack = currentGoal.isOnTrack)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = CurrencyFormatter.formatIndianRupees(currentGoal.savedAmount),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        
                        Text(
                            text = "of ${CurrencyFormatter.formatIndianRupees(currentGoal.targetWithInflation)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LinearProgressIndicator(
                            progress = { currentGoal.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = if (currentGoal.isOnTrack) StatusGreen else StatusAmber,
                            trackColor = Divider,
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${(currentGoal.progress * 100).toInt()}% complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                PiPlannerCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow(
                            label = "Needs/month",
                            value = CurrencyFormatter.formatIndianRupees(currentGoal.needsPerMonth)
                        )
                        
                        Divider()
                        
                        DetailRow(
                            label = "Target date",
                            value = DateUtils.formatFullDate(currentGoal.endDate)
                        )
                        
                        DetailRow(
                            label = "Start date",
                            value = DateUtils.formatFullDate(currentGoal.startDate)
                        )
                        
                        Divider()
                        
                        DetailRow(
                            label = "Inflation rate",
                            value = "${currentGoal.inflationRate.toInt()}%"
                        )
                        
                        DetailRow(
                            label = "Share of credits",
                            value = "${currentGoal.sharePercentage}%"
                        )
                        
                        currentGoal.pendingShareChange?.let { pending ->
                            Text(
                                text = "Share will change to $pending% at next credit",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusAmber
                            )
                        }
                    }
                }
                
                if (historyEntries.isNotEmpty()) {
                    Text(
                        text = "From History",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    historyEntries.take(3).forEach { entry ->
                        PiPlannerCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = entry.type.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = DateUtils.formatRelativeTime(entry.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                
                                val splitForGoal = entry.splits.find { it.goalId == goalId }
                                Text(
                                    text = "+${CurrencyFormatter.formatIndianRupees(splitForGoal?.amount ?: 0)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StatusGreen
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onTransfer,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transfer")
                    }
                    
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                }
                
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusRed)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete goal")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
