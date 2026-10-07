package com.piplanner.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.domain.CreditEntryService
import com.piplanner.domain.OpeningSplitService
import com.piplanner.ui.components.EntryBadge
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Open / locked New credit History entry (frames 13 / 13a–13g / 13t) — PIP-86 visual parity.
 *
 * Visual / layout / token / component only. Lock / assign behaviour stays in
 * [CreditEntryViewModel] / [CreditEntryService] (unchanged).
 */
@Composable
fun CreditEntryScreen(
    viewModel: CreditEntryViewModel,
    onDone: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            viewModel.consumeNavigateBack()
            onDone()
        }
    }

    CreditEntryContent(
        uiState = uiState,
        formattedSavedSoFar = viewModel::formattedSavedSoFar,
        formattedAmount = viewModel::formattedAmount,
        onPercentChange = viewModel::setDisplayPercent,
        onUseThisSplitChange = viewModel::setUseThisSplitForStanding,
        onSave = viewModel::saveAndLock,
        onDone = onDone,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun CreditEntryContent(
    uiState: CreditEntryUiState,
    formattedSavedSoFar: (Goal) -> String,
    formattedAmount: (String) -> String,
    onPercentChange: (String, Int) -> Unit,
    onUseThisSplitChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
    onDismissError: () -> Unit,
) {
    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PiPlannerColors.BackgroundApp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = PiPlannerColors.NavyPrimary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space16)
            .semantics { contentDescription = "Credit entry screen" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
    ) {
        CreditEntryHeader(uiState = uiState)

        PiCard(contentDescription = "Credit entry amount card") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                if (!uiState.isTyped && uiState.formattedPrevious != null) {
                    LabeledAmountRow(
                        label = stringResource(R.string.credit_entry_previous),
                        value = uiState.formattedPrevious,
                    )
                }
                if (!uiState.isTyped && uiState.formattedNewBalance != null) {
                    LabeledAmountRow(
                        label = stringResource(R.string.credit_entry_balance_now),
                        value = uiState.formattedNewBalance,
                    )
                }
                LabeledAmountRow(
                    label = stringResource(R.string.credit_entry_new_amount),
                    value = uiState.formattedCreditAmount,
                    emphasize = true,
                )
            }
        }

        PiCard(contentDescription = "Already saved block") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
                Text(
                    text = stringResource(R.string.credit_entry_already_saved),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Already saved header"
                    },
                )
                uiState.goals.forEach { goal ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = PiPlannerColors.OnSurface,
                        )
                        Text(
                            text = formattedSavedSoFar(goal),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        PiCard(contentDescription = "This credit split block") {
            Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16)) {
                Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                    Text(
                        text = stringResource(
                            R.string.credit_entry_this_credit_split,
                            uiState.formattedCreditAmount,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PiPlannerColors.NavyPrimary,
                        modifier = Modifier.semantics {
                            contentDescription = "This credit header"
                        },
                    )
                    if (!uiState.isLocked) {
                        Text(
                            text = stringResource(R.string.credit_entry_update_percentages),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                uiState.goals.forEach { goal ->
                    CreditGoalSplitRow(
                        goal = goal,
                        thisCreditAmount = formattedAmount(goal.id),
                        percent = uiState.displayPercents[goal.id] ?: 0,
                        isSingleGoal = uiState.isSingleGoal,
                        isLocked = uiState.isLocked,
                        onPercentChange = { onPercentChange(goal.id, it) },
                    )
                }
            }
        }

        if (!uiState.isLocked && !uiState.isSingleGoal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Use this split checkbox" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = uiState.useThisSplitForStanding,
                    onCheckedChange = onUseThisSplitChange,
                )
                Text(
                    text = CreditEntryService.USE_THIS_SPLIT_CHECKBOX_TITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PiPlannerColors.OnSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = "Credit entry status" },
        )

        if (!uiState.isLocked) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = "Save and lock" },
                    color = PiPlannerColors.NavyPrimary,
                )
            } else {
                PrimaryCta(
                    text = stringResource(R.string.credit_entry_save_lock),
                    onClick = onSave,
                    enabled = uiState.canSave,
                    contentDescription = "Save and lock",
                )
            }
        } else {
            PrimaryCta(
                text = stringResource(R.string.done),
                onClick = onDone,
                contentDescription = "Credit entry done",
            )
        }

        Spacer(modifier = Modifier.height(PiPlannerDimens.Space8))
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.credit_entry_save_failed)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun CreditEntryHeader(uiState: CreditEntryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
        Text(
            text = stringResource(R.string.credit_entry_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            color = PiPlannerColors.NavyPrimary.copy(alpha = 0.72f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.isLocked) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Icon(
                        imageVector = PiIcons.lock,
                        contentDescription = stringResource(R.string.credit_entry_locked_a11y),
                        tint = PiPlannerColors.NavyPrimary,
                        modifier = Modifier
                            .size(22.dp)
                            .semantics { contentDescription = "Credit entry lock icon" },
                    )
                    Text(
                        text = stringResource(R.string.credit_entry_saved_and_locked),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PiPlannerColors.NavyPrimary,
                        modifier = Modifier.semantics {
                            contentDescription = "Saved and locked"
                        },
                    )
                }
            } else {
                Text(
                    text = CreditEntryService.ASSIGN_NOW_TITLE,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.NavyPrimary,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .semantics { contentDescription = "Assign now" },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
                if (uiState.isTyped) {
                    EntryBadge(
                        label = stringResource(R.string.credit_entry_typed_badge),
                        contentDescription = "Typed credit badge",
                    )
                }
                if (uiState.isLocked && !uiState.isTyped) {
                    EntryBadge(
                        label = stringResource(R.string.credit_entry_custom_badge),
                        contentDescription = "Custom credit badge",
                    )
                }
            }
        }
        Text(
            text = if (uiState.isLocked) {
                OpeningSplitService.LOCKED_AMOUNTS_CAPTION
            } else {
                CreditEntryService.LOCKED_ONCE_CAPTION
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LabeledAmountRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) PiPlannerColors.NavyPrimary else PiPlannerColors.OnSurface,
        )
    }
}

@Composable
private fun CreditGoalSplitRow(
    goal: Goal,
    thisCreditAmount: String,
    percent: Int,
    isSingleGoal: Boolean,
    isLocked: Boolean,
    onPercentChange: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
        modifier = Modifier
            .padding(vertical = PiPlannerDimens.Space8)
            .semantics { contentDescription = "Credit split ${goal.name}" },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = thisCreditAmount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.NavyPrimary,
            )
        }
        when {
            isSingleGoal -> {
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = PiPlannerColors.NavyPrimary,
                    modifier = Modifier.semantics {
                        contentDescription = "${goal.name} automatically assigned 100 percent"
                    },
                )
            }
            isLocked -> {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = PiPlannerColors.NavyPrimary,
                )
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.share_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = percent.toString(),
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }
                                onPercentChange(digits.toIntOrNull() ?: 0)
                            },
                            modifier = Modifier
                                .width(88.dp)
                                .semantics {
                                    contentDescription = "${goal.name} percentage"
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.titleMedium,
                            color = PiPlannerColors.NavyPrimary,
                            modifier = Modifier.padding(start = PiPlannerDimens.Space8),
                        )
                    }
                }
            }
        }
    }
}
