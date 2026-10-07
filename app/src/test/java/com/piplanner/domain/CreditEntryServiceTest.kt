package com.piplanner.domain

import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.GoalAllocation
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import com.piplanner.util.DemoData
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CreditEntryServiceTest {

    private lateinit var openingSplitService: OpeningSplitService
    private lateinit var standingSplitService: StandingSplitService
    private lateinit var service: CreditEntryService

    private val carId = DemoData.DEMO_CAR_GOAL_ID
    private val emergencyId = DemoData.DEMO_EMERGENCY_GOAL_ID
    private val hdfcId = DemoData.DEMO_SAVINGS_ACCOUNT_ID
    private val entryId = "cccccccc-cccc-cccc-cccc-cccccccccccc"
    private val createdAt = "2023-11-14T12:01:40Z"

    @Before
    fun setUp() {
        openingSplitService = OpeningSplitService()
        standingSplitService = StandingSplitService(openingSplitService)
        service = CreditEntryService(openingSplitService, standingSplitService)
    }

    // MARK: - Balance compare (R7 / R26 / 10b)

    @Test
    fun higherBalanceProducesCreditAmount() {
        val result = service.compare(previousBalance = 10_000_000L, newBalance = 11_000_000L)
        assertThat(result).isEqualTo(
            BalanceCompareResult.Higher(
                creditAmount = 1_000_000L,
                previousBalance = 10_000_000L,
                newBalance = 11_000_000L,
            ),
        )
    }

    @Test
    fun sameBalanceIsNoOp() {
        assertThat(service.compare(10_000_000L, 10_000_000L))
            .isEqualTo(BalanceCompareResult.Same)
        assertThat(CreditEntryService.NO_NEW_CREDIT_MESSAGE)
            .isEqualTo("No new credit since the last sync.")
    }

    @Test
    fun lowerBalanceProducesShortfall() {
        val result = service.compare(previousBalance = 10_000_000L, newBalance = 9_000_000L)
        assertThat(result).isEqualTo(
            BalanceCompareResult.Lower(
                shortfall = 1_000_000L,
                previousBalance = 10_000_000L,
                newBalance = 9_000_000L,
            ),
        )
    }

    // MARK: - Split validation (BR-2 / this-credit defaults)

    @Test
    fun defaultPercentagesFromStandingSplit() {
        val goals = sampleGoals(savedCar = 6_000_000L, savedEmergency = 4_000_000L)
        val standing = listOf(
            StandingSplit(goalId = carId, percentage = 0.60),
            StandingSplit(goalId = emergencyId, percentage = 0.40),
        )
        val map = service.defaultPercentages(goals, standing)
        assertThat(map[carId]).isEqualTo(BigDecimal("0.6000"))
        assertThat(map[emergencyId]).isEqualTo(BigDecimal("0.4000"))
        assertThat(openingSplitService.isValidHundredPercent(map.values.toList())).isTrue()
    }

    @Test
    fun singleGoalAutoAssignsHundredPercent() {
        val goal = sampleGoals(savedCar = 10_000_000L, savedEmergency = 0L)[0]
        val map = service.defaultPercentages(listOf(goal), emptyList())
        assertThat(map[carId]).isEqualTo(BigDecimal.ONE)
        assertThat(openingSplitService.isValidHundredPercent(map.values.toList())).isTrue()
    }

    @Test
    fun canSaveRequiresHundredPercentAndOpenEntry() {
        val entry = sampleOpenEntry()
        assertThat(
            service.canSaveAndLock(
                entry,
                listOf(BigDecimal("0.60"), BigDecimal("0.40")),
            ),
        ).isTrue()
        assertThat(
            service.canSaveAndLock(
                entry,
                listOf(BigDecimal("0.50"), BigDecimal("0.30")),
            ),
        ).isFalse()
        assertThat(
            service.canSaveAndLock(
                entry.copy(isLocked = true),
                listOf(BigDecimal("0.60"), BigDecimal("0.40")),
            ),
        ).isFalse()
    }

    // MARK: - Entry state machine (open → lock / BR-5 / BR-6)

    @Test
    fun processFetchedHigherCreatesOpenEntry() {
        val state = samplePostSetupState()
        val outcome = service.processFetchedBalance(
            state = state,
            fetchedBalance = 11_000_000L,
            dedicatedAccountId = hdfcId,
            isTyped = false,
            id = entryId,
            createdAt = createdAt,
        )

        val created = outcome as CreditProcessOutcome.OpenCreditCreated
        assertThat(created.entry.id).isEqualTo(entryId)
        assertThat(created.entry.type).isEqualTo(HistoryEntryType.NewCredit)
        assertThat(created.entry.isLocked).isFalse()
        assertThat(created.entry.creditAmount).isEqualTo(1_000_000L)
        assertThat(created.entry.previousBalance).isEqualTo(10_000_000L)
        assertThat(created.entry.newBalance).isEqualTo(11_000_000L)
        assertThat(created.entry.isTyped).isFalse()
        assertThat(created.entry.allocations.sumOf { it.amount }).isEqualTo(1_000_000L)
        assertThat(created.state.accounts.first { it.isDedicated }.balance).isEqualTo(11_000_000L)
        assertThat(service.isSyncOrUpdateBlocked(created.state.history)).isTrue()
        assertThat(service.openCreditEntry(created.state.history)?.id).isEqualTo(entryId)
    }

    @Test
    fun processFetchedSameReturnsNoNewCreditWithoutHistoryWrite() {
        val state = samplePostSetupState()
        val outcome = service.processFetchedBalance(
            state = state,
            fetchedBalance = 10_000_000L,
            dedicatedAccountId = hdfcId,
            isTyped = false,
            createdAt = createdAt,
        )
        val noOp = outcome as CreditProcessOutcome.NoNewCredit
        assertThat(noOp.message).isEqualTo(CreditEntryService.NO_NEW_CREDIT_MESSAGE)
        assertThat(state.history).hasSize(1)
    }

    @Test
    fun processFetchedLowerTriggersWithdrawalOutcome() {
        val state = samplePostSetupState()
        val outcome = service.processFetchedBalance(
            state = state,
            fetchedBalance = 8_500_000L,
            dedicatedAccountId = hdfcId,
            isTyped = false,
            createdAt = createdAt,
        )
        val withdrawal = outcome as CreditProcessOutcome.WithdrawalRequired
        assertThat(withdrawal.shortfall).isEqualTo(1_500_000L)
        assertThat(withdrawal.previousBalance).isEqualTo(10_000_000L)
        assertThat(withdrawal.newBalance).isEqualTo(8_500_000L)
        assertThat(service.openCreditEntry(state.history)).isNull()
    }

    @Test
    fun typedCreditSetsIsTypedFlag() {
        val state = samplePostSetupState()
        val outcome = service.processFetchedBalance(
            state = state,
            fetchedBalance = 12_000_000L,
            dedicatedAccountId = hdfcId,
            isTyped = true,
            id = entryId,
            createdAt = createdAt,
        )
        val created = outcome as CreditProcessOutcome.OpenCreditCreated
        assertThat(created.entry.isTyped).isTrue()
        assertThat(created.entry.creditAmount).isEqualTo(2_000_000L)
    }

    @Test
    fun openEntryBlocksSecondSync() {
        val first = service.processFetchedBalance(
            state = samplePostSetupState(),
            fetchedBalance = 11_000_000L,
            dedicatedAccountId = hdfcId,
            isTyped = false,
            id = entryId,
            createdAt = createdAt,
        ) as CreditProcessOutcome.OpenCreditCreated

        assertThat(service.isSyncOrUpdateBlocked(first.state.history)).isTrue()

        val error = assertThrows(CreditEntryException::class.java) {
            service.processFetchedBalance(
                state = first.state,
                fetchedBalance = 12_000_000L,
                dedicatedAccountId = hdfcId,
                isTyped = false,
                createdAt = createdAt,
            )
        }
        assertThat(error.message).contains("open credit")
    }

    @Test
    fun saveAndLockUpdatesSavedAmountsAndFreezesEntry() {
        val created = service.processFetchedBalance(
            state = samplePostSetupState(),
            fetchedBalance = 11_000_000L,
            dedicatedAccountId = hdfcId,
            isTyped = false,
            id = entryId,
            createdAt = createdAt,
        ) as CreditProcessOutcome.OpenCreditCreated

        val lockedState = service.applyCreditLock(
            to = created.state,
            entryId = entryId,
            percentages = mapOf(
                carId to BigDecimal("0.70"),
                emergencyId to BigDecimal("0.30"),
            ),
            useThisSplitForStanding = false,
            nowIso = createdAt,
        )

        val locked = lockedState.history.first { it.id == entryId }
        assertThat(locked.isLocked).isTrue()
        assertThat(locked.allocations.sumOf { it.amount }).isEqualTo(1_000_000L)

        val car = lockedState.goals.first { it.id == carId }
        val emergency = lockedState.goals.first { it.id == emergencyId }
        assertThat(car.savedAmount).isEqualTo(6_700_000L)
        assertThat(emergency.savedAmount).isEqualTo(4_300_000L)
        assertThat(car.shareOfNewCredits).isEqualTo(0.60)
        assertThat(lockedState.standingSplits.first { it.goalId == carId }.percentage)
            .isEqualTo(0.60)

        assertThat(service.isSyncOrUpdateBlocked(lockedState.history)).isFalse()
        assertThat(service.openCreditEntry(lockedState.history)).isNull()

        assertThrows(CreditEntryException::class.java) {
            service.applyCreditLock(
                to = lockedState,
                entryId = entryId,
                percentages = mapOf(
                    carId to BigDecimal("0.50"),
                    emergencyId to BigDecimal("0.50"),
                ),
                useThisSplitForStanding = false,
                nowIso = createdAt,
            )
        }
    }

    @Test
    fun useThisSplitUpdatesStandingSplit() {
        val created = service.processFetchedBalance(
            state = samplePostSetupState(),
            fetchedBalance = 11_000_000L,
            dedicatedAccountId = hdfcId,
            isTyped = false,
            id = entryId,
            createdAt = createdAt,
        ) as CreditProcessOutcome.OpenCreditCreated

        val lockedState = service.applyCreditLock(
            to = created.state,
            entryId = entryId,
            percentages = mapOf(
                carId to BigDecimal("0.25"),
                emergencyId to BigDecimal("0.75"),
            ),
            useThisSplitForStanding = true,
            nowIso = createdAt,
        )

        assertThat(lockedState.standingSplits.first { it.goalId == carId }.percentage)
            .isEqualTo(0.25)
        assertThat(lockedState.standingSplits.first { it.goalId == emergencyId }.percentage)
            .isEqualTo(0.75)
        assertThat(lockedState.goals.first { it.id == carId }.shareOfNewCredits)
            .isEqualTo(0.25)
    }

    @Test
    fun bannerMessageIncludesAmountAndAssignNow() {
        val formatting = FormattingService()
        val message = service.openEntryBannerMessage(
            creditAmount = 1_000_000L,
            formatting = formatting,
        )
        assertThat(message).contains(CreditEntryService.OPEN_ENTRY_BANNER_PREFIX)
        assertThat(message).contains(CreditEntryService.ASSIGN_NOW_TITLE)
        assertThat(message).contains("₹10,000")
    }

    @Test
    fun percentagesForNextCredit_prefersStandingSplits() {
        val state = samplePostSetupState()
        val map = standingSplitService.percentagesForNextCredit(state)
        assertThat(map[carId]).isEqualTo(BigDecimal("0.6000"))
        assertThat(map[emergencyId]).isEqualTo(BigDecimal("0.4000"))
    }

    // MARK: - Fixtures

    private fun sampleGoals(savedCar: Long, savedEmergency: Long): List<Goal> {
        return listOf(
            Goal(
                id = carId,
                name = "Car",
                targetAmount = 50_000_000L,
                startDate = "2023-11-14",
                endDate = "2024-11-14",
                inflationRate = 0.07,
                savedAmount = savedCar,
                shareOfNewCredits = 0.60,
                createdAt = "2023-11-14T12:00:00Z",
                updatedAt = "2023-11-14T12:00:00Z",
            ),
            Goal(
                id = emergencyId,
                name = "Emergency Fund",
                targetAmount = 20_000_000L,
                startDate = "2023-11-14",
                endDate = "2024-11-14",
                inflationRate = 0.07,
                savedAmount = savedEmergency,
                shareOfNewCredits = 0.40,
                createdAt = "2023-11-14T12:00:00Z",
                updatedAt = "2023-11-14T12:00:00Z",
            ),
        )
    }

    private fun samplePostSetupState(): AppState {
        val goals = sampleGoals(savedCar = 6_000_000L, savedEmergency = 4_000_000L)
        val opening = HistoryEntry(
            id = "dddddddd-dddd-dddd-dddd-dddddddddddd",
            type = HistoryEntryType.OpeningBalance,
            createdAt = "2023-11-14T12:00:00Z",
            isLocked = true,
            previousBalance = null,
            newBalance = 10_000_000L,
            creditAmount = 10_000_000L,
            isTyped = true,
            allocations = listOf(
                GoalAllocation(
                    goalId = carId,
                    goalName = "Car",
                    amount = 6_000_000L,
                    percentage = 0.60,
                ),
                GoalAllocation(
                    goalId = emergencyId,
                    goalName = "Emergency Fund",
                    amount = 4_000_000L,
                    percentage = 0.40,
                ),
            ),
        )
        return AppState(
            accounts = listOf(
                Account(
                    id = hdfcId,
                    bankName = "HDFC",
                    maskedNumber = "••4821",
                    balance = 10_000_000L,
                    isDedicated = true,
                    isPaytmLinked = true,
                    consentAutoUpdate = true,
                ),
            ),
            goals = goals,
            history = listOf(opening),
            standingSplits = listOf(
                StandingSplit(goalId = carId, percentage = 0.60),
                StandingSplit(goalId = emergencyId, percentage = 0.40),
            ),
            hasCompletedSetup = true,
        )
    }

    private fun sampleOpenEntry(): HistoryEntry {
        return service.createOpenCreditEntry(
            goals = sampleGoals(savedCar = 6_000_000L, savedEmergency = 4_000_000L),
            standingSplits = listOf(
                StandingSplit(goalId = carId, percentage = 0.60),
                StandingSplit(goalId = emergencyId, percentage = 0.40),
            ),
            previousBalance = 10_000_000L,
            newBalance = 11_000_000L,
            creditAmount = 1_000_000L,
            isTyped = false,
            id = entryId,
            createdAt = createdAt,
        )
    }
}
