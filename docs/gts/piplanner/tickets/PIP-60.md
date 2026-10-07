# PIP-60 — Android History tab

Ticket: https://linear.app/telco-paytm/issue/PIP-60/android-implement-history-tab

## Delivered

History tab listing all History entries with open vs locked navigation (PRD R16 / R23, Spec §3.1 / BR-3, design frames 12 / 12a):

- `HistoryService` — newest-first sort, type labels/icons (Opening balance, New credit, Transfer, Withdrawal, **Goal deleted**), Assign now subtitle, navigation targets, `ORIGINAL_AMOUNTS_CAPTION` ("Original amounts never change")
- `HistoryViewModel` + `HistoryTab` + `HistoryEntryRow` — empty / with-entries states; lock glyph on saved + Opening balance; open New credit shows Assign now
- Navigation from History:
  - Open New credit → existing `credit_entry/{entryId}` (editable)
  - **Locked New credit → `history_detail/{entryId}`** read-only with “Original amounts never change” (not CreditEntry)
  - Opening balance → `history_opening/{entryId}` → Opening split read-only (`HistoryOpeningBalanceScreen`) with frame 12a caption
  - Transfer / Withdrawal / Goal deleted → `history_detail/{entryId}` → `HistoryEntryDetailScreen` read-only + original-amounts caption
- Bottom nav: `MainTabsScreen` History stub replaced with real `HistoryTab`
- Unit tests: `HistoryServiceTest`, `HistoryViewModelTest` (list + open/locked nav), `HistoryDetailViewModelTest` (read-only caption)

## GTS Review round 1 fixes

1. Locked New credit → `LockedDetail` / `history_detail` with `ORIGINAL_AMOUNTS_CAPTION` (not CreditEntry “Locked amounts never change”)
2. Goal deleted History list label → exactly **“Goal deleted”** (`HistoryService.TYPE_GOAL_DELETED`); `DeleteGoalService.historyTitle` “Deleted / moved” unchanged for writer

## Mapping to Spec / PRD / Design

| Requirement | Implementation |
|-------------|----------------|
| R16 / R23 / BR-3 locked History | Lock icon + read-only detail paths; Opening always read-only |
| Newest first (12) | `HistoryService.entriesNewestFirst` |
| Type icons/labels | Five AC labels including **Goal deleted** |
| Assign now → editable credit | Open New credit only → `CreditEntry` |
| Locked New credit / Opening 12a | `ORIGINAL_AMOUNTS_CAPTION` on History detail / Opening read-only |
| Empty / with entries / open vs locked | `HistoryUiState.isEmpty`, row flags, nav targets |

## Acceptance criteria

- [x] History tab (12) shows entries newest first
- [x] Icons/labels distinguish: Opening balance, New credit, Transfer, Withdrawal, Goal deleted
- [x] Saved entries show lock icons
- [x] Open "Assign now" entry → navigates to editable credit entry
- [x] Saved/locked entry (12a) → read-only view with "Original amounts never change"
- [x] Opening balance entry always read-only
- [x] States: Empty, With entries, Open vs Locked
- [x] Tests: unit tests for list and read-only state

## Base

Rebased onto `main@5df557e` (PIP-62 Settings; prior `e95618b` / PIP-56). Branch: `PIP-60-history`. PR #14. Keep-both: History + Withdrawal + Transfer + Settings wiring.

## Test result

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL** (incl. round-1 must-fixes).

## Assumptions

- Transfer / Withdrawal create flows may land on parallel branches; History still lists and opens read-only detail for those types.
- Setup Opening split lock caption remains "Locked amounts never change"; History read-only uses "Original amounts never change" (design 12a).
- Delete goal writer History title may remain “Deleted / moved”; list chrome uses “Goal deleted” (PIP-59 parity).
