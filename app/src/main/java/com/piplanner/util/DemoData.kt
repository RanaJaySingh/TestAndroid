package com.piplanner.util

import com.piplanner.data.model.Goal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Persona constants (PIP-66) plus a minimal Opening-split demo seed so PIP-44 is reachable
 * before Welcome → Goals setup screens land.
 */
object DemoData {
    const val PERSONA_NAME: String = "Rahul"
    const val SAVINGS_BANK: String = "HDFC"
    const val SAVINGS_MASKED: String = "••4821"
    const val SPENDING_BANK: String = "SBI"
    const val SPENDING_MASKED: String = "••7730"
    const val SAVINGS_OPENING_BALANCE_PAISA: Long = 10_000_000L
    const val SPENDING_BALANCE_PAISA: Long = 7_200_000L

    const val DEMO_CAR_GOAL_ID: String = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    const val DEMO_EMERGENCY_GOAL_ID: String = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"

    /** Car 60% / Emergency 40% — matches design happy-path proposal for Opening split demos. */
    fun sampleOpeningSplitGoals(now: Instant = Instant.now()): List<Goal> {
        val start = LocalDate.ofInstant(now, ZoneOffset.UTC)
        val end = start.plusYears(1)
        val createdAt = now.toString()
        return listOf(
            Goal(
                id = DEMO_CAR_GOAL_ID,
                name = "Car",
                targetAmount = 50_000_000L,
                startDate = start.toString(),
                endDate = end.toString(),
                savedAmount = 0L,
                shareOfNewCredits = 0.6,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
            Goal(
                id = DEMO_EMERGENCY_GOAL_ID,
                name = "Emergency Fund",
                targetAmount = 20_000_000L,
                startDate = start.toString(),
                endDate = end.toString(),
                savedAmount = 0L,
                shareOfNewCredits = 0.4,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
        )
    }
}
