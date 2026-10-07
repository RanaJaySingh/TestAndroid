package com.piplanner.ui.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.domain.TransferAskPrefill
import com.piplanner.ui.setup.GoalFormScreen

/**
 * Ask tab — frames 19 / 19a / 19b / 19c / 19d (PRD R18 / R19).
 */
@Composable
fun AskTab(
    onOpenTransfer: (TransferAskPrefill) -> Unit,
    onOpenStandingSplit: () -> Unit,
    viewModel: AskViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateTransfer) {
        val prefill = uiState.navigateTransfer ?: return@LaunchedEffect
        onOpenTransfer(prefill)
        viewModel.consumeTransferNavigation()
    }
    LaunchedEffect(uiState.navigateStandingSplit) {
        if (!uiState.navigateStandingSplit) return@LaunchedEffect
        onOpenStandingSplit()
        viewModel.consumeStandingSplitNavigation()
    }

    when (uiState.phase) {
        AskPhase.GoalForm -> {
            GoalFormScreen(
                draft = uiState.formDraft,
                validation = viewModel.validationService(),
                formatInr = viewModel::formatInr,
                onDraftChange = { draft -> viewModel.updateFormDraft { draft } },
                onOpenInflation = { viewModel.setShowInflationPopup(true) },
                onSave = { viewModel.saveForm() },
                onCancel = viewModel::cancelForm,
                showInflationPopup = uiState.showInflationPopup,
                onDismissInflation = { viewModel.setShowInflationPopup(false) },
            )
        }
        else -> {
            AskTabContent(
                uiState = uiState,
                onDraftChange = viewModel::setDraftInput,
                onChip = viewModel::selectChip,
                onTemplate = viewModel::selectTemplate,
                onSubmit = viewModel::submit,
                onConfirm = viewModel::confirmProposal,
                onEdit = viewModel::editProposal,
                onUseForm = viewModel::useFormPath,
                onOpenStandingSplit = viewModel::openStandingSplitFallback,
            )
        }
    }
}

@Composable
private fun AskTabContent(
    uiState: AskUiState,
    onDraftChange: (String) -> Unit,
    onChip: (String) -> Unit,
    onTemplate: (String) -> Unit,
    onSubmit: () -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onUseForm: () -> Unit,
    onOpenStandingSplit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .semantics { contentDescription = "Ask tab" },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.ask_tab_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.ask_tab_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.phase != AskPhase.Unavailable) {
            ChipRow(
                chips = uiState.suggestionChips,
                onChip = onChip,
                contentDescriptionPrefix = "Ask chip",
            )
        }

        OutlinedTextField(
            value = uiState.draftInput,
            onValueChange = onDraftChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Ask input" },
            placeholder = { Text(stringResource(R.string.ask_input_placeholder)) },
            singleLine = false,
            minLines = 2,
        )

        Button(
            onClick = onSubmit,
            enabled = uiState.draftInput.isNotBlank() && uiState.phase != AskPhase.Unavailable,
            modifier = Modifier.semantics { contentDescription = "Ask submit" },
        ) {
            Text(stringResource(R.string.ask_send))
        }

        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Ask status $message" },
            )
        }

        when (uiState.phase) {
            AskPhase.PlainAnswer -> {
                uiState.plainAnswer?.let { answer ->
                    PlainAnswerCard(answer = answer)
                }
            }
            AskPhase.Proposal -> {
                ProposalCard(
                    title = uiState.proposalTitle,
                    body = uiState.proposalBody,
                    checkedByLabel = uiState.checkedByLabel,
                    onEdit = onEdit,
                    onConfirm = onConfirm,
                )
            }
            AskPhase.Unavailable -> {
                UnavailableFallback(
                    templates = uiState.fallbackTemplates,
                    onTemplate = onTemplate,
                    onUseForm = onUseForm,
                    onOpenStandingSplit = onOpenStandingSplit,
                )
            }
            AskPhase.InvalidDraft -> {
                // Prompt already in statusMessage; keep input visible for one retry.
            }
            AskPhase.Input, AskPhase.GoalForm -> Unit
        }
    }
}

@Composable
private fun ChipRow(
    chips: List<String>,
    onChip: (String) -> Unit,
    contentDescriptionPrefix: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            OutlinedButton(
                onClick = { onChip(chip) },
                modifier = Modifier.semantics {
                    contentDescription = "$contentDescriptionPrefix $chip"
                },
            ) {
                Text(chip)
            }
        }
    }
}

@Composable
private fun PlainAnswerCard(answer: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
            .semantics { contentDescription = "Ask plain answer" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ask_answer_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun UnavailableFallback(
    templates: List<String>,
    onTemplate: (String) -> Unit,
    onUseForm: () -> Unit,
    onOpenStandingSplit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
            .semantics { contentDescription = "Ask unavailable fallback" },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.grok_unavailable_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.ask_unavailable_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.ask_templates_label),
            style = MaterialTheme.typography.labelLarge,
        )
        templates.forEach { template ->
            TextButton(
                onClick = { onTemplate(template) },
                modifier = Modifier.semantics {
                    contentDescription = "Ask template $template"
                },
            ) {
                Text(template)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onUseForm,
                modifier = Modifier.semantics { contentDescription = "Use a form" },
            ) {
                Text(stringResource(R.string.use_a_form))
            }
            OutlinedButton(
                onClick = onOpenStandingSplit,
                modifier = Modifier.semantics { contentDescription = "Open standing split" },
            ) {
                Text(stringResource(R.string.standing_split_title))
            }
        }
    }
}
