package com.piplanner.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.piplanner.domain.HistoryService
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * History tab — design frames 12 (list) and navigation into open/locked detail (12a).
 * PIP-92: list chrome only (tokens / PiCard rows / empty well); behaviour unchanged.
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
            .background(PiPlannerColors.BackgroundApp)
            .semantics { contentDescription = "History tab" },
    ) {
        Text(
            text = stringResource(R.string.history_tab_title),
            style = PiPlannerTypography.title,
            fontWeight = FontWeight.Bold,
            color = PiPlannerColors.OnBackground,
            modifier = Modifier.padding(
                horizontal = PiPlannerDimens.Space16,
                vertical = PiPlannerDimens.Space16,
            ),
        )

        when {
            uiState.isLoading && uiState.rows.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = PiPlannerColors.NavyPrimary,
                        modifier = Modifier.semantics {
                            contentDescription = "History loading"
                        },
                    )
                }
            }
            uiState.isEmpty -> {
                HistoryEmptyState()
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
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PiPlannerDimens.Space24)
            .semantics { contentDescription = "History empty state" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PiPlannerColors.ChipLightBlue.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PiIcons.historyTab,
                contentDescription = null,
                tint = PiPlannerColors.NavyPrimary.copy(alpha = 0.55f),
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = stringResource(R.string.history_empty_title),
            style = PiPlannerTypography.title,
            fontWeight = FontWeight.SemiBold,
            color = PiPlannerColors.OnBackground,
            modifier = Modifier.padding(top = PiPlannerDimens.Space16),
        )
        Text(
            text = HistoryService.EMPTY_STATE_BODY,
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface.copy(alpha = 0.62f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = PiPlannerDimens.Space8),
        )
    }
}
