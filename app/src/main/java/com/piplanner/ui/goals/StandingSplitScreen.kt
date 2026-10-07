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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.domain.StandingSplitService
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PiSheetHandle
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme

/**
 * Standing split screen — design frame 15 (PRD R14 visual).
 * Multi-goal % editor for the next credit; one-goal path skips this UI.
 * Visual/layout only — allocation logic unchanged (PIP-52).
 */
@Composable
fun StandingSplitScreen(
    viewModel: StandingSplitViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.shouldNavigateBack, uiState.shouldSkip) {
        if (uiState.shouldNavigateBack) {
            viewModel.consumeNavigation()
            onBack()
        }
    }

    StandingSplitContent(
        uiState = uiState,
        onPercentChange = viewModel::setDisplayPercent,
        onSave = viewModel::save,
        onBack = onBack,
        onDismissError = viewModel::clearError,
    )
}

@Composable
fun StandingSplitContent(
    uiState: StandingSplitUiState,
    onPercentChange: (String, Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    // One-goal skip: minimal shell while auto-apply navigates away.
    if (uiState.shouldSkip) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PiPlannerColors.BackgroundApp)
                .padding(PiPlannerDimens.Space16)
                .semantics { contentDescription = "Standing split skipped" },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = uiState.statusMessage.ifBlank {
                    stringResource(R.string.standing_split_skip_body)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PiPlannerDimens.Space24)
            .padding(top = PiPlannerDimens.Space12, bottom = PiPlannerDimens.Space28)
            .semantics { contentDescription = "Standing split" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
    ) {
        PiSheetHandle()

        SecondaryCta(
            text = stringResource(R.string.back),
            onClick = onBack,
            style = SecondaryCtaStyle.Text,
            fillMaxWidth = false,
            contentDescription = "Back",
        )

        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.standing_split_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.standing_split_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Text(
                text = uiState.caption.ifBlank { StandingSplitService.SAVED_MONEY_STAYS_PUT },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = PiPlannerColors.NavyPrimary,
                modifier = Modifier.semantics {
                    contentDescription = "Saved money stays put"
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
            uiState.goals.forEach { goal ->
                StandingSplitGoalRow(
                    goal = goal,
                    percent = uiState.displayPercents[goal.id] ?: 0,
                    onPercentChange = { onPercentChange(goal.id, it) },
                )
            }
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.canSave) {
                PiPlannerColors.OnSurface.copy(alpha = 0.72f)
            } else {
                PiPlannerColors.Behind
            },
            modifier = Modifier.semantics {
                contentDescription = "Standing split status ${uiState.statusMessage}"
            },
        )

        if (uiState.isSaving) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .height(PiPlannerDimens.Space20)
                        .width(PiPlannerDimens.Space20),
                    strokeWidth = PiPlannerDimens.Space8 / 4,
                    color = PiPlannerColors.NavyPrimary,
                )
            }
        }

        PrimaryCta(
            text = stringResource(R.string.save),
            onClick = onSave,
            enabled = uiState.canSave && !uiState.isSaving,
            contentDescription = "Save standing split",
        )

        Spacer(modifier = Modifier.height(PiPlannerDimens.Space8))
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.standing_split_save_failed_title)) },
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
private fun StandingSplitGoalRow(
    goal: Goal,
    percent: Int,
    onPercentChange: (Int) -> Unit,
) {
    PiCard(contentDescription = "${goal.name} share row") {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.share_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = percent.toString(),
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }
                            onPercentChange(digits.toIntOrNull() ?: 0)
                        },
                        modifier = Modifier
                            .width(PiPlannerDimens.Space28 * 3)
                            .semantics {
                                contentDescription = "${goal.name} percentage"
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PiPlannerColors.NavyPrimary,
                            cursorColor = PiPlannerColors.NavyPrimary,
                        ),
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.titleMedium,
                        color = PiPlannerColors.OnSurface,
                        modifier = Modifier.padding(start = PiPlannerDimens.Space8),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Standing split · valid 100%")
@Composable
private fun StandingSplitValidPreview() {
    PiPlannerTheme {
        StandingSplitContent(
            uiState = StandingSplitUiState(
                goals = previewGoals(),
                displayPercents = mapOf("g1" to 60, "g2" to 40),
                statusMessage = "Total 100% — ready to save",
                canSave = true,
                caption = StandingSplitService.SAVED_MONEY_STAYS_PUT,
            ),
            onPercentChange = { _, _ -> },
            onSave = {},
            onBack = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true, name = "Standing split · invalid total")
@Composable
private fun StandingSplitInvalidPreview() {
    PiPlannerTheme {
        StandingSplitContent(
            uiState = StandingSplitUiState(
                goals = previewGoals(),
                displayPercents = mapOf("g1" to 60, "g2" to 30),
                statusMessage = "Total 90% — need 100%",
                canSave = false,
                caption = StandingSplitService.SAVED_MONEY_STAYS_PUT,
            ),
            onPercentChange = { _, _ -> },
            onSave = {},
            onBack = {},
            onDismissError = {},
        )
    }
}

private fun previewGoals(): List<Goal> {
    val now = "2026-01-01T00:00:00Z"
    return listOf(
        Goal(
            id = "g1",
            name = "Emergency",
            targetAmount = 20_000_000L,
            startDate = "2026-01-01",
            endDate = "2027-01-01",
            savedAmount = 4_000_000L,
            shareOfNewCredits = 0.6,
            createdAt = now,
            updatedAt = now,
        ),
        Goal(
            id = "g2",
            name = "Vacation",
            targetAmount = 10_000_000L,
            startDate = "2026-01-01",
            endDate = "2026-12-01",
            savedAmount = 2_000_000L,
            shareOfNewCredits = 0.4,
            createdAt = now,
            updatedAt = now,
        ),
    )
}
