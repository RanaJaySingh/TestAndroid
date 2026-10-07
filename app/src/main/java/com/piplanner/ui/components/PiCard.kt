package com.piplanner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Shared white content card (PRD R2 / Tech Spec §3.5).
 * White surface, radius 20–24 (token 22), soft elevation.
 *
 * @param contentPadding inner padding; default [PiPlannerDimens.Space16] (Welcome may use Space20).
 */
@Composable
fun PiCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = PiPlannerDimens.Space16,
    contentDescription: String = "Pi card",
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(PiPlannerDimens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = PiPlannerColors.SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = PiPlannerDimens.ElevationCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}
