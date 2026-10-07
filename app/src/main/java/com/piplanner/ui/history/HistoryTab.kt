package com.piplanner.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * History tab — design frames 12 (list) and navigation into open/locked detail (12a).
 */
@Composable
fun HistoryTab(
    viewModel: HistoryViewModel,
    onOpenCreditEntry: (String) -> Unit,
    onOpenOpeningEntry: (String) -> Unit,
    onOpenLockedDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.navigateToCreditEntryId) {
        val entryId = uiState.navigateToCreditEntryId ?: return@LaunchedEffect
        viewModel.consumeCreditEntryNavigation()
        onOpenCreditEntry(entryId)
    }

    LaunchedEffect(uiState.navigateToOpeningEntryId) {
        val entryId = uiState.navigateToOpeningEntryId ?: return@LaunchedEffect
        viewModel.consumeOpeningEntryNavigation()
        onOpenOpeningEntry(entryId)
    }

    LaunchedEffect(uiState.navigateToLockedDetailId) {
        val entryId = uiState.navigateToLockedDetailId ?: return@LaunchedEffect
        viewModel.consumeLockedDetailNavigation()
        onOpenLockedDetail(entryId)
    }

    HistoryTabContent(
        uiState = uiState,
        onEntryClick = viewModel::onEntryClick,
    )
}

@Composable
fun HistoryTabContent(
    uiState: HistoryUiState,
    onEntryClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "History tab" },
    ) {
        Text(
            text = stringResource(R.string.history_tab_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )

        when {
            uiState.isLoading && uiState.rows.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = "History loading"
                        },
                    )
                }
            }
            uiState.isEmpty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .semantics { contentDescription = "History empty state" },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.history_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = HistoryService.EMPTY_STATE_BODY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "History entry list" },
                ) {
                    items(
                        items = uiState.rows,
                        key = { it.id },
                    ) { row ->
                        HistoryEntryRow(
                            row = row,
                            onClick = { onEntryClick(row.id) },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}
