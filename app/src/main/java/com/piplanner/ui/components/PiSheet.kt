package com.piplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens

/**
 * Paytm-like bottom sheet chrome: white surface, top radius, drag handle, title spacing.
 * Visual shell only — hosts existing sheet content without changing product behaviour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    contentDescription: String = "Pi sheet",
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(
            topStart = PiPlannerDimens.RadiusSheetTop,
            topEnd = PiPlannerDimens.RadiusSheetTop,
        ),
        containerColor = PiPlannerColors.SurfaceCard,
        dragHandle = { PiSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PiPlannerDimens.Space24)
                .padding(bottom = PiPlannerDimens.Space28),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
            content = content,
        )
    }
}

/**
 * In-content Paytm-like sheet chrome for previews / embedding without a modal host.
 */
@Composable
fun PiSheetChrome(
    title: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    contentDescription: String = "Pi sheet chrome",
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = PiPlannerDimens.RadiusSheetTop,
                    topEnd = PiPlannerDimens.RadiusSheetTop,
                ),
            )
            .background(PiPlannerColors.SurfaceCard)
            .padding(horizontal = PiPlannerDimens.Space24)
            .padding(top = PiPlannerDimens.Space12, bottom = PiPlannerDimens.Space28)
            .semantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
    ) {
        PiSheetHandle()
        Column(verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.OnSurface,
            )
            if (helper != null) {
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
                )
            }
        }
        content()
    }
}

@Composable
fun PiSheetHandle(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            modifier = Modifier
                .width(PiPlannerDimens.SheetHandleWidth)
                .height(PiPlannerDimens.SheetHandleHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(PiPlannerColors.OutlineMuted)
                .semantics { contentDescription = "Sheet handle" },
        )
    }
}
