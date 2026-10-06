package com.piplanner.android.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val indianLocale = Locale("en", "IN")
    
    fun formatIndianRupees(amount: Long): String {
        return "₹${formatIndianNumber(amount)}"
    }
    
    fun formatIndianRupees(amount: Double): String {
        return "₹${formatIndianNumber(amount.toLong())}"
    }
    
    fun formatIndianNumber(number: Long): String {
        if (number < 0) {
            return "-${formatIndianNumber(-number)}"
        }
        
        if (number < 1000) {
            return number.toString()
        }
        
        val numberStr = number.toString()
        val length = numberStr.length
        
        val result = StringBuilder()
        
        val lastThreeDigits = numberStr.takeLast(3)
        val remainingDigits = numberStr.dropLast(3)
        
        if (remainingDigits.isNotEmpty()) {
            val chunks = mutableListOf<String>()
            var remaining = remainingDigits
            
            while (remaining.length > 2) {
                chunks.add(0, remaining.takeLast(2))
                remaining = remaining.dropLast(2)
            }
            if (remaining.isNotEmpty()) {
                chunks.add(0, remaining)
            }
            
            result.append(chunks.joinToString(","))
            result.append(",")
        }
        
        result.append(lastThreeDigits)
        
        return result.toString()
    }
    
    fun parseIndianNumber(text: String): Long {
        val cleanText = text
            .replace("₹", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()
        
        return cleanText.toLongOrNull() ?: 0L
    }
    
    fun formatCompact(amount: Long): String {
        return when {
            amount >= 10_000_000 -> "₹${amount / 10_000_000}Cr"
            amount >= 100_000 -> "₹${amount / 100_000}L"
            amount >= 1000 -> "₹${amount / 1000}K"
            else -> formatIndianRupees(amount)
        }
    }
}
