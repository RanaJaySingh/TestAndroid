package com.piplanner.ui.setup

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

/**
 * Goal chat — design frames 5 / 5a / 5b / 5c, with form hand-off (6) and goals-defined Continue.
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
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.goal_chat_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (uiState.phase != GoalChatPhase.Unavailable) {
                TextButton(
                    onClick = onUseForm,
                    modifier = Modifier.semantics { contentDescription = "Use a form" },
                ) {
                    Text(stringResource(R.string.use_a_form))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.messages.forEach { message ->
                MessageBubble(message)
            }

            if (uiState.phase == GoalChatPhase.Proposal) {
                ProposalCard(
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
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isUser) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                .padding(12.dp)
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

@Composable
private fun ProposalCard(
    proposals: List<GoalProposal>,
    checkedByLabel: String,
    formatInr: (Long) -> String,
    displayPercent: (Double) -> Int,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.suggested_goals),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        proposals.forEach { proposal ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = proposal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatInr(proposal.suggestedTarget ?: 0L),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.percent_value,
                        displayPercent(proposal.sharePercentage),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = checkedByLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = checkedByLabel },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.semantics { contentDescription = "Edit proposal" },
            ) {
                Text(stringResource(R.string.edit))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.semantics { contentDescription = "Confirm proposal" },
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    }
}

@Composable
private fun UnavailableCard(onUseForm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.grok_unavailable_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.grok_unavailable_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onUseForm,
            modifier = Modifier.semantics { contentDescription = "Use a form" },
        ) {
            Text(stringResource(R.string.use_a_form))
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.goals_defined_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        goals.forEach { goal ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatInr(goal.targetAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier = Modifier
                        .width(72.dp)
                        .semantics { contentDescription = "${goal.name} share percent" },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onEditGoal(goal) }) {
                    Text(stringResource(R.string.edit))
                }
            }
        }

        Text(
            text = continueDisabledReason
                ?: stringResource(R.string.goals_defined_ready),
            style = MaterialTheme.typography.bodySmall,
            color = if (continueDisabledReason != null) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Continue" },
        ) {
            Text(stringResource(R.string.continue_label))
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(modifier = Modifier.height(1.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text(stringResource(R.string.goal_chat_placeholder)) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Goal description" },
            )
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.semantics { contentDescription = "Send" },
            ) {
                Text(stringResource(R.string.send))
            }
        }
    }
}
