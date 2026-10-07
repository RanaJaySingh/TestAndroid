# PIP-40 — Android Consent sheet and balance entry flows

Ticket: https://linear.app/telco-paytm/issue/PIP-40/android-implement-consent-sheet-and-balance-entry-flows

## Delivered

Consent Yes/No + balance entry under `ui/setup` with `ConsentViewModel`, mock `BalanceSyncService` (Spec §3.3), Manual amount / Demo UPI PIN / other-app / wrong-PIN states, wiring from Accounts (replacing `ConsentPlaceholder`), handoff of resolved opening balance into Opening split, and unit tests.

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| R3 Consent Yes → ₹1,00,000 (3a) | `ConsentViewModel.chooseConsentYes` → `MockBalanceSyncService.fetchBalance` → `FetchedBalanceScreen` |
| R4 Consent No → Update balance (4) | `chooseConsentNo` → `UpdateBalanceSheet` (Manually / Balance sync) |
| Manual ₹0 disables Continue (4a) | `ConsentService.canContinueManual` + `ManualBalanceScreen` |
| UPI PIN Demo `"1234"` success (4b) | `MockBalanceSyncService.verifyUpiPin` → fetched balance |
| Wrong PIN → retry/manual (4d/4e) | `WrongPinScreen` |
| Other UPI app → force manual (4c) | `OtherAppScreen` via “Account on another UPI app” |
| Spec §3.3 BalanceSyncService | `BalanceSyncService.kt` (mock; paisa `Long`; Hilt-injectable) |
| Wire Accounts → Consent | `PiPlannerNavHost` routes; placeholder removed |

## Acceptance criteria

- [x] Consent Yes → balance ₹1,00,000 shown (3a)
- [x] Consent No → Update balance sheet (4)
- [x] Manual path: amount ₹0 → Continue disabled
- [x] UPI PIN Demo: PIN `"1234"` → balance fetch success
- [x] Wrong PIN → error with retry/manual options (4d, 4e)
- [x] “Account on another UPI app” (4c) → force manual entry
- [x] States: Yes path, No path, Manual, PIN success, PIN fail, Other app
- [x] Unit tests for mock BalanceSyncService (+ ConsentViewModel transitions)

## Test result

`./gradlew assembleDebug test` — see PR body.

## Assumptions

- Goal chat (frame 5) is not built yet; after a resolved balance the flow hands off to existing Opening split with `DemoData.sampleOpeningSplitGoals()` and the resolved paisa amount.
- Money remains **Long paisa** (Spec Decisions Log); mock Yes/PIN success returns `10_000_000L` paisa.
- Design artifact copy was inferred from PRD R3/R4 + iOS PIP-39 when the Claude artifact page did not expose frame text.
- Copy and behaviour aligned with merged iOS PIP-39.

## Out of scope

Real UPI / bank integration; Goal chat / form changes; Opening split logic beyond receiving a resolved balance.
