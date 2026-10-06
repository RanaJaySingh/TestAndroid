package com.piplanner.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Spec §5.3 / BR-10 / R20 — INR with Indian grouping (pattern `##,##,##0`, e.g. ₹1,00,000).
 * Amounts are Long paisa; display uses whole rupees (no paise).
 */
@Singleton
class FormattingService @Inject constructor() {

    /**
     * Formats [paisa] as Indian INR, e.g. 10_000_000 paisa → "₹1,00,000".
     */
    fun formatInrFromPaisa(paisa: Long): String {
        return formatInrFromRupees(paisa / PAISA_PER_RUPEE)
    }

    /**
     * Formats whole [rupees] with ₹ and Indian grouping (`##,##,##0`).
     */
    fun formatInrFromRupees(rupees: Long): String {
        val negative = rupees < 0
        val grouped = applyIndianGrouping(abs(rupees))
        val sign = if (negative) "-" else ""
        return "$sign$RUPEE_SYMBOL$grouped"
    }

    /**
     * Indian grouping: last three digits, then groups of two (Spec pattern ##,##,##0).
     */
    private fun applyIndianGrouping(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) {
            return digits
        }
        val lastThree = digits.takeLast(3)
        var remaining = digits.dropLast(3)
        val segments = ArrayDeque<String>()
        while (remaining.length > 2) {
            segments.addFirst(remaining.takeLast(2))
            remaining = remaining.dropLast(2)
        }
        if (remaining.isNotEmpty()) {
            segments.addFirst(remaining)
        }
        return segments.joinToString(",") + "," + lastThree
    }

    companion object {
        const val RUPEE_SYMBOL: String = "₹"
        const val PAISA_PER_RUPEE: Long = 100L
        /** Spec §5.3 DecimalFormat pattern for Indian grouping. */
        const val INDIAN_PATTERN: String = "##,##,##0"
    }
}
