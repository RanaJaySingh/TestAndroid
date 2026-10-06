package com.piplanner.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.piplanner.android.domain.model.Account
import com.piplanner.android.domain.model.AccountType
import com.piplanner.android.ui.theme.CardBorder
import com.piplanner.android.ui.theme.NavyPrimary
import com.piplanner.android.ui.theme.Surface
import com.piplanner.android.ui.theme.TextSecondary
import com.piplanner.android.util.CurrencyFormatter

@Composable
fun AccountCard(
    account: Account,
    showDedicatedToggle: Boolean = false,
    onDedicatedToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, if (account.isDedicatedSavings) NavyPrimary else CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        text = CurrencyFormatter.formatIndianRupees(account.balance),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${account.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${account.bankName} ••${account.maskedNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            if (showDedicatedToggle && account.type == AccountType.SAVINGS) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dedicated savings",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Switch(
                        checked = account.isDedicatedSavings,
                        onCheckedChange = onDedicatedToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NavyPrimary
                        )
                    )
                }
            }
            
            if (account.type == AccountType.SPENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Everyday spend. Not split across goals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
