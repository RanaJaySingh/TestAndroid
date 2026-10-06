package com.piplanner.android.ui.screens.balance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import com.piplanner.android.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualBalanceScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Opening balance",
                onBackClick = onBack,
                stepIndicator = { StepIndicator(currentStep = 2, totalSteps = 3) }
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
                text = "How much is in this account?",
                style = MaterialTheme.typography.headlineSmall
            )
            
            dedicatedAccount?.let { account ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Savings · ${account.bankName} ••${account.maskedNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.displayMedium,
                    color = NavyPrimary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = if (amount > 0) CurrencyFormatter.formatIndianNumber(amount) else "",
                    onValueChange = { newValue ->
                        amountText = newValue.filter { it.isDigit() }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.displayMedium.copy(color = NavyPrimary),
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.displayMedium,
                            color = TextSecondary.copy(alpha = 0.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = CardBorder
                    )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryButton(
                text = "Continue",
                onClick = {
                    scope.launch {
                        dedicatedAccount?.let { account ->
                            accountRepository.updateBalance(account.id, amount)
                        }
                        onContinue()
                    }
                },
                enabled = amount > 0
            )
        }
    }
}
