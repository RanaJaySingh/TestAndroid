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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceFetchedScreen(
    onContinue: () -> Unit
) {
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Balance",
                stepIndicator = { StepIndicator(currentStep = 2, totalSteps = 3) }
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
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(StatusGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = TextOnPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Balance fetched",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            dedicatedAccount?.let { account ->
                Text(
                    text = CurrencyFormatter.formatIndianRupees(account.balance),
                    style = MaterialTheme.typography.displaySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${account.bankName} ••${account.maskedNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryButton(
                text = "Continue",
                onClick = onContinue
            )
        }
    }
}
