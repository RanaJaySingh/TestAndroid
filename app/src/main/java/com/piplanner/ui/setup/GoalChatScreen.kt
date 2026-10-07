package com.piplanner.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.data.model.Goal
import com.piplanner.domain.GoalProposal
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.components.ProposalCard
import com.piplanner.ui.components.SecondaryCta
import com.piplanner.ui.components.SecondaryCtaStyle
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Goal chat — design frames 5 / 5a / 5b / 5c, with form hand-off (6) and goals-defined Continue.
 * Visual parity (PIP-80): tokens + ProposalCard / PiCard / PrimaryCta / SecondaryCta only.
 */
@Composable
fun GoalChatScreen(
    viewModel: GoalChatViewModel,
    onContinueToOpeningSplit: (List<Goal>) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val validation = viewModel.validationService()

    LaunchedEffect(uiState.shouldNavigateToOpeningSplit) {
        if (uiState.shouldNavigateToOpeningSplit) {
            val goals = uiState.definedGoals
            viewModel.consumeNavigation()
            onContinueToOpeningSplit(goals)
        }
    }

    if (uiState.phase == GoalChatPhase.Form) {
        GoalFormScreen(
            draft = uiState.formDraft,
            validation = validation,
            formatInr = viewModel::formatInr,
            onDraftChange = { draft -> viewModel.updateFormDraft { draft } },
            onOpenInflation = { viewModel.setShowInflationPopup(true) },
            onSave = { viewModel.saveForm() },
            onCancel = viewModel::cancelForm,
            showInflationPopup = uiState.showInflationPopup,
            onDismissInflation = { viewModel.setShowInflationPopup(false) },
        )
    } else {
        GoalChatContent(
            uiState = uiState,
            canSend = viewModel.canSend(uiState),
            canContinue = viewModel.canContinue(uiState.definedGoals),
            continueDisabledReason = viewModel.continueDisabledReason(uiState.definedGoals),
            formatInr = viewModel::formatInr,
            displayPercent = viewModel::displayPercent,
            onDraftChange = viewModel::setDraftInput,
            onSend = viewModel::sendDraft,
            onUseForm = viewModel::useFormPath,
            onConfirmProposals = viewModel::confirmProposals,
            onEditProposals = viewModel::editProposals,
            onEditDefinedGoal = viewModel::editDefinedGoal,
            onShareChange = viewModel::updateShare,
            onContinue = viewModel::continueToOpeningSplit,
        )
    }
}

@Composable
fun GoalChatContent(
    uiState: GoalChatUiState,
    canSend: Boolean,
    canContinue: Boolean,
    continueDisabledReason: String?,
    formatInr: (Long) -> String,
    displayPercent: (Double) -> Int,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onUseForm: () -> Unit,
    onConfirmProposals: () -> Unit,
    onEditProposals: () -> Unit,
    onEditDefinedGoal: (Goal) -> Unit,
    onShareChange: (String, Int) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PiPlannerDimens.Space16,
                    vertical = PiPlannerDimens.Space8,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.goal_chat_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnBackground,
            )
            if (uiState.phase != GoalChatPhase.Unavailable) {
                SecondaryCta(
                    text = stringResource(R.string.use_a_form),
                    onClick = onUseForm,
                    fillMaxWidth = false,
                    style = SecondaryCtaStyle.Text,
                    contentDescription = "Use a form",
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(PiPlannerDimens.Space16),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
        ) {
            uiState.messages.forEach { message ->
                MessageBubble(message)
            }

            if (uiState.phase == GoalChatPhase.Proposal) {
                GoalProposalCard(
                    proposals = uiState.proposals,
                    checkedByLabel = uiState.checkedByLabel,
                    formatInr = formatInr,
                    displayPercent = displayPercent,
                    onEdit = onEditProposals,
                    onConfirm = onConfirmProposals,
                )
            }

            if (uiState.phase == GoalChatPhase.Unavailable) {
                UnavailableCard(onUseForm = onUseForm)
            }

            if (uiState.phase == GoalChatPhase.GoalsDefined || uiState.definedGoals.isNotEmpty()) {
                GoalsDefinedSection(
                    goals = uiState.definedGoals,
                    canContinue = canContinue,
                    continueDisabledReason = continueDisabledReason,
                    formatInr = formatInr,
                    displayPercent = displayPercent,
                    onShareChange = onShareChange,
                    onEditGoal = onEditDefinedGoal,
                    onContinue = onContinue,
                )
            }
        }

        if (uiState.phase == GoalChatPhase.Chat ||
            uiState.phase == GoalChatPhase.FollowUp ||
            uiState.phase == GoalChatPhase.Proposal
        ) {
            Composer(
                draft = uiState.draftInput,
                canSend = canSend,
                onDraftChange = onDraftChange,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: GoalChatMessage) {
    val isUser = message.role == GoalChatMessage.Role.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = message.text,
            style = PiPlannerTypography.body,
            color = if (isUser) {
                PiPlannerColors.OnChipLightBlue
            } else {
                PiPlannerColors.OnSurface
            },
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(PiPlannerDimens.RadiusCard))
                .background(
                    if (isUser) {
                        PiPlannerColors.ChipLightBlue
                    } else {
                        PiPlannerColors.SurfaceCard
                    },
                )
                .padding(PiPlannerDimens.Space12)
                .semantics {
                    contentDescription = if (isUser) {
                        "You: ${message.text}"
                    } else {
                        "Assistant: ${message.text}"
                    }
                },
        )
    }
}

/** Shared ProposalCard shell — title / body / checked-by / Edit·Confirm (PRD R9). */
@Composable
private fun GoalProposalCard(
    proposals: List<GoalProposal>,
    checkedByLabel: String,
    formatInr: (Long) -> String,
    displayPercent: (Double) -> Int,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
) {
    ProposalCard(
        title = stringResource(R.string.grok_proposal_title),
        onEdit = onEdit,
        onConfirm = onConfirm,
        checkedByLabel = checkedByLabel,
        contentDescription = "Proposal card",
    ) {
        proposals.forEach { proposal ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8 / 2)) {
                    Text(
                        text = proposal.name,
                        style = PiPlannerTypography.body,
                        fontWeight = FontWeight.SemiBold,
                        color = PiPlannerColors.OnSurface,
                    )
                    Text(
                        text = formatInr(proposal.suggestedTarget ?: 0L),
                        style = PiPlannerTypography.caption,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.percent_value,
                        displayPercent(proposal.sharePercentage),
                    ),
                    style = PiPlannerTypography.title,
                    fontWeight = FontWeight.SemiBold,
                    color = PiPlannerColors.NavyPrimary,
                )
            }
        }
    }
}

@Composable
private fun UnavailableCard(onUseForm: () -> Unit) {
    PiCard(contentDescription = "Grok unavailable") {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
            Text(
                text = stringResource(R.string.grok_unavailable_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            Text(
                text = stringResource(R.string.grok_unavailable_body),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
            PrimaryCta(
                text = stringResource(R.string.use_a_form),
                onClick = onUseForm,
                contentDescription = "Use a form",
            )
        }
    }
}

@Composable
private fun GoalsDefinedSection(
    goals: List<Goal>,
    canContinue: Boolean,
    continueDisabledReason: String?,
    formatInr: (Long) -> String,
    displayPercent: (Double) -> Int,
    onShareChange: (String, Int) -> Unit,
    onEditGoal: (Goal) -> Unit,
    onContinue: () -> Unit,
) {
    PiCard(contentDescription = "Goals defined") {
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12)) {
            Text(
                text = stringResource(R.string.goals_defined_title),
                style = PiPlannerTypography.title,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            goals.forEach { goal ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8 / 2),
                    ) {
                        Text(
                            text = goal.name,
                            style = PiPlannerTypography.body,
                            fontWeight = FontWeight.SemiBold,
                            color = PiPlannerColors.OnSurface,
                        )
                        Text(
                            text = formatInr(goal.targetAmount),
                            style = PiPlannerTypography.caption,
                            color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                        )
                    }
                    OutlinedTextField(
                        value = displayPercent(goal.shareOfNewCredits).toString(),
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }
                            onShareChange(goal.id, digits.toIntOrNull() ?: 0)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = goalFormFieldColors(),
                        modifier = Modifier
                            .width(PiPlannerDimens.Space28 * 3)
                            .semantics { contentDescription = "${goal.name} share percent" },
                    )
                    Spacer(modifier = Modifier.width(PiPlannerDimens.Space8 / 2))
                    Text(
                        text = "%",
                        style = PiPlannerTypography.body,
                        color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                    )
                    SecondaryCta(
                        text = stringResource(R.string.edit),
                        onClick = { onEditGoal(goal) },
                        fillMaxWidth = false,
                        style = SecondaryCtaStyle.Text,
                        contentDescription = "Edit ${goal.name}",
                    )
                }
            }

            Text(
                text = continueDisabledReason
                    ?: stringResource(R.string.goals_defined_ready),
                style = PiPlannerTypography.caption,
                color = if (continueDisabledReason != null) {
                    PiPlannerColors.Behind
                } else {
                    PiPlannerColors.OnSurface.copy(alpha = 0.72f)
                },
            )

            PrimaryCta(
                text = stringResource(R.string.continue_label),
                onClick = onContinue,
                enabled = canContinue,
                contentDescription = "Continue",
            )
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PiPlannerColors.SurfaceCard)
            .padding(
                horizontal = PiPlannerDimens.Space16,
                vertical = PiPlannerDimens.Space12,
            ),
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.goal_chat_placeholder),
                        style = PiPlannerTypography.body,
                    )
                },
                colors = goalFormFieldColors(),
                shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Goal description" },
            )
            PrimaryCta(
                text = stringResource(R.string.send),
                onClick = onSend,
                enabled = canSend,
                fillMaxWidth = false,
                contentDescription = "Send",
            )
        }
    }
}

@Composable
internal fun goalFormFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.35f),
    unfocusedBorderColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.18f),
    focusedContainerColor = PiPlannerColors.BackgroundApp,
    unfocusedContainerColor = PiPlannerColors.BackgroundApp,
    cursorColor = PiPlannerColors.NavyPrimary,
    focusedTextColor = PiPlannerColors.OnSurface,
    unfocusedTextColor = PiPlannerColors.OnSurface,
)
