package com.piplanner.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onResetDemo: () -> Unit
) {
    val preferencesDataStore = PiPlannerApplication.instance.preferencesDataStore
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val goalRepository = PiPlannerApplication.instance.goalRepository
    val historyRepository = PiPlannerApplication.instance.historyRepository
    
    val preferences by preferencesDataStore.userPreferences.collectAsStateWithLifecycle(
        initialValue = com.piplanner.android.domain.model.UserPreferences()
    )
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    
    var showResetDialog by remember { mutableStateOf(false) }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset demo?") },
            text = { 
                Text("This will delete all goals, history, and reset to the initial demo state.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            goalRepository.deleteAllGoals()
                            historyRepository.deleteAllEntries()
                            accountRepository.deleteAllAccounts()
                            preferencesDataStore.resetAll()
                            accountRepository.initializeDemoAccounts()
                            showResetDialog = false
                            onResetDemo()
                        }
                    }
                ) {
                    Text("Reset", color = StatusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Settings",
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
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
            
            PiPlannerCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preferences.userName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Demo user",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Linked accounts",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
            
            dedicatedAccount?.let { account ->
                PiPlannerCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${account.bankName} ••${account.maskedNumber}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Dedicated savings",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusGreen
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Balance updates",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
            
            PiPlannerCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic balance updates",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Allow PiPlanner to check balance via UPI",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    
                    Switch(
                        checked = preferences.autoBalanceUpdates,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                preferencesDataStore.setAutoBalanceUpdates(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NavyPrimary
                        )
                    )
                }
            }
            
            Text(
                text = "Demo",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
            
            PiPlannerCard(
                onClick = { showResetDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = StatusRed,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reset demo",
                            style = MaterialTheme.typography.titleMedium,
                            color = StatusRed
                        )
                        Text(
                            text = "Start fresh with demo data",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PiPlanner v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Powered by Grok",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
