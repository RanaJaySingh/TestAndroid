package com.piplanner.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.domain.HistoryService

/**
 * Read-only History entry detail — Transfer / Withdrawal / Goal deleted (frame 12a copy).
 */
@Composable
fun HistoryEntryDetailScreen(
    viewModel: HistoryDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryEntryDetailContent(
        uiState = uiState,
        onBack = onBack,
    )
}

@Composable
fun HistoryEntryDetailContent(
    uiState: HistoryDetailUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .semantics { contentDescription = "History entry detail read-only" },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = "Back from history detail" },
        ) {
            Text(stringResource(R.string.back))
        }

        if (uiState.missing) {
            Text(
                text = stringResource(R.string.history_detail_missing),
                style = MaterialTheme.typography.bodyLarge,
            )
            return
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = uiState.typeIcon,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = uiState.typeLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = uiState.caption.ifBlank { HistoryService.ORIGINAL_AMOUNTS_CAPTION },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = HistoryService.ORIGINAL_AMOUNTS_CAPTION
            },
        )

        Text(
            text = uiState.formattedAmount,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                contentDescription = "History detail amount ${uiState.formattedAmount}"
            },
        )

        uiState.detailLines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (uiState.allocationRows.isNotEmpty()) {
            Text(
                text = stringResource(R.string.history_detail_allocations),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.allocationRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Allocation ${row.goalName} ${row.formattedAmount} ${row.percentLabel}"
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = row.goalName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = row.percentLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = row.formattedAmount,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
