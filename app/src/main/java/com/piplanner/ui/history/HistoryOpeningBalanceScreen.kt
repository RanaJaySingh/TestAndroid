package com.piplanner.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.ui.setup.OpeningSplitContent
import com.piplanner.ui.setup.OpeningSplitViewModel

/**
 * History → Opening balance (always read-only, frame 12a).
 */
@Composable
fun HistoryOpeningBalanceScreen(
    entryId: String,
    viewModel: OpeningSplitViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(entryId) {
        viewModel.loadLockedOpeningFromHistory(entryId)
    }

    if (uiState.lockedEntry == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Opening balance history loading" },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    OpeningSplitContent(
        uiState = uiState,
        formattedAmount = viewModel::formattedAmount,
        onPercentChange = viewModel::setDisplayPercent,
        onLockClick = viewModel::requestLock,
        onConfirmLock = viewModel::confirmLock,
        onDismissConfirm = viewModel::dismissConfirmLock,
        onDismissError = viewModel::clearError,
        onBack = onBack,
    )
}
