package com.piplanner.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CurrencyFormatter
import com.piplanner.android.util.DateUtils
import java.time.LocalDateTime

@Composable
fun BalanceCard(
    balance: Long,
    lastSynced: LocalDateTime?,
    onSyncClick: () -> Unit,
    onNewGoalClick: () -> Unit,
    onTransferClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Savings · HDFC ••4821",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnPrimary.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = CurrencyFormatter.formatIndianRupees(balance),
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextOnPrimary
                    )
                    
                    if (lastSynced != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Last synced ${DateUtils.formatRelativeTime(lastSynced)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnPrimary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Outlined.Sync,
                    label = "Sync",
                    onClick = onSyncClick
                )
                QuickActionButton(
                    icon = Icons.Outlined.Add,
                    label = "New goal",
                    onClick = onNewGoalClick
                )
                QuickActionButton(
                    icon = Icons.Outlined.SwapHoriz,
                    label = "Transfer",
                    onClick = onTransferClick
                )
                QuickActionButton(
                    icon = Icons.Outlined.History,
                    label = "History",
                    onClick = onHistoryClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = TextOnPrimary.copy(alpha = 0.1f),
                contentColor = TextOnPrimary
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextOnPrimary.copy(alpha = 0.8f)
        )
    }
}
