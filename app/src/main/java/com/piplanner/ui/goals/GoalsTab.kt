package com.piplanner.ui.goals

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.domain.GoalsTabService
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Goals tab — design frames 9 / 9b / 9c / 11 with Sync/Update credit entry (PIP-48)
 * and navy home visual chrome (PIP-82).
 */
@Composable
fun GoalsTab(
    viewModel: GoalsViewModel,
    onOpenGoal: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreditEntry: (String) -> Unit,
    onOpenWithdrawal: (previousPaisa: Long, newPaisa: Long, isTyped: Boolean) -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenStandingSplit: () -> Unit = {},
    onOpenTransfer: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenNewGoal: () -> Unit = {},
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

    LaunchedEffect(uiState.navigateToCreditEntryId) {
        val entryId = uiState.navigateToCreditEntryId ?: return@LaunchedEffect
        viewModel.consumeCreditEntryNavigation()
        onOpenCreditEntry(entryId)
    }

    LaunchedEffect(
        uiState.navigateToWithdrawalPreviousPaisa,
        uiState.navigateToWithdrawalNewPaisa,
    ) {
        val previous = uiState.navigateToWithdrawalPreviousPaisa ?: return@LaunchedEffect
        val newBalance = uiState.navigateToWithdrawalNewPaisa ?: return@LaunchedEffect
        val isTyped = uiState.navigateToWithdrawalIsTyped
        viewModel.consumeWithdrawalNavigation()
        onOpenWithdrawal(previous, newBalance, isTyped)
    }

    GoalsTabContent(
        uiState = uiState,
        formattedSavedOfTarget = viewModel::formattedSavedOfTarget,
        statusLabel = viewModel::statusLabel,
        monthlyNeedLabel = viewModel::monthlyNeedLabel,
        creditsPercentLabel = viewModel::creditsPercentLabel,
        onBalanceAction = viewModel::tapBalanceAction,
        onGoalClick = { goalId -> viewModel.selectGoal(goalId) },
        onSettingsClick = viewModel::openSettings,
        onNewGoalClick = onOpenNewGoal,
        onTransferClick = onOpenTransfer,
        onHistoryClick = onOpenHistory,
        onRecordWithdrawalClick = viewModel::openRecordWithdrawal,
        onDismissError = viewModel::clearError,
        onDismissSync = viewModel::dismissSyncSheet,
        onConfirmSync = viewModel::performSync,
        onContinueCreditFromSync = viewModel::openCreditEntryFromSheet,
        onContinueWithdrawalFromSync = viewModel::continueToWithdrawal,
        onDismissUpdateBalance = viewModel::dismissUpdateBalanceSheet,
        onApplyManualBalance = viewModel::applyManualBalance,
        onUpdateBalanceSync = viewModel::performUpdateBalanceSync,
        onContinueCreditFromUpdate = viewModel::openCreditEntryFromSheet,
        onContinueWithdrawalFromUpdate = viewModel::continueToWithdrawal,
        onAssignNow = viewModel::openCreditEntryFromBanner,
        onDismissRecordWithdrawal = viewModel::dismissRecordWithdrawalSheet,
        onContinueRecordWithdrawal = viewModel::continueRecordWithdrawal,
    )
}

@Composable
fun GoalsTabContent(
    uiState: GoalsUiState,
    formattedSavedOfTarget: (com.piplanner.data.model.Goal) -> String,
    statusLabel: (com.piplanner.data.model.Goal) -> String,
    monthlyNeedLabel: (com.piplanner.data.model.Goal) -> String,
    creditsPercentLabel: (com.piplanner.data.model.Goal) -> String,
    onBalanceAction: () -> Unit,
    onGoalClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onNewGoalClick: () -> Unit = {},
    onTransferClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onRecordWithdrawalClick: () -> Unit = {},
    onDismissError: () -> Unit,
    onDismissSync: () -> Unit,
    onConfirmSync: () -> Unit,
    onContinueCreditFromSync: () -> Unit,
    onContinueWithdrawalFromSync: () -> Unit,
    onDismissUpdateBalance: () -> Unit,
    onApplyManualBalance: (Long) -> Unit,
    onUpdateBalanceSync: () -> Unit = {},
    onContinueCreditFromUpdate: () -> Unit,
    onContinueWithdrawalFromUpdate: () -> Unit,
    onAssignNow: () -> Unit,
    onDismissRecordWithdrawal: () -> Unit = {},
    onContinueRecordWithdrawal: (Long) -> Unit = {},
) {
    val balanceActionEnabled = !uiState.isSyncOrUpdateBlocked
    val quickBalanceTitle = GoalsTabService.quickBalanceActionTitle(uiState.balanceAction)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .semantics { contentDescription = "Goals tab" },
    ) {
        GoalsHomeHeader(
            greeting = uiState.greeting,
            onSettingsClick = onSettingsClick,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PiPlannerDimens.Space16)
                .padding(bottom = PiPlannerDimens.Space24),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
        ) {
            uiState.openEntryBannerMessage?.let { message ->
                OpenEntryBanner(
                    message = message,
                    onAssignNow = onAssignNow,
                )
            }

            BalanceCard(
                formattedTotal = uiState.formattedTotalSavings,
                accountSubtitle = uiState.dedicatedAccountSubtitle,
                lastActivityLine = uiState.lastActivityLine,
                actionTitle = uiState.balanceActionTitle,
                actionEnabled = balanceActionEnabled,
                onAction = onBalanceAction,
            )

            QuickActionRow(
                balanceActionTitle = quickBalanceTitle,
                balanceActionEnabled = balanceActionEnabled,
                transferEnabled = uiState.goals.size >= 2,
                onBalanceAction = onBalanceAction,
                onNewGoal = onNewGoalClick,
                onTransfer = onTransferClick,
                onHistory = onHistoryClick,
            )

            TextButton(
                onClick = onRecordWithdrawalClick,
                modifier = Modifier.semantics {
                    contentDescription = "Record a withdrawal"
                },
            ) {
                Text(
                    text = stringResource(R.string.withdrawal_record_cta),
                    color = PiPlannerColors.NavyPrimary,
                    style = PiPlannerTypography.caption,
                )
            }

            when {
                uiState.isLoading && !uiState.hasGoals -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(PiPlannerDimens.Space24),
                        color = PiPlannerColors.NavyPrimary,
                    )
                }
                uiState.hasGoals -> {
                    Text(
                        text = stringResource(R.string.goals_section_header),
                        style = PiPlannerTypography.title,
                        fontWeight = FontWeight.SemiBold,
                        color = PiPlannerColors.OnSurface,
                        modifier = Modifier.semantics {
                            contentDescription = "Your goals section"
                        },
                    )
                    uiState.goals.forEach { goal ->
                        GoalCard(
                            name = goal.name,
                            formattedSavedOfTarget = formattedSavedOfTarget(goal),
                            statusLabel = statusLabel(goal),
                            monthlyNeedLabel = monthlyNeedLabel(goal),
                            creditsPercentLabel = creditsPercentLabel(goal),
                            onClick = { onGoalClick(goal.id) },
                        )
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.goals_empty),
                        style = PiPlannerTypography.body,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.62f),
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
            phase = uiState.syncPhase,
            formattedPrevious = uiState.formattedPreviousBalance,
            formattedFetched = uiState.formattedFetchedBalance,
            formattedNewAmount = uiState.formattedNewCreditAmount,
            formattedWentDownBy = uiState.formattedWentDownBy,
            infoMessage = uiState.syncInfoMessage,
            errorMessage = uiState.syncErrorMessage,
            isBlockedByOpenEntry = uiState.isSyncOrUpdateBlocked,
            canContinueToCreditEntry = uiState.canContinueToCreditEntry,
            canContinueToWithdrawal = uiState.canContinueToWithdrawal,
            onSync = onConfirmSync,
            onContinueToCreditEntry = onContinueCreditFromSync,
            onContinueToWithdrawal = onContinueWithdrawalFromSync,
            onDismiss = onDismissSync,
        )
    }

    if (uiState.showUpdateBalanceSheet) {
        GoalsUpdateBalanceSheet(
            currentFormatted = uiState.formattedTotalSavings,
            infoMessage = uiState.updateInfoMessage,
            errorMessage = uiState.updateErrorMessage,
            isBlockedByOpenEntry = uiState.isSyncOrUpdateBlocked,
            canContinueToCreditEntry = uiState.canContinueToCreditEntry,
            canContinueToWithdrawal = uiState.canContinueToWithdrawal,
            onApply = onApplyManualBalance,
            onBalanceSync = onUpdateBalanceSync,
            onContinueToCreditEntry = onContinueCreditFromUpdate,
            onContinueToWithdrawal = onContinueWithdrawalFromUpdate,
            onRecordWithdrawal = onRecordWithdrawalClick,
            onDismiss = onDismissUpdateBalance,
        )
    }

    if (uiState.showRecordWithdrawalSheet) {
        RecordWithdrawalSheet(
            currentFormatted = uiState.formattedTotalSavings,
            currentBalancePaisa = uiState.totalSavingsPaisa,
            onContinue = onContinueRecordWithdrawal,
            onDismiss = onDismissRecordWithdrawal,
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

/**
 * Goals header — greeting + chrome-only Search / notifications / chart + Settings gear
 * (A2 / R20 — no new flows from header icons).
 */
@Composable
private fun GoalsHomeHeader(
    greeting: String,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PiPlannerDimens.Space8, vertical = PiPlannerDimens.Space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = PiPlannerDimens.Space8),
        ) {
            Text(
                text = greeting,
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                modifier = Modifier.semantics {
                    contentDescription = "goals.greeting"
                },
            )
            Text(
                text = stringResource(R.string.goals_tab_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
        }
        HeaderChromeIcon(
            icon = PiIcons.headerSearch,
            label = "Search",
        )
        HeaderChromeIcon(
            icon = PiIcons.headerNotifications,
            label = "Notifications",
        )
        HeaderChromeIcon(
            icon = PiIcons.headerChart,
            label = "Chart",
        )
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.semantics { contentDescription = "Settings" },
        ) {
            Icon(
                imageVector = PiIcons.settings,
                contentDescription = "Settings",
                tint = PiPlannerColors.NavyPrimary.copy(alpha = 0.85f),
            )
        }
    }
}

/** Non-interactive header chrome icon (A2 / R20). */
@Composable
private fun HeaderChromeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = PiPlannerColors.NavyPrimary.copy(alpha = 0.85f),
        modifier = Modifier
            .padding(horizontal = PiPlannerDimens.Space8)
            .semantics {
                contentDescription = "goals.header.${label.lowercase()}"
            },
    )
}
