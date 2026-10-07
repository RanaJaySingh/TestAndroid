package com.piplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography hierarchy tokens (Tech Spec §3.1).
 * Platform default fonts; hierarchy matches design (amount hero > title > body > caption).
 */
object PiPlannerTypography {
    val amountHero: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp,
    )

    val title: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    )

    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    /** Material3 Typography wired from shared tokens for [PiPlannerTheme]. */
    val material: Typography = Typography(
        displayLarge = amountHero,
        headlineLarge = title.copy(fontSize = 28.sp, lineHeight = 36.sp),
        titleLarge = title,
        titleMedium = title.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = body,
        bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = caption.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = caption.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = caption.copy(fontWeight = FontWeight.Medium),
        labelSmall = caption.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    )
}
