package com.piplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.piplanner.domain.StubGrokService
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Compose previews / visual smoke gallery for PIP-70 shared components.
 * Demo-only — not wired into product navigation (PIP-74+ adopts per screen).
 */
@Composable
fun SharedVisualComponentsGallery(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space16),
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space24),
    ) {
        Text(
            text = "PiCard",
            style = PiPlannerTypography.title,
            color = PiPlannerColors.OnBackground,
        )
        PiCard {
            Text(
                text = "White surface · radius 22dp · soft elevation",
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface,
            )
        }

        Text(
            text = "CTAs",
            style = PiPlannerTypography.title,
            color = PiPlannerColors.OnBackground,
        )
        PrimaryCta(text = "Set up savings", onClick = {})
        PrimaryCta(text = "Continue (disabled)", onClick = {}, enabled = false)
        SecondaryCta(text = "Cancel", onClick = {}, style = SecondaryCtaStyle.Outline)
        SecondaryCta(text = "Use a form", onClick = {}, style = SecondaryCtaStyle.Text)

        Text(
            text = "LightBlueChip",
            style = PiPlannerTypography.title,
            color = PiPlannerColors.OnBackground,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            LightBlueChip(label = "₹1,000", selected = false, onClick = {})
            LightBlueChip(label = "₹5,000", selected = true, onClick = {})
            LightBlueChip(label = "₹10,000", selected = false, onClick = {})
        }

        Text(
            text = "PiSheet chrome",
            style = PiPlannerTypography.title,
            color = PiPlannerColors.OnBackground,
        )
        PiSheetChrome(
            title = "Update balance",
            helper = "Choose how to refresh dedicated savings.",
        ) {
            PrimaryCta(text = "Manually", onClick = {})
            SecondaryCta(text = "Balance sync", onClick = {})
        }

        Text(
            text = "ProposalCard",
            style = PiPlannerTypography.title,
            color = PiPlannerColors.OnBackground,
        )
        ProposalCard(
            title = "Grok's proposal",
            onEdit = {},
            onConfirm = {},
            checkedByLabel = StubGrokService.CHECKED_BY_LABEL,
        ) {
            Text(
                text = "Emergency fund · ₹2,00,000 · 40%",
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface,
            )
        }
    }
}

@Preview(showBackground = true, name = "Shared visual components")
@Composable
private fun SharedVisualComponentsPreview() {
    PiPlannerTheme {
        SharedVisualComponentsGallery()
    }
}

@Preview(showBackground = true, name = "Primary CTA states")
@Composable
private fun PrimaryCtaStatesPreview() {
    PiPlannerTheme {
        Column(
            modifier = Modifier
                .background(PiPlannerColors.BackgroundApp)
                .padding(PiPlannerDimens.Space16),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        ) {
            PrimaryCta(text = "Enabled", onClick = {})
            PrimaryCta(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}

@Preview(showBackground = true, name = "Chip selected/unselected")
@Composable
private fun LightBlueChipStatesPreview() {
    PiPlannerTheme {
        Row(
            modifier = Modifier
                .background(PiPlannerColors.BackgroundApp)
                .padding(PiPlannerDimens.Space16),
            horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
        ) {
            LightBlueChip(label = "Unselected", selected = false, onClick = {})
            LightBlueChip(label = "Selected", selected = true, onClick = {})
        }
    }
}
