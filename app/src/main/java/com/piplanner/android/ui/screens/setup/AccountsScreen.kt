package com.piplanner.android.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.domain.model.Account
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.Background
import com.piplanner.android.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val accounts by accountRepository.getAllAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    
    val hasDedicatedAccount = accounts.any { it.isDedicatedSavings }
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Accounts",
                onBackClick = onBack,
                stepIndicator = { StepIndicator(currentStep = 1, totalSteps = 3) }
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
            Text(
                text = "Your accounts",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Pick one for dedicated savings.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                accounts.forEach { account ->
                    AccountCard(
                        account = account,
                        showDedicatedToggle = true,
                        onDedicatedToggle = { enabled ->
                            scope.launch {
                                if (enabled) {
                                    accountRepository.setDedicatedSavings(account.id)
                                } else {
                                    accountRepository.setDedicatedSavings("")
                                }
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = hasDedicatedAccount
            )
        }
    }
}
