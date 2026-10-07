package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * Goals tab — design frames 9 / 9b / 9c / 11 (PIP-46).
 * Sync / Update sheets are hooks only (PIP-48 owns full credit entry).
 * Goal detail is navigated via [onOpenGoal] (stub or PIP-50 route).
 */
@Composable
fun GoalsTab(
    viewModel: GoalsViewModel,
    onOpenGoal: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.selectedGoalId) {
        val goalId = uiState.selectedGoalId ?: return@LaunchedEffect
        viewModel.consumeSelectedGoal()
        onOpenGoal(goalId)
    }

    LaunchedEffect(uiState.navigateToSettings) {
        if (uiState.navigateToSettings) {
            viewModel.consumeSettingsNavigation()
            onOpenSettings()
        }
    }

    GoalsTabContent(
        uiState = uiState,
        formattedSavedAmount = viewModel::formattedSavedAmount,
        statusLabel = viewModel::statusLabel,
        onBalanceAction = viewModel::tapBalanceAction,
        onGoalClick = { goalId -> viewModel.selectGoal(goalId) },
        onSettingsClick = viewModel::openSettings,
        onDismissError = viewModel::clearError,
        onDismissSync = viewModel::dismissSyncSheet,
        onConfirmSync = viewModel::performSync,
        onDismissUpdateBalance = viewModel::dismissUpdateBalanceSheet,
        onApplyManualBalance = viewModel::applyManualBalance,
    )
}

@Composable
fun GoalsTabContent(
    uiState: GoalsUiState,
    formattedSavedAmount: (com.piplanner.data.model.Goal) -> String,
    statusLabel: (com.piplanner.data.model.Goal) -> String,
    onBalanceAction: () -> Unit,
    onGoalClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onDismissError: () -> Unit,
    onDismissSync: () -> Unit,
    onConfirmSync: () -> Unit,
    onDismissUpdateBalance: () -> Unit,
    onApplyManualBalance: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Goals tab" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.goals_tab_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.semantics { contentDescription = "Settings" },
            ) {
                Text(
                    text = "⚙",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BalanceCard(
                formattedTotal = uiState.formattedTotalSavings,
                accountSubtitle = uiState.dedicatedAccountSubtitle,
                actionTitle = uiState.balanceActionTitle,
                onAction = onBalanceAction,
            )

            when {
                uiState.isLoading && !uiState.hasGoals -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(24.dp),
                    )
                }
                uiState.hasGoals -> {
                    Text(
                        text = stringResource(R.string.goals_section_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics {
                            contentDescription = "Your goals section"
                        },
                    )
                    uiState.goals.forEach { goal ->
                        GoalCard(
                            name = goal.name,
                            formattedSaved = formattedSavedAmount(goal),
                            statusLabel = statusLabel(goal),
                            onClick = { onGoalClick(goal.id) },
                            modifier = Modifier.semantics {
                                contentDescription = "Goal card ${goal.name}"
                            },
                        )
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.goals_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "No goals yet"
                        },
                    )
                }
            }
        }
    }

    if (uiState.showSyncSheet) {
        GoalsSyncSheet(
            isSyncing = uiState.isSyncing,
            onSync = onConfirmSync,
            onDismiss = onDismissSync,
        )
    }

    if (uiState.showUpdateBalanceSheet) {
        GoalsUpdateBalanceSheet(
            currentFormatted = uiState.formattedTotalSavings,
            onApply = onApplyManualBalance,
            onDismiss = onDismissUpdateBalance,
        )
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.goals_error_title)) },
            text = { Text(uiState.errorMessage) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}
