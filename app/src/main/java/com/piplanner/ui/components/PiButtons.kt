package com.piplanner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Filled navy primary CTA (Tech Spec §3.5 / PRD inventory).
 * Supports enabled / disabled visual states.
 */
@Composable
fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
    contentDescription: String = text,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
        colors = ButtonDefaults.buttonColors(
            containerColor = PiPlannerColors.NavyPrimary,
            contentColor = PiPlannerColors.OnNavy,
            disabledContainerColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.38f),
            disabledContentColor = PiPlannerColors.OnNavy.copy(alpha = 0.70f),
        ),
        contentPadding = PaddingValues(
            horizontal = PiPlannerDimens.Space20,
            vertical = PiPlannerDimens.Space12,
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

enum class SecondaryCtaStyle {
    Outline,
    Text,
}

/**
 * Secondary CTA — outline or text (Cancel / Use a form / No…).
 */
@Composable
fun SecondaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SecondaryCtaStyle = SecondaryCtaStyle.Outline,
    fillMaxWidth: Boolean = true,
    contentDescription: String = text,
) {
    val layoutModifier = modifier
        .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
        .semantics { this.contentDescription = contentDescription }

    when (style) {
        SecondaryCtaStyle.Outline -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = layoutModifier,
                shape = RoundedCornerShape(PiPlannerDimens.RadiusChip),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PiPlannerColors.NavyPrimary,
                    disabledContentColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.38f),
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (enabled) {
                        PiPlannerColors.NavyPrimary
                    } else {
                        PiPlannerColors.NavyPrimary.copy(alpha = 0.38f)
                    },
                ),
                contentPadding = PaddingValues(
                    horizontal = PiPlannerDimens.Space20,
                    vertical = PiPlannerDimens.Space12,
                ),
            ) {
                Text(text = text, fontWeight = FontWeight.SemiBold)
            }
        }
        SecondaryCtaStyle.Text -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = layoutModifier,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PiPlannerColors.NavyPrimary,
                    disabledContentColor = PiPlannerColors.NavyPrimary.copy(alpha = 0.38f),
                ),
                contentPadding = PaddingValues(
                    horizontal = PiPlannerDimens.Space16,
                    vertical = PiPlannerDimens.Space8,
                ),
            ) {
                Text(text = text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
