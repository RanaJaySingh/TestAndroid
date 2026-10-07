package com.piplanner.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piplanner.R
import com.piplanner.data.model.Goal

/**
 * Placeholder Goals tab (screen 9) until PIP-46 owns the full list/cards UI.
 * Minimal goal name links keep Goal detail/edit (PIP-50) reachable after Opening lock.
 */
@Composable
fun GoalsTabPlaceholder(
    goals: List<Goal> = emptyList(),
    onOpenGoal: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .semantics {
                contentDescription = "Goals tab placeholder. Opening balance locked."
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.goals_tab_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.goals_tab_placeholder_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )

        if (goals.isNotEmpty()) {
            Text(
                text = stringResource(R.string.goals_tab_open_goal_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            goals.forEach { goal ->
                TextButton(
                    onClick = { onOpenGoal(goal.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Open goal ${goal.name}" },
                ) {
                    Text(goal.name)
                }
            }
        }
    }
}
