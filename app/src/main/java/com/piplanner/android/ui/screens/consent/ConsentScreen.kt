package com.piplanner.android.ui.screens.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(
    onBack: () -> Unit,
    onYesAuto: () -> Unit,
    onNoManual: () -> Unit
) {
    val preferencesDataStore = PiPlannerApplication.instance.preferencesDataStore
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Accounts",
                onBackClick = onBack
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
            PiPlannerCard {
                Column {
                    Text(
                        text = "Allow PiPlanner to check this balance?",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    dedicatedAccount?.let { account ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Savings · ${account.bankName} ••${account.maskedNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ConsentItem(
                        icon = Icons.Outlined.Visibility,
                        text = "Reads only this balance",
                        isPositive = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ConsentItem(
                        icon = Icons.Outlined.Storage,
                        text = "Stores the last balance to find what is new",
                        isPositive = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ConsentItem(
                        icon = Icons.Outlined.Receipt,
                        text = "Never accesses transactions",
                        isPositive = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ConsentItem(
                        icon = Icons.Outlined.SwapHoriz,
                        text = "Never makes transfers",
                        isPositive = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ConsentItem(
                        icon = Icons.Outlined.Settings,
                        text = "Change it any time in Settings",
                        isPositive = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryButton(
                text = "Yes, update automatically",
                onClick = {
                    scope.launch {
                        preferencesDataStore.setAutoBalanceUpdates(true)
                        onYesAuto()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SecondaryButton(
                text = "No, I'll update it myself",
                onClick = {
                    scope.launch {
                        preferencesDataStore.setAutoBalanceUpdates(false)
                        onNoManual()
                    }
                }
            )
        }
    }
}

@Composable
private fun ConsentItem(
    icon: ImageVector,
    text: String,
    isPositive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isPositive) Icons.Outlined.Check else Icons.Outlined.Close,
            contentDescription = null,
            tint = StatusGreen,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
