package com.piplanner.domain

import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * History tab helpers — Spec §3.1 HistoryEntry, BR-3, design frames 12 / 12a.
 * List is newest-first; locked entries are read-only; Opening balance always read-only.
 */
@Singleton
class HistoryService @Inject constructor(
    private val formattingService: FormattingService,
) {

    /** Newest first by [HistoryEntry.createdAt] (ISO-8601 Instant). Stable for equal timestamps. */
    fun entriesNewestFirst(history: List<HistoryEntry>): List<HistoryEntry> {
        return history.sortedWith(
            compareByDescending<HistoryEntry> { parseInstant(it.createdAt) }
                .thenByDescending { it.id },
        )
    }

    fun typeLabel(type: HistoryEntryType): String {
        return when (type) {
            HistoryEntryType.OpeningBalance -> TYPE_OPENING
            HistoryEntryType.NewCredit -> TYPE_NEW_CREDIT
            HistoryEntryType.Transfer -> TYPE_TRANSFER
            HistoryEntryType.Withdrawal -> TYPE_WITHDRAWAL
            // List label (AC / PIP-59). Writer title "Deleted / moved" stays on DeleteGoalService.
            HistoryEntryType.GoalDeleted -> TYPE_GOAL_DELETED
        }
    }

    /** Distinguishing glyph for the row icon (Compose has no Material Icons dependency). */
    fun typeIcon(type: HistoryEntryType): String {
        return when (type) {
            HistoryEntryType.OpeningBalance -> "◎"
            HistoryEntryType.NewCredit -> "+"
            HistoryEntryType.Transfer -> "↔"
            HistoryEntryType.Withdrawal -> "−"
            HistoryEntryType.GoalDeleted -> "×"
        }
    }

    fun isOpenAssignable(entry: HistoryEntry): Boolean {
        return entry.type == HistoryEntryType.NewCredit && !entry.isLocked
    }

    /** Opening balance is always read-only (BR-3); any locked entry is read-only. */
    fun isReadOnly(entry: HistoryEntry): Boolean {
        return entry.type == HistoryEntryType.OpeningBalance || entry.isLocked
    }

    fun primaryAmountPaisa(entry: HistoryEntry): Long {
        return when (entry.type) {
            HistoryEntryType.OpeningBalance ->
                entry.creditAmount ?: entry.newBalance ?: entry.allocations.sumOf { it.amount }
            HistoryEntryType.NewCredit ->
                entry.creditAmount ?: 0L
            HistoryEntryType.Transfer ->
                entry.transferAmount ?: 0L
            HistoryEntryType.Withdrawal ->
                entry.withdrawalAmount ?: 0L
            HistoryEntryType.GoalDeleted ->
                entry.releasedAmount ?: entry.allocations.sumOf { it.amount }
        }
    }

    fun formattedPrimaryAmount(entry: HistoryEntry): String {
        return formattingService.formatInrFromPaisa(primaryAmountPaisa(entry))
    }

    /**
     * Secondary label under the type: "Assign now" for open credits,
     * Typed / Custom split badges, deleted goal name, or date.
     */
    fun rowSubtitle(entry: HistoryEntry): String {
        if (isOpenAssignable(entry)) {
            return ASSIGN_NOW_SUBTITLE
        }
        val date = entry.createdAt.take(10)
        return when (entry.type) {
            HistoryEntryType.NewCredit -> {
                if (entry.isTyped) "$TYPED_BADGE · $date" else date
            }
            HistoryEntryType.GoalDeleted -> {
                val name = entry.deletedGoalName
                if (name.isNullOrBlank()) date else "$name · $date"
            }
            else -> date
        }
    }

    fun navigationTarget(entry: HistoryEntry): HistoryNavigationTarget {
        return when {
            isOpenAssignable(entry) -> HistoryNavigationTarget.CreditEntry(entry.id)
            entry.type == HistoryEntryType.OpeningBalance ->
                HistoryNavigationTarget.OpeningBalanceReadOnly(entry.id)
            else -> HistoryNavigationTarget.LockedDetail(entry.id)
        }
    }

    private fun parseInstant(iso: String): Instant {
        return try {
            Instant.parse(iso)
        } catch (_: Exception) {
            Instant.EPOCH
        }
    }

    companion object {
        const val TYPE_OPENING: String = "Opening balance"
        const val TYPE_NEW_CREDIT: String = "New credit"
        const val TYPE_TRANSFER: String = "Transfer"
        const val TYPE_WITHDRAWAL: String = "Withdrawal"
        /** History list label for GoalDeleted (AC2 / PIP-59 parity). */
        const val TYPE_GOAL_DELETED: String = "Goal deleted"
        const val ASSIGN_NOW_SUBTITLE: String = "Assign now"
        const val TYPED_BADGE: String = "Typed"

        /** Frame 12a / BR-3 read-only History copy. */
        const val ORIGINAL_AMOUNTS_CAPTION: String = "Original amounts never change"

        const val EMPTY_STATE_BODY: String = "No history yet. Lock an opening split or assign a credit to see entries here."
    }
}

sealed class HistoryNavigationTarget {
    data class CreditEntry(val entryId: String) : HistoryNavigationTarget()
    data class OpeningBalanceReadOnly(val entryId: String) : HistoryNavigationTarget()
    data class LockedDetail(val entryId: String) : HistoryNavigationTarget()
}
