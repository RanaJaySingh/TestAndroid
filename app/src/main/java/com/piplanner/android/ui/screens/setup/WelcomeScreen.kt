package com.piplanner.android.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piplanner.android.PiPlannerApplication
import com.piplanner.android.ui.components.PrimaryButton
import com.piplanner.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onSetupClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            PiPlannerApplication.instance.accountRepository.initializeDemoAccounts()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .background(NavyPrimary)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PiPlanner",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextOnPrimary.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "PIPLANNER",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextOnPrimary.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Every Rupee Has a Plan.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextOnPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Every credit to savings gets a goal before it can be spent.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnPrimary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Powered by Grok",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextOnPrimary.copy(alpha = 0.5f)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            HowItWorksStep(
                number = 1,
                icon = Icons.Outlined.AccountBalance,
                title = "Pick a savings account",
                description = "Pick a savings account that you will use exclusively for goals."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HowItWorksStep(
                number = 2,
                icon = Icons.Outlined.Flag,
                title = "Set your goals",
                description = "Tell us your goals and we'll calculate how much you need to save."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HowItWorksStep(
                number = 3,
                icon = Icons.Outlined.CallSplit,
                title = "Split every new credit",
                description = "Every credit to that savings account will be split across your goals."
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryButton(
                text = "Set up savings",
                onClick = onSetupClick
            )
        }
    }
}

@Composable
private fun HowItWorksStep(
    number: Int,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NavyPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NavyPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

private val sp = androidx.compose.ui.unit.sp
