# PIP-50 — Android Goal detail and edit screens

Ticket: https://linear.app/telco-paytm/issue/PIP-50/android-implement-goal-detail-and-edit-screens

## Delivered

- `GoalDetailScreen` + `GoalDetailViewModel` (frame 14): saved amount, status, adjusted target, monthly need, dates, inflation, share, From History list, Transfer / Edit / Delete actions.
- `GoalEditScreen` + `GoalEditViewModel` (frame 6e): editable fields with **saved amount locked** read-only; save persists held change.
- `GoalHeldChangeService` + `HeldGoalChange` on `AppState` (BR-4 / R11 / R24): updates goal params + standing share without rewriting History or saved amount; toast 9c; edit-held info 13g on later detail view; `clearHeldChanges` hook for PIP-48.
- Nav routes for detail / edit / transfer stub / delete stub; Goals tab placeholder only lists goal names to open detail (PIP-46 owns full tab UI).
- Unit tests: `GoalHeldChangeServiceTest` (+ `AppState.heldGoalChanges` serialization).

## Acceptance criteria

- [x] Goal detail (14): saved amount, status, adjusted target, monthly need, dates, inflation, share visible
- [x] From History section lists related history entries
- [x] Transfer / Edit / Delete navigate (delete/transfer stubbed)
- [x] Goal edit (6e): saved amount field locked read-only
- [x] On save: toast "Change saved. Applies at next credit." (9c)
- [x] Later detail view shows edit-held info (13g)
- [x] States: Detail view, Edit mode, Post-edit toast
- [x] Unit tests for held-changes logic

## How to test

1. Complete setup through Opening split lock (or use persisted `hasCompletedSetup` state with goals).
2. On Goals placeholder, open a goal name → detail metrics + From History.
3. Edit → change target/share → Save changes → toast → back on detail with held-edit banner.
4. Confirm History amounts unchanged; saved amount unchanged on edit.
5. `./gradlew assembleDebug test`

## Test result

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL**.

## Out of scope

Full delete reassignment; real money movement / credit apply of held changes (PIP-48); Goals tab cards (PIP-46).
