package com.piplanner.android.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.domain.model.HistoryEntry
import com.piplanner.android.domain.model.HistoryEntryType
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CurrencyFormatter
import com.piplanner.android.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val historyRepository = PiPlannerApplication.instance.historyRepository
    val entries by historyRepository.getAllEntries().collectAsStateWithLifecycle(initialValue = emptyList())
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(title = "History")
        }
    ) { paddingValues ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No history yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your transactions will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(paddingValues),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    HistoryEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: HistoryEntry) {
    val (icon, iconColor, amountColor, amountPrefix) = when (entry.type) {
        HistoryEntryType.OPENING_BALANCE -> Quadruple(
            Icons.Outlined.AccountBalance,
            NavyPrimary,
            NavyPrimary,
            ""
        )
        HistoryEntryType.NEW_CREDIT -> Quadruple(
            Icons.Outlined.Add,
            StatusGreen,
            StatusGreen,
            "+"
        )
        HistoryEntryType.CUSTOM_SPLIT -> Quadruple(
            Icons.Outlined.CallSplit,
            NavyPrimary,
            StatusGreen,
            "+"
        )
        HistoryEntryType.TYPED_ENTRY -> Quadruple(
            Icons.Outlined.Edit,
            NavyPrimary,
            StatusGreen,
            "+"
        )
        HistoryEntryType.TRANSFER -> Quadruple(
            Icons.Outlined.SwapHoriz,
            NavyPrimary,
            NavyPrimary,
            ""
        )
        HistoryEntryType.WITHDRAWAL -> Quadruple(
            Icons.Outlined.RemoveCircleOutline,
            StatusRed,
            StatusRed,
            "-"
        )
        HistoryEntryType.GOAL_DELETED -> Quadruple(
            Icons.Outlined.Delete,
            StatusAmber,
            StatusAmber,
            ""
        )
    }
    
    PiPlannerCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.type.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$amountPrefix${CurrencyFormatter.formatIndianRupees(entry.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = amountColor
                        )
                        
                        if (entry.isLocked) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = DateUtils.formatRelativeTime(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                if (entry.splits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    entry.splits.forEach { split ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${split.goalName} (${split.percentage}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatIndianRupees(split.amount),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                entry.description?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
