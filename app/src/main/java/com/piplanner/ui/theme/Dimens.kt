package com.piplanner.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared spacing and radius tokens (Tech Spec §3.1 / PRD R2).
 * Card radius prefer 22dp within 20–24; spacing scale 8/12/16/20/24/28.
 */
object PiPlannerDimens {
    val RadiusCard: Dp = 22.dp
    val RadiusChip: Dp = 12.dp
    val RadiusSheetTop: Dp = 22.dp

    /** Soft Paytm-like card elevation (not heavy multi-layer shadows). */
    val ElevationCard: Dp = 2.dp

    val Space8: Dp = 8.dp
    val Space12: Dp = 12.dp
    val Space16: Dp = 16.dp
    val Space20: Dp = 20.dp
    val Space24: Dp = 24.dp
    val Space28: Dp = 28.dp

    val SheetHandleWidth: Dp = 36.dp
    val SheetHandleHeight: Dp = 4.dp
}
