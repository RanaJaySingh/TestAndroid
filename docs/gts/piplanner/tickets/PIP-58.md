# PIP-58 — Android Withdrawal flow

**Status:** Implemented on `PIP-58-withdrawal` (rebased onto `756cfab` / PIP-54)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-58/android-implement-withdrawal-flow  
**PRD:** R15 — Withdrawal proportional; no goal below ₹0 · https://docs.google.com/document/d/16cthPNS-djP0KLlH9Lak3OTDrottiD5bMnTJwW_cRDc/edit  
**Spec:** Section 3.2 BR-8, Section 5 · https://docs.google.com/document/d/1xg3FrN6802Ya8hEoiwiuE-Id3m-fPCHN929bVr0FW_k/edit  
**Design:** Frames 18, 18a, 18b, 18c · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  

## Acceptance criteria

- [x] Given lower balance detected (10b / 18c), when Withdrawal (18) opened, then proportional allocation is default
- [x] Given reductions, when edited once, then user can modify before Save
- [x] Given total reductions, when they ≠ shortfall (18a), then Save disabled with running total shown
- [x] Given any goal, when reduction would make it < ₹0, then validation prevents it
- [x] Given valid withdrawal, when Save and lock tapped, then History entry (18b) created
- [x] Given Record a withdrawal (18c), when initiated manually, then same flow opens
- [x] States handled: Proportional default, Edit, Invalid total, Goal below zero, Complete
- [x] Tests: Unit test for proportional calculation and validation

## Landed

- `WithdrawalService`: BR-8 proportional defaults (largest-remainder / paisa), sum==shortfall + no-goal-below-zero validation, locked `HistoryEntryType.Withdrawal`, account + goal saved-amount updates; standing split unchanged.
- `WithdrawalViewModel` + `WithdrawalScreen`: proportional default, Edit→Done once, running total vs shortfall, Save and lock.
- Entry points: Sync/Update lower balance → Continue to withdrawal; Goals tab **Record a withdrawal** (18c) sheet → same route.
- Nav: replaces PIP-48 `WITHDRAWAL_STUB` with `withdrawal/{previousBalance}/{newBalance}/{isTyped}`.
- Unit tests: `WithdrawalServiceTest`, `WithdrawalViewModelTest`.

## Out of scope

- Transfer (PIP-56 parallel).
- History tab UI (list still later).

## Verify

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL** (on tip rebased onto `756cfab`)
