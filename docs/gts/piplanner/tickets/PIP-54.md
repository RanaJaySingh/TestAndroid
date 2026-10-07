# PIP-54 — Android Delete goal flow

Ticket: https://linear.app/telco-paytm/issue/PIP-54/android-implement-delete-goal-flow

## Delivered

Delete goal with money reassignment and standing-split renormalization (PRD R13, Spec BR-9 / §5):

- `StandingSplitService` — shared merge-friendly API (`equalSplits`, `renormalizeAfterRemoving`, `resetToEqual`, `upsert`, `applySharesToGoals`) used by delete and by `GoalHeldChangeService` (PIP-50); ready for PIP-52 Standing split screen
- `DeleteGoalService` — equal default reassignment, paisa allocations via largest-remainder, locked `HistoryEntryType.GoalDeleted` (“deleted / moved”), apply delete + standing renormalize; clears `heldGoalChanges` for the deleted goal; only-goal gate requires replacement; mid-delete create can reset standing split to equal (17d)
- `DeleteGoalViewModel` + `DeleteGoalScreen` — states: Reassign default, Reassign edit, Confirm, Only-goal gate
- Wired from Goal detail (PIP-50) Delete → `delete_goal/{goalId}` (replaces PIP-50 stub)
- Unit tests: reassignment + renormalization (`DeleteGoalServiceTest`, `StandingSplitServiceTest`) and ViewModel gate/confirm (`DeleteGoalViewModelTest`)

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| R13 / BR-9 equal default | `DeleteGoalService.equalReassignmentDisplayPercents` |
| Editable once before confirm | `DeleteGoalPhase.ReassignEdit` via `setDisplayPercent` |
| History deleted / moved | Locked `GoalDeleted` with `deletedGoalName`, `releasedAmount`, allocations |
| Standing split renormalizes | `StandingSplitService.renormalizeAfterRemoving` in `applyDelete` |
| Only-goal gate (17e) | Confirm disabled until replacement created |
| Mid-delete create (17d) | Replacement form; `resetStandingSplitToEqual = true` |
| Entry from Goal detail | `GoalDetailScreen.onDelete` → `PiPlannerRoutes.deleteGoal` |

## Acceptance criteria

- [x] Delete (17) → reassignment UI with equal default distribution
- [x] Reassignment editable once; user can modify before confirm
- [x] Confirm → money moves + History “deleted / moved” entry
- [x] Standing split renormalizes across remaining goals
- [x] Only-goal (17e) → confirm disabled until replacement created
- [x] Create mid-delete (17d) → standing split may reset to equal
- [x] States: Reassign default, Reassign edit, Confirm, Only-goal gate
- [x] Unit tests for money reassignment and standing-split renormalization

## Base / rebase

Rebased onto `main@dac8439` (PIP-50 Goal detail merged). Soft Goals-tab Delete hook removed; entry is Goal detail Delete only.

## Test result

`./gradlew assembleDebug test` — pending / recorded after CI run in this agent turn.

## Assumptions

- Design artifact frames 17a–17e copy inferred from PRD R13 + Spec BR-9; UI follows Opening split patterns (percent fields, confirm dialog).
- Money remains **Long paisa**; percentages use `BigDecimal` fractions summing to 1.0.
- `StandingSplitService` owns read/save/renormalize helpers so PIP-52 should extend it rather than fork persistence.
