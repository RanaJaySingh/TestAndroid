# PIP-84 — Android Sync / Update sheets visual parity

**Status:** Implemented on `PIP-84-sync-update-sheets-visual` (from `33b810b`)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-84/android-sync-update-sheets-visual-parity  
**PRD:** R11 · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §5.2 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** frames 10, 10a, 10b, 11a · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  
**iOS twin:** PIP-83  

## Acceptance criteria

- [x] Given Sync sheet, When open, Then Previous / Fetched / New amount (+green) layout matches design
- [x] Given same-balance or went-down messaging, When shown, Then hierarchy matches design
- [x] Given Update balance from Goals, When open, Then Paytm-like choice/amount chrome matches design
- [x] States handled: credit up / same / down messaging visuals
- [x] Tests: Reviewer screenshots; sync/update behaviour tests green

## Landed

- `GoalsSyncSheet` — PiSheet “Balance sync”; PiCard Previous / Fetched / **New amount +₹…** (`PositiveGreen`) or Went down by −₹… (`Destructive`); PrimaryCta / SecondaryCta
- `GoalsUpdateBalanceSheet` — frame 11a choice (Current card · Manually primary · Balance sync outline · Record a withdrawal text) → Manual amount PiCard chrome; result Continue CTAs unchanged
- `GoalsViewModel.performUpdateBalanceSync` — thin wiring of existing fetch → `processBalanceOutcome(forUpdateSheet)` for 11a Balance sync choice
- `formattedWentDownBy` on `GoalsUiState` for frame 10b amount row (presentation only)
- Unit smoke: `SyncUpdateSheetsVisualTest`; existing Goals sync/update VM tests green

## Reviewer checklist

Compare device/emulator to design frames 10 / 10a / 10b / 11a:

1. Sync sheet uses PiSheet handle + title **Balance sync**
2. Balance rows sit in a white `PiCard` (radius 22)
3. New amount shows **+₹…** in positive green `#1B8A4A`
4. Same / down messaging is body secondary; went-down amount row uses destructive
5. Update 11a: Manually = filled navy PrimaryCta; Balance sync = outline SecondaryCta
6. Semantics preserved: `Goals sync sheet`, `Sync now`, `Continue to credit entry`, `Goals update balance sheet`, `Manually`, `Balance sync`, `New balance digits`, `Apply balance`

## Out of scope

- Sync/credit entry logic (Done in PIP-48)
- UPI PIN pad chrome (11c — setup / later)
- ViewModel product rules beyond visual wiring of existing fetch/outcome paths
- Goals home navy card / other screens (parallel tickets)

## Verify

`./gradlew assembleDebug test`
