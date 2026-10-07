# PIP-56 — Android Transfer between goals

**Status:** Implemented — rebased onto `756cfab` (PIP-54)  

**Linear:** https://linear.app/telco-paytm/issue/PIP-56/android-implement-transfer-between-goals  
**PRD:** R14 — Transfer between goals · https://docs.google.com/document/d/16cthPNS-djP0KLlH9Lak3OTDrottiD5bMnTJwW_cRDc/edit  
**Spec:** Section 3.2 BR-7, Section 5 · https://docs.google.com/document/d/1xg3FrN6802Ya8hEoiwiuE-Id3m-fPCHN929bVr0FW_k/edit  
**Design:** Frames 16, 16a, 16b, 16c · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  

## Acceptance criteria

- [x] Given Transfer (16), when opened, then From/To selectors and amount field shown
- [x] Given amount, when chips (₹1,000 / ₹5,000 / ₹10,000) tapped, then amount auto-filled
- [x] Given After transfer preview, when shown, then new balances displayed
- [x] Given amount > From saved (16b), when entered, then Move button disabled
- [x] Given valid transfer, when Move tapped, then money moves and History entry (16a) created
- [x] Given standing split, when transfer completes, then it remains unchanged
- [x] Given Ask proposal (16c), when received, then Transfer opens pre-filled
- [x] States handled: Select, Enter amount, Preview, Over-amount, Complete
- [x] Tests: Unit test for validation and History entry

## Landed

- `TransferService`: validate From≠To, amount>0, amount≤From.saved; apply debit/credit; append locked `HistoryEntryType.Transfer`; leave `standingSplits` / `shareOfNewCredits` untouched (BR-7).
- `TransferViewModel` + `TransferScreen`: From/To selectors, amount field, chips ₹1k/₹5k/₹10k, after-transfer preview, Move gated on Preview.
- Navigation: `PiPlannerRoutes.TRANSFER` with optional `fromGoalId` / `toGoalId` / `amountPaisa` (Ask 16c pre-fill). Entry from Goal detail and Goals tab.
- Unit tests: `TransferServiceTest`, `TransferViewModelTest`.

## Out of scope

- Standing split changes (explicitly unchanged by transfer).
- Ask tab UI that emits proposals (nav args ready for when Ask lands).

## Branch

`PIP-56-transfer` based on `756cfab` (main after PIP-54 Delete goal).
