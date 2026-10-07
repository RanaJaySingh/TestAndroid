package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.GoalAllocation
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import org.junit.Before
import org.junit.Test

class HistoryServiceTest {

    private lateinit var service: HistoryService

    @Before
    fun setUp() {
        service = HistoryService(FormattingService())
    }

    @Test
    fun entriesNewestFirst_ordersByCreatedAtDescending() {
        val older = entry(
            id = "a",
            type = HistoryEntryType.OpeningBalance,
            createdAt = "2024-01-01T00:00:00Z",
            locked = true,
            credit = 10_000_000L,
        )
        val newer = entry(
            id = "b",
            type = HistoryEntryType.NewCredit,
            createdAt = "2024-02-01T00:00:00Z",
            locked = true,
            credit = 5_000_000L,
        )
        val newest = entry(
            id = "c",
            type = HistoryEntryType.Transfer,
            createdAt = "2024-03-01T00:00:00Z",
            locked = true,
            transfer = 100_000L,
        )

        val ordered = service.entriesNewestFirst(listOf(older, newest, newer))

        assertThat(ordered.map { it.id }).containsExactly("c", "b", "a").inOrder()
    }

    @Test
    fun typeLabelsAndIcons_coverAllEntryTypes() {
        HistoryEntryType.entries.forEach { type ->
            assertThat(service.typeLabel(type)).isNotEmpty()
            assertThat(service.typeIcon(type)).isNotEmpty()
        }
        assertThat(service.typeLabel(HistoryEntryType.OpeningBalance))
            .isEqualTo("Opening balance")
        assertThat(service.typeLabel(HistoryEntryType.NewCredit)).isEqualTo("New credit")
        assertThat(service.typeLabel(HistoryEntryType.Transfer)).isEqualTo("Transfer")
        assertThat(service.typeLabel(HistoryEntryType.Withdrawal)).isEqualTo("Withdrawal")
        assertThat(service.typeLabel(HistoryEntryType.GoalDeleted))
            .isEqualTo(DeleteGoalService.historyTitle)
    }

    @Test
    fun openAssignable_onlyUnlockedNewCredit() {
        val open = entry(
            id = "open",
            type = HistoryEntryType.NewCredit,
            createdAt = "2024-02-01T00:00:00Z",
            locked = false,
            credit = 1_000_000L,
        )
        val locked = open.copy(id = "locked", isLocked = true)
        val opening = entry(
            id = "opening",
            type = HistoryEntryType.OpeningBalance,
            createdAt = "2024-01-01T00:00:00Z",
            locked = true,
            credit = 10_000_000L,
        )

        assertThat(service.isOpenAssignable(open)).isTrue()
        assertThat(service.isOpenAssignable(locked)).isFalse()
        assertThat(service.isOpenAssignable(opening)).isFalse()
        assertThat(service.rowSubtitle(open)).isEqualTo(HistoryService.ASSIGN_NOW_SUBTITLE)
    }

    @Test
    fun openingBalance_alwaysReadOnly_andNavigatesToOpeningPath() {
        val opening = entry(
            id = "opening",
            type = HistoryEntryType.OpeningBalance,
            createdAt = "2024-01-01T00:00:00Z",
            locked = true,
            credit = 10_000_000L,
        )
        assertThat(service.isReadOnly(opening)).isTrue()
        assertThat(service.navigationTarget(opening))
            .isEqualTo(HistoryNavigationTarget.OpeningBalanceReadOnly("opening"))
    }

    @Test
    fun lockedCredit_navigatesToCreditEntry_openCreditSame() {
        val open = entry(
            id = "open",
            type = HistoryEntryType.NewCredit,
            createdAt = "2024-02-01T00:00:00Z",
            locked = false,
            credit = 1_000_000L,
        )
        val locked = open.copy(id = "locked", isLocked = true)
        assertThat(service.navigationTarget(open))
            .isEqualTo(HistoryNavigationTarget.CreditEntry("open"))
        assertThat(service.navigationTarget(locked))
            .isEqualTo(HistoryNavigationTarget.CreditEntry("locked"))
        assertThat(service.isReadOnly(locked)).isTrue()
    }

    @Test
    fun transferWithdrawalDeleted_navigateToLockedDetail() {
        val transfer = entry(
            id = "t",
            type = HistoryEntryType.Transfer,
            createdAt = "2024-03-01T00:00:00Z",
            locked = true,
            transfer = 50_000L,
        )
        val withdrawal = entry(
            id = "w",
            type = HistoryEntryType.Withdrawal,
            createdAt = "2024-03-02T00:00:00Z",
            locked = true,
            withdrawal = 80_000L,
        )
        val deleted = entry(
            id = "d",
            type = HistoryEntryType.GoalDeleted,
            createdAt = "2024-03-03T00:00:00Z",
            locked = true,
            released = 100_000L,
            deletedName = "Vacation",
        )

        assertThat(service.navigationTarget(transfer))
            .isEqualTo(HistoryNavigationTarget.LockedDetail("t"))
        assertThat(service.navigationTarget(withdrawal))
            .isEqualTo(HistoryNavigationTarget.LockedDetail("w"))
        assertThat(service.navigationTarget(deleted))
            .isEqualTo(HistoryNavigationTarget.LockedDetail("d"))
        assertThat(service.rowSubtitle(deleted)).contains("Vacation")
    }

    @Test
    fun originalAmountsCaption_matchesFrame12a() {
        assertThat(HistoryService.ORIGINAL_AMOUNTS_CAPTION)
            .isEqualTo("Original amounts never change")
    }

    @Test
    fun formattedPrimaryAmount_usesIndianGrouping() {
        val opening = entry(
            id = "opening",
            type = HistoryEntryType.OpeningBalance,
            createdAt = "2024-01-01T00:00:00Z",
            locked = true,
            credit = 10_000_000L,
        )
        assertThat(service.formattedPrimaryAmount(opening)).isEqualTo("₹1,00,000")
    }

    private fun entry(
        id: String,
        type: HistoryEntryType,
        createdAt: String,
        locked: Boolean,
        credit: Long? = null,
        transfer: Long? = null,
        withdrawal: Long? = null,
        released: Long? = null,
        deletedName: String? = null,
    ): HistoryEntry {
        return HistoryEntry(
            id = id,
            type = type,
            createdAt = createdAt,
            isLocked = locked,
            creditAmount = credit,
            transferAmount = transfer,
            withdrawalAmount = withdrawal,
            releasedAmount = released,
            deletedGoalName = deletedName,
            allocations = if (credit != null && type == HistoryEntryType.OpeningBalance) {
                listOf(
                    GoalAllocation(
                        goalId = "g1",
                        goalName = "Car",
                        amount = credit,
                        percentage = 1.0,
                    ),
                )
            } else {
                emptyList()
            },
        )
    }
}
