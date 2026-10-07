package com.piplanner.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.piplanner.domain.HistoryService
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.theme.PiIcons
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Single History list row — type label, amount, lock on saved entries (frames 12 / 12a).
 * PIP-92 visual only: PiCard + tokens + PiIcons; open vs locked chrome (iOS PIP-91 parity).
 * Product destination logic stays in [HistoryViewModel] / [HistoryTab].
 */
@Composable
fun HistoryEntryRow(
    row: HistoryRowUi,
    onClick: () -> Unit,
) {
    val isOpenChrome = row.isOpenAssignable
    val lockSuffix = if (row.showLockIcon) " locked" else ""
    val assignSuffix = if (isOpenChrome) " Assign now" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PiPlannerDimens.Space16,
                vertical = PiPlannerDimens.Space8,
            ),
    ) {
        PiCard(
            modifier = Modifier
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription =
                        "History row ${row.typeLabel} ${row.formattedAmount}$lockSuffix$assignSuffix"
                },
            contentPadding = PiPlannerDimens.Space16,
            contentDescription = "History row card ${row.id}",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
                verticalAlignment = Alignment.Top,
            ) {
                TypeIconWell(
                    typeIcon = row.typeIcon,
                    typeName = row.type.name,
                    isOpenChrome = isOpenChrome,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.typeLabel,
                            style = PiPlannerTypography.body,
                            fontWeight = FontWeight.SemiBold,
                            color = PiPlannerColors.OnSurface,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (row.showLockIcon) {
                            Icon(
                                imageVector = PiIcons.lock,
                                contentDescription = "Locked history entry",
                                tint = PiPlannerColors.NavyPrimary.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .semantics { contentDescription = "Locked history entry" },
                            )
                        }
                    }
                    if (row.subtitle.isNotBlank()) {
                        if (isOpenChrome) {
                            AssignNowChip(label = row.subtitle)
                        } else {
                            Text(
                                text = row.subtitle,
                                style = PiPlannerTypography.caption,
                                color = PiPlannerColors.OnSurface.copy(alpha = 0.62f),
                            )
                        }
                    }
                }
                Text(
                    text = row.formattedAmount,
                    style = PiPlannerTypography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOpenChrome) {
                        PiPlannerColors.NavyPrimary
                    } else {
                        PiPlannerColors.OnSurface
                    },
                    textAlign = TextAlign.End,
                )
            }
        }
        if (isOpenChrome) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = PiPlannerDimens.Space8),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(PiPlannerColors.NavyPrimary)
                        .semantics { contentDescription = "History open accent" },
                )
            }
        }
    }
}

@Composable
private fun TypeIconWell(
    typeIcon: String,
    typeName: String,
    isOpenChrome: Boolean,
) {
    val wellFill = if (isOpenChrome) {
        PiPlannerColors.ChipLightBlue
    } else {
        PiPlannerColors.ChipLightBlue.copy(alpha = 0.55f)
    }
    val iconTint = if (isOpenChrome) {
        PiPlannerColors.NavyPrimary
    } else {
        PiPlannerColors.NavyPrimary.copy(alpha = 0.72f)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(wellFill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = PiIcons.resolve(typeIcon),
            contentDescription = "History icon $typeName",
            tint = iconTint,
            modifier = Modifier
                .size(22.dp)
                .semantics { contentDescription = "History icon $typeName" },
        )
    }
}

@Composable
private fun AssignNowChip(label: String) {
    Text(
        text = label,
        style = PiPlannerTypography.caption,
        fontWeight = FontWeight.SemiBold,
        color = PiPlannerColors.OnChipLightBlue,
        modifier = Modifier
            .clip(RoundedCornerShape(PiPlannerDimens.RadiusChip))
            .background(PiPlannerColors.ChipLightBlue.copy(alpha = 0.85f))
            .padding(
                horizontal = PiPlannerDimens.Space8,
                vertical = 4.dp,
            )
            .semantics {
                contentDescription = HistoryService.ASSIGN_NOW_SUBTITLE
            },
    )
}
