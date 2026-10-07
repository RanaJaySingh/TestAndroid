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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.domain.TransferAskPrefill
import com.piplanner.ui.components.LightBlueChip
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.ProposalCard
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.setup.GoalFormScreen
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Ask tab — frames 19 / 19a / 19b / 19c / 19d (PRD R17).
 * Visual parity (PIP-96): tokens + LightBlueChip / PiCard / ProposalCard / PrimaryCta —
 * no Grok stub or confirm-routing changes.
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
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space16)
            .semantics { contentDescription = "Ask tab" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.ask_tab_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.Bold,
                color = PiPlannerColors.OnBackground,
            )
            Text(
                text = stringResource(R.string.ask_tab_subtitle),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnBackground.copy(alpha = 0.72f),
            )
        }

        if (uiState.phase != AskPhase.Unavailable) {
            SuggestionChipRow(
                chips = uiState.suggestionChips,
                draftInput = uiState.draftInput,
                onChip = onChip,
            )
        }

        AskGrokComposer(
            draft = uiState.draftInput,
            enabled = uiState.phase != AskPhase.Unavailable,
            onDraftChange = onDraftChange,
            onSubmit = onSubmit,
        )

        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnBackground.copy(alpha = 0.72f),
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
                AskProposalCard(
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
private fun SuggestionChipRow(
    chips: List<String>,
    draftInput: String,
    onChip: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
    ) {
        chips.forEach { chip ->
            LightBlueChip(
                label = chip,
                selected = draftInput == chip,
                onClick = { onChip(chip) },
                contentDescription = "Ask chip $chip",
            )
        }
    }
}

@Composable
private fun AskGrokComposer(
    draft: String,
    enabled: Boolean,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Ask input" },
            placeholder = {
                Text(
                    text = stringResource(R.string.ask_input_placeholder),
                    style = PiPlannerTypography.body,
                )
            },
            singleLine = false,
            minLines = 2,
            shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
            colors = askFieldColors(),
            textStyle = PiPlannerTypography.body,
        )
        PrimaryCta(
            text = stringResource(R.string.ask_send),
            onClick = onSubmit,
            enabled = enabled && draft.isNotBlank(),
            contentDescription = "Ask submit",
        )
    }
}

@Composable
private fun PlainAnswerCard(answer: String) {
    PiCard(contentDescription = "Ask plain answer") {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = stringResource(R.string.ask_answer_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = answer,
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface,
            )
        }
    }
}

/** Shared ProposalCard shell — “Grok's proposal” / body / checked-by / Edit·Confirm (PRD R17). */
@Composable
private fun AskProposalCard(
    body: String,
    checkedByLabel: String,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
) {
    ProposalCard(
        title = stringResource(R.string.grok_proposal_title),
        onEdit = onEdit,
        onConfirm = onConfirm,
        checkedByLabel = checkedByLabel,
        contentDescription = "Ask proposal card",
    ) {
        Text(
            text = body,
            style = PiPlannerTypography.body,
            color = PiPlannerColors.OnSurface,
            modifier = Modifier.semantics { contentDescription = "Proposal summary $body" },
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
    PiCard(contentDescription = "Ask unavailable fallback") {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
            Text(
                text = stringResource(R.string.grok_unavailable_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.ask_unavailable_body),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            Text(
                text = stringResource(R.string.ask_templates_label),
                style = PiPlannerTypography.caption.copy(fontWeight = FontWeight.Medium),
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            templates.forEach { template ->
                LightBlueChip(
                    label = template,
                    selected = false,
                    onClick = { onTemplate(template) },
                    contentDescription = "Ask template $template",
                )
            }
            PrimaryCta(
                text = stringResource(R.string.use_a_form),
                onClick = onUseForm,
                contentDescription = "Use a form",
            )
            SecondaryCta(
                text = stringResource(R.string.standing_split_title),
                onClick = onOpenStandingSplit,
                style = SecondaryCtaStyle.Outline,
                contentDescription = "Open standing split",
            )
        }
    }
}

@Composable
private fun askFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.35f),
    unfocusedBorderColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.18f),
    disabledBorderColor = PiPlannerColors.OutlineMuted,
    focusedContainerColor = PiPlannerColors.SurfaceCard,
    unfocusedContainerColor = PiPlannerColors.SurfaceCard,
    disabledContainerColor = PiPlannerColors.SurfaceCard.copy(alpha = 0.70f),
    cursorColor = PiPlannerColors.NavyPrimary,
    focusedTextColor = PiPlannerColors.OnSurface,
    unfocusedTextColor = PiPlannerColors.OnSurface,
    disabledTextColor = PiPlannerColors.OnSurface.copy(alpha = 0.50f),
)
