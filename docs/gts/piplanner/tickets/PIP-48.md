# PIP-48 — Android Sync/Update balance and credit entry flow

Ticket: https://linear.app/telco-paytm/issue/PIP-48/android-implement-syncupdate-balance-and-credit-entry-flow

## Status

Done on branch `cursor/pip-48-sync-credit-entry-d07b`, rebased onto `7830826` (PIP-46 on main). Draft PR #11.

## Delivered

- `CreditEntryService` — balance compare (higher / same / lower), open New credit entry, BR-6 block, save-and-lock once (BR-5), optional standing-split update
- `StandingSplitService.percentagesForNextCredit` — next-credit defaults (single goal → 100%)
- `@PostSetupBalanceSync` mock returns ₹1,10,000 so first Goals Sync creates a ₹10,000 credit
- Goals Sync / Update sheets with result states (Continue to credit / No new credit / Withdrawal stub)
- `CreditEntryScreen` + ViewModel (edit %, Save and lock, Use this split checkbox)
- `OpenEntryBanner` (“Assign now”) on Goals; Sync/Update blocked while open
- Withdrawal stub route (full UI out of scope)

## Acceptance criteria

- [x] Sync higher balance → open History credit entry (13)
- [x] Same balance (10a) → "No new credit", no entry (R26)
- [x] Lower balance (10b) → Withdrawal flow hook/stub
- [x] Open entry: edit % + Save → locked (13a/13d); "Use this split" updates standing split
- [x] Open entry pending → Goals banner "Assign now" (9b); Sync/Update blocked while banner visible
- [x] One goal (13e) → 100% auto-assigned
- [x] States: Sync new, Same, Lower, Open, Locked, Banner
- [x] Unit tests for split validation and entry state machine (`CreditEntryServiceTest`, Goals VM cases)

## Scope

In: SyncSheet / GoalsSyncSheet, GoalsUpdateBalanceSheet, CreditEntryScreen, open entry banner, domain services  
Out: Full withdrawal UI (stub only)

## Test result

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL**
