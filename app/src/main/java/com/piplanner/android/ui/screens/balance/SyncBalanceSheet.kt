package com.piplanner.android.ui.screens.balance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncBalanceSheet(
    onDismiss: () -> Unit,
    onBalanceUpdated: () -> Unit
) {
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val preferencesDataStore = PiPlannerApplication.instance.preferencesDataStore
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var previousBalance by remember { mutableLongStateOf(0L) }
    var newBalance by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(dedicatedAccount) {
        dedicatedAccount?.let { account ->
            previousBalance = account.balance
            delay(1500)
            newBalance = account.balance + 15000
            accountRepository.updateBalance(account.id, newBalance)
            preferencesDataStore.updateLastSyncTimestamp(System.currentTimeMillis())
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Sync",
                onBackClick = onDismiss
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Spacer(modifier = Modifier.height(48.dp))
                
                CircularProgressIndicator(
                    color = NavyPrimary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Checking balance...",
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                PiPlannerCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Balance updated",
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Previous",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatIndianRupees(previousBalance),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fetched",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatIndianRupees(newBalance),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Divider()
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "New amount",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "+${CurrencyFormatter.formatIndianRupees(newBalance - previousBalance)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusGreen
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                PrimaryButton(
                    text = "Continue",
                    onClick = onBalanceUpdated
                )
            }
        }
    }
}
