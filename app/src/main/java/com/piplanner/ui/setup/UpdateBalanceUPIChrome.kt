package com.piplanner.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Shared Paytm-like chrome for Update balance / UPI Demo (PIP-78 · PRD R8 · frames 4 / 4a–4e / 11a–11c).
 * Visual helpers only — no ViewModel or product behaviour.
 */

@Composable
fun UpdateBalanceChoiceRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    contentDescription: String = title,
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = PiPlannerDimens.ElevationCard,
                shape = RoundedCornerShape(PiPlannerDimens.RadiusCard),
                spotColor = PiPlannerColors.OnSurface.copy(alpha = 0.08f),
                ambientColor = PiPlannerColors.OnSurface.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(PiPlannerDimens.RadiusCard))
            .background(PiPlannerColors.SurfaceCard)
            .clickable(
                enabled = enabled,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(PiPlannerDimens.Space16)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = PiPlannerColors.NavyPrimary.copy(alpha = alpha),
                modifier = Modifier.size(28.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = PiPlannerTypography.body,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface.copy(alpha = alpha),
            )
            Text(
                text = subtitle,
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = if (enabled) 0.72f else 0.45f),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PiPlannerColors.OutlineMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun UPIDemoBadge(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "DEMO",
        style = PiPlannerTypography.caption,
        fontWeight = FontWeight.Bold,
        color = PiPlannerColors.OnChipLightBlue,
        letterSpacing = 0.6.sp,
        modifier = modifier
            .clip(RoundedCornerShape(PiPlannerDimens.RadiusChip))
            .background(PiPlannerColors.ChipLightBlue)
            .padding(
                horizontal = PiPlannerDimens.Space12,
                vertical = PiPlannerDimens.Space8,
            )
            .semantics { contentDescription = "Demo" },
    )
}

@Composable
fun UPIBankMaskedLine(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = PiPlannerTypography.body,
        fontWeight = FontWeight.Medium,
        color = PiPlannerColors.OnSurface,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Bank account $title" },
    )
}

@Composable
fun UPIPinDots(
    filledCount: Int,
    modifier: Modifier = Modifier,
    showsError: Boolean = false,
) {
    val total = 4
    val strokeColor = if (showsError) {
        PiPlannerColors.Destructive
    } else {
        PiPlannerColors.OnSurface.copy(alpha = 0.55f)
    }
    val fillColor = if (showsError) {
        PiPlannerColors.Destructive
    } else {
        PiPlannerColors.NavyPrimary
    }
    Row(
        modifier = modifier.semantics {
            contentDescription = "PIN entered ${minOf(filledCount, total)} of $total digits"
        },
        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
    ) {
        repeat(total) { index ->
            val filled = index < filledCount
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(width = 1.5.dp, color = strokeColor, shape = CircleShape)
                    .background(if (filled) fillColor else PiPlannerColors.SurfaceCard),
            )
        }
    }
}

@Composable
fun UPIMockPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val padRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = PiPlannerDimens.ElevationCard,
                shape = RoundedCornerShape(PiPlannerDimens.RadiusCard),
                spotColor = PiPlannerColors.OnSurface.copy(alpha = 0.08f),
                ambientColor = PiPlannerColors.OnSurface.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(PiPlannerDimens.RadiusCard))
            .background(PiPlannerColors.SurfaceCard)
            .padding(PiPlannerDimens.Space12)
            .semantics { contentDescription = "UPI mock pad" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
    ) {
        padRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f).height(52.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(PiPlannerDimens.RadiusChip))
                                .background(PiPlannerColors.BackgroundApp)
                                .clickable(
                                    enabled = enabled,
                                    indication = ripple(),
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    if (key == "⌫") onDelete() else onDigit(key)
                                }
                                .semantics {
                                    contentDescription = if (key == "⌫") "Delete" else key
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = key,
                                style = PiPlannerTypography.title,
                                fontWeight = FontWeight.Medium,
                                color = PiPlannerColors.NavyPrimary.copy(
                                    alpha = if (enabled) 1f else 0.38f,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Convenience leading icons for Update balance choice rows (Material core). */
object UpdateBalanceChoiceIcons {
    val Manually: ImageVector get() = Icons.Filled.Edit
    /** Sync metaphor via Material core Refresh (Sync lives in icons-extended). */
    val BalanceSync: ImageVector get() = Icons.Filled.Refresh
}

@Preview(showBackground = true, name = "Choice rows")
@Composable
private fun UpdateBalanceChoiceRowPreview() {
    PiPlannerTheme {
        Column(
            modifier = Modifier
                .background(PiPlannerColors.BackgroundApp)
                .padding(PiPlannerDimens.Space20),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        ) {
            UpdateBalanceChoiceRow(
                title = "Manually",
                subtitle = "Type the opening balance",
                leadingIcon = UpdateBalanceChoiceIcons.Manually,
                onClick = {},
            )
            UpdateBalanceChoiceRow(
                title = "Balance sync",
                subtitle = "Check with demo UPI PIN",
                leadingIcon = UpdateBalanceChoiceIcons.BalanceSync,
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "UPI pad")
@Composable
private fun UPIMockPadPreview() {
    PiPlannerTheme {
        Column(
            modifier = Modifier
                .background(PiPlannerColors.BackgroundApp)
                .padding(PiPlannerDimens.Space20),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
        ) {
            UPIDemoBadge()
            UPIBankMaskedLine(title = "HDFC ••4821")
            UPIPinDots(filledCount = 2)
            UPIMockPad(onDigit = {}, onDelete = {})
        }
    }
}
