package com.piplanner.android.ui.screens.balance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.ui.components.*
import com.piplanner.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiPinScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onWrongPin: (() -> Unit)? = null
) {
    val accountRepository = PiPlannerApplication.instance.accountRepository
    val dedicatedAccount by accountRepository.getDedicatedSavingsAccount().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    
    var pin by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    
    val demoPIN = "1234"
    
    Scaffold(
        topBar = {
            PiPlannerTopBar(
                title = "Check balance",
                onBackClick = onBack
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
            Spacer(modifier = Modifier.height(24.dp))
            
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
                        
                        Column {
                            Text(
                                text = "${account.bankName} Bank ••${account.maskedNumber}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Balance check only, no debit",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Enter your UPI PIN",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Demo PIN: 1234",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            
            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wrong PIN. Try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusRed
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, if (index < pin.length) NavyPrimary else CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < pin.length) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isVerifying) {
                CircularProgressIndicator(color = NavyPrimary)
            } else {
                NumPad(
                    onDigit = { digit ->
                        if (pin.length < 4) {
                            pin += digit
                            showError = false
                            if (pin.length == 4) {
                                isVerifying = true
                                scope.launch {
                                    delay(1500)
                                    if (pin == demoPIN) {
                                        onSuccess()
                                    } else {
                                        isVerifying = false
                                        showError = true
                                        pin = ""
                                        onWrongPin?.invoke()
                                    }
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = onBack) {
                Text(
                    text = "Cancel",
                    color = NavyPrimary
                )
            }
        }
    }
}

@Composable
private fun NumPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        buttons.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                row.forEach { button ->
                    if (button.isEmpty()) {
                        Spacer(modifier = Modifier.size(72.dp))
                    } else if (button == "⌫") {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable { onBackspace() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Backspace,
                                contentDescription = "Backspace",
                                tint = NavyPrimary
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Surface)
                                .border(1.dp, CardBorder, CircleShape)
                                .clickable { onDigit(button) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = button,
                                style = MaterialTheme.typography.headlineMedium,
                                color = NavyPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private val Icons.Outlined.AccountBalance: androidx.compose.ui.graphics.vector.ImageVector
    get() = androidx.compose.material.icons.Icons.Outlined.AccountBalance
