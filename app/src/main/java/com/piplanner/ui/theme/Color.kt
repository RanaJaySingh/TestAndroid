package com.piplanner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Shared PiPlanner colour tokens (Tech Spec §3.1 / PRD R1).
 * Navy range #0A2A6B–#003A8C; replaces forest-green/cream brand chrome.
 */
object PiPlannerColors {
    /** color.navy.primary — balance card fill / primary CTA */
    val NavyPrimary: Color = Color(0xFF0A2A6B)

    /** color.navy.deep — gradient/end accent within approved range */
    val NavyDeep: Color = Color(0xFF003A8C)

    /** color.chip.lightBlue — soft blue fill for Ask / Transfer chips */
    val ChipLightBlue: Color = Color(0xFFD6EBFF)

    /** Readable label on light-blue chips */
    val OnChipLightBlue: Color = Color(0xFF0A2A6B)

    /** color.positive.green — +₹ / On track */
    val PositiveGreen: Color = Color(0xFF1B8A4A)

    /** color.behind — Behind status (amber) */
    val Behind: Color = Color(0xFFE65100)

    /** Destructive / errors (PIN error, delete) */
    val Destructive: Color = Color(0xFFC62828)

    /** color.surface.card — white card surfaces */
    val SurfaceCard: Color = Color(0xFFFFFFFF)

    /** color.background.app — light app chrome (not cream #F7F4EF) */
    val BackgroundApp: Color = Color(0xFFF5F7FB)

    val OnNavy: Color = Color(0xFFFFFFFF)
    val OnBackground: Color = Color(0xFF1C1B1F)
    val OnSurface: Color = Color(0xFF1C1B1F)
    val OutlineMuted: Color = Color(0xFFC5CDD8)
}
