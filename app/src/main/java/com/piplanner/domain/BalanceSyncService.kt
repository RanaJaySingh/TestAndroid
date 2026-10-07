package com.piplanner.domain

/**
 * Spec §3.3 — BalanceSyncService (mock for local demo).
 * Amounts are Long paisa; never Float/Double.
 */
interface BalanceSyncService {
    /** Mock: returns seeded demo balance for a known account. */
    suspend fun fetchBalance(accountId: String): Result<Long>

    /** Mock: succeeds only with demo PIN `"1234"`. */
    suspend fun verifyUpiPin(pin: String): Result<Long>
}

enum class SyncError {
    NetworkError,
    PermissionDenied,
    AccountNotFound,
}

enum class PinError {
    WrongPin,
    Cancelled,
    OtherApp,
}

class SyncException(val error: SyncError) : Exception(error.name)

class PinException(val error: PinError) : Exception(error.name)

/**
 * Demo Balance sync used by Consent Yes / UPI PIN paths (PRD R3 / R4).
 */
class MockBalanceSyncService(
    private val knownAccountIds: Set<String> = emptySet(),
) : BalanceSyncService {

    override suspend fun fetchBalance(accountId: String): Result<Long> {
        if (knownAccountIds.isNotEmpty() && accountId !in knownAccountIds) {
            return Result.failure(SyncException(SyncError.AccountNotFound))
        }
        return Result.success(DEMO_BALANCE_PAISA)
    }

    override suspend fun verifyUpiPin(pin: String): Result<Long> {
        return if (pin == DEMO_PIN) {
            Result.success(DEMO_BALANCE_PAISA)
        } else {
            Result.failure(PinException(PinError.WrongPin))
        }
    }

    /** Frame 4c — account registered on another UPI app (forces manual entry). */
    fun accountOnOtherUpiApp(): PinError = PinError.OtherApp

    companion object {
        /** Demo UPI PIN accepted by the mock pad (Spec §5.3). */
        const val DEMO_PIN: String = "1234"

        /** Seeded opening balance — ₹1,00,000 (PRD R3 / persona). */
        const val DEMO_BALANCE_PAISA: Long = 10_000_000L
    }
}

/**
 * Pure helpers for Consent / manual balance entry (PRD R3 / R4).
 */
object ConsentService {
    /** Consent sheet bullets (design frame 3 / PRD R3). */
    val consentBullets: List<String> = listOf(
        "Reads only this account’s balance",
        "Stores the last balance we checked",
        "Never sends statements, payees, UPI IDs, or account numbers to Grok",
        "You can change this anytime in Settings",
    )

    /** Continue on Manual amount (4a) is disabled at ₹0. */
    fun canContinueManual(amountPaisa: Long): Boolean = amountPaisa > 0L

    /** Parses whole-rupee digit string into paisa. Non-digits ignored; empty → 0. */
    fun paisaFromRupeeDigits(digits: String): Long {
        val filtered = digits.filter { it.isDigit() }
        if (filtered.isEmpty()) return 0L
        val rupees = filtered.toLongOrNull() ?: return 0L
        return rupees * FormattingService.PAISA_PER_RUPEE
    }

    fun isCompletePin(pin: String): Boolean =
        pin.length == 4 && pin.all { it.isDigit() }
}
