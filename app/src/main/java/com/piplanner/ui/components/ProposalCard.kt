package com.piplanner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.piplanner.R
import com.piplanner.domain.StubGrokService
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Shared ProposalCard shell (Goal chat / Ask / entry proposal).
 * Hierarchy: title → body slot → “Checked by PiPlanner. Estimate.” → Edit / Confirm.
 * Visual only — callers wire behaviour; screens adopt this in later tickets.
 */
@Composable
fun ProposalCard(
    title: String,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    checkedByLabel: String = StubGrokService.CHECKED_BY_LABEL,
    editLabel: String? = null,
    confirmLabel: String? = null,
    contentDescription: String = "Proposal card",
    body: @Composable ColumnScope.() -> Unit,
) {
    val resolvedEdit = editLabel ?: stringResource(R.string.edit)
    val resolvedConfirm = confirmLabel ?: stringResource(R.string.confirm)

    PiCard(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        contentDescription = contentDescription,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            body()
            Text(
                text = checkedByLabel,
                style = MaterialTheme.typography.bodySmall,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                modifier = Modifier.semantics { this.contentDescription = checkedByLabel },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            ) {
                SecondaryCta(
                    text = resolvedEdit,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = true,
                    style = SecondaryCtaStyle.Outline,
                    contentDescription = "Edit proposal",
                )
                PrimaryCta(
                    text = resolvedConfirm,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    fillMaxWidth = true,
                    contentDescription = "Confirm proposal",
                )
            }
        }
    }
}
