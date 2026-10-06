package com.piplanner.android.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val fullDisplayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    
    fun formatMonthYear(date: LocalDate): String {
        return date.format(displayFormatter)
    }
    
    fun formatFullDate(date: LocalDate): String {
        return date.format(fullDisplayFormatter)
    }
    
    fun formatShortDate(date: LocalDate): String {
        return date.format(shortDateFormatter)
    }
    
    fun formatTime(dateTime: LocalDateTime): String {
        return dateTime.format(timeFormatter)
    }
    
    fun formatRelativeTime(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val minutesAgo = ChronoUnit.MINUTES.between(dateTime, now)
        val hoursAgo = ChronoUnit.HOURS.between(dateTime, now)
        val daysAgo = ChronoUnit.DAYS.between(dateTime, now)
        
        return when {
            minutesAgo < 1 -> "Just now"
            minutesAgo < 60 -> "${minutesAgo}m ago"
            hoursAgo < 24 -> "${hoursAgo}h ago"
            daysAgo == 1L -> "Yesterday"
            daysAgo < 7 -> "${daysAgo}d ago"
            else -> formatFullDate(dateTime.toLocalDate())
        }
    }
    
    fun monthsBetween(start: LocalDate, end: LocalDate): Long {
        return ChronoUnit.MONTHS.between(start, end)
    }
    
    fun getGreeting(): String {
        val hour = LocalDateTime.now().hour
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
