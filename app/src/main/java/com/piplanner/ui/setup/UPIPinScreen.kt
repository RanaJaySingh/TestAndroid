package com.piplanner.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R

/**
 * UPI PIN (Demo) — design frame 4b (PRD R4). Demo PIN `"1234"`.
 */
@Composable
fun UPIPinScreen(
    viewModel: ConsentViewModel,
    onSuccess: () -> Unit,
    onWrongPin: () -> Unit,
    onCancel: () -> Unit,
    onOtherApp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldShowFetchedBalance) {
        if (uiState.shouldShowFetchedBalance) {
            viewModel.consumeNavigation()
            onSuccess()
        }
    }

    LaunchedEffect(uiState.shouldShowWrongPin) {
        if (uiState.shouldShowWrongPin) {
            viewModel.consumeNavigation()
            onWrongPin()
        }
    }

    LaunchedEffect(uiState.shouldShowOtherApp) {
        if (uiState.shouldShowOtherApp) {
            viewModel.consumeNavigation()
            onOtherApp()
        }
    }

    UPIPinContent(
        uiState = uiState,
        onDigit = viewModel::appendPinDigit,
        onDelete = viewModel::deletePinDigit,
        onCheckBalance = viewModel::checkBalanceWithPin,
        onCancel = {
            viewModel.cancelPin()
            onCancel()
        },
        onOtherApp = viewModel::accountOnOtherUpiApp,
    )
}

@Composable
fun UPIPinContent(
    uiState: ConsentUiState,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onCheckBalance: () -> Unit,
    onCancel: () -> Unit,
    onOtherApp: () -> Unit,
) {
    val padRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { contentDescription = "UPI PIN screen" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.upi_pin_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.upi_pin_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.semantics {
                contentDescription = "PIN entered ${uiState.pinDigits.length} of 4 digits"
            },
        ) {
            repeat(4) { index ->
                val filled = index < uiState.pinDigits.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape,
                        )
                        .background(
                            if (filled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            padRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            OutlinedButton(
                                onClick = {
                                    if (key == "⌫") onDelete() else onDigit(key)
                                },
                                enabled = !uiState.isWorking,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .semantics {
                                        contentDescription = if (key == "⌫") "Delete" else key
                                    },
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onCheckBalance,
                enabled = uiState.canCheckPin,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Check balance" },
            ) {
                if (uiState.isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.upi_pin_check_balance),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !uiState.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Cancel" },
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        TextButton(
            onClick = onOtherApp,
            enabled = !uiState.isWorking,
            modifier = Modifier.semantics {
                contentDescription = "Account on another UPI app"
            },
        ) {
            Text(stringResource(R.string.upi_pin_other_app))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
