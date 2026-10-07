package com.piplanner.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piplanner.ui.theme.PiIcons

/**
 * Single History list row — type icon/label, amount, lock for saved entries (frame 12).
 * PIP-72: Material icons for type + lock (Spec §3.3).
 */
@Composable
fun HistoryEntryRow(
    row: HistoryRowUi,
    onClick: () -> Unit,
) {
    val lockSuffix = if (row.showLockIcon) " locked" else ""
    val assignSuffix = if (row.isOpenAssignable) " Assign now" else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                contentDescription =
                    "History row ${row.typeLabel} ${row.formattedAmount}$lockSuffix$assignSuffix"
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = PiIcons.resolve(row.typeIcon),
            contentDescription = "History icon ${row.type.name}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(28.dp)
                .semantics { contentDescription = "History icon ${row.type.name}" },
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.typeLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = row.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (row.isOpenAssignable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.formattedAmount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (row.showLockIcon) {
                Icon(
                    imageVector = PiIcons.lock,
                    contentDescription = "Locked history entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .semantics { contentDescription = "Locked history entry" },
                )
            }
        }
    }
}
