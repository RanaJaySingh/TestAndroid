# PIP-48 — Android Sync/Update balance and credit entry flow

Ticket: https://linear.app/telco-paytm/issue/PIP-48/android-implement-syncupdate-balance-and-credit-entry-flow

## Status

In progress — scaffold on branch `cursor/pip-48-sync-credit-entry-d07b` from `dac8439` (main with PIP-42 / PIP-50).

## Acceptance criteria

- [ ] Sync higher balance → open History credit entry (13)
- [ ] Same balance (10a) → "No new credit", no entry (R26)
- [ ] Lower balance (10b) → Withdrawal flow hook/stub
- [ ] Open entry: edit % + Save → locked (13a/13d); "Use this split" updates standing split
- [ ] Open entry pending → Goals banner "Assign now" (9b); Sync/Update blocked while banner visible
- [ ] One goal (13e) → 100% auto-assigned
- [ ] States: Sync new, Same, Lower, Open, Locked, Banner
- [ ] Unit tests for split validation and entry state machine

## Scope

In: SyncSheet, UpdateBalanceSheet (Goals), CreditEntryScreen, open entry banner  
Out: Full withdrawal; Goals tab cards (PIP-46); standing-split screen (PIP-52)
