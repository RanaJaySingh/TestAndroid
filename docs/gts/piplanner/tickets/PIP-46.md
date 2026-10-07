# PIP-46 — Android Goals tab with balance card and goal cards

Ticket: https://linear.app/telco-paytm/issue/PIP-46/android-implement-goals-tab-with-balance-card-and-goal-cards

## Delivered

Goals tab layout under `ui/goals` with `GoalsViewModel` + `GoalsTabService`, balance card (INR total + Sync/Update CTA by consent), goal cards (saved amount + On track/Behind), bottom nav shell (Goals | History | Ask), gear → Settings stub, goal-card → PIP-50 `GoalDetailScreen` (`goal_detail/{goalId}`), Sync/Update sheet hooks (PIP-48 owns full credit entry), and unit tests.

Rebased onto `dac8439` (PIP-50 merged); detail stub removed in favor of real PIP-50 screens/routes.

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| R21 dedicated savings total | `GoalsTabService.totalSavingsPaisa` prefers dedicated account balance |
| Frames 9 / 9b / 9c / 11 | `GoalsTab` + `BalanceCard` + `GoalCard` |
| Consent On → Sync | `GoalsBalanceAction.Sync` → `GoalsSyncSheet` → `BalanceSyncService` |
| Consent Off → Update balance | `GoalsBalanceAction.UpdateBalance` → `GoalsUpdateBalanceSheet` |
| Gear → Settings (A4) | `SettingsScreen` stub via `PiPlannerRoutes.SETTINGS` |
| Goal card → detail | PIP-50 `GoalDetailScreen` / edit / transfer / delete routes |
| Bottom nav Goals/History/Ask | `MainTabsScreen` + History/Ask placeholders |
| Spec §5.3 INR | `FormattingService.formatInrFromPaisa` |

## Acceptance criteria

- [x] Balance card shows total savings with INR formatting
- [x] Each goal shows as a card with saved amount and status
- [x] Goal card tap → Goal detail navigation (PIP-50 `GoalDetailScreen`)
- [x] Consent On → Sync button triggers balance sync flow hooks
- [x] Consent Off → Update balance triggers manual update flow hooks
- [x] Gear → Settings
- [x] States: Post-setup, With goals, Consent On/Off
- [x] Unit tests for tab layout helpers + ViewModel navigation/hooks

## Test result

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL** (after rebase onto `dac8439`).

## Assumptions

- Design artifact page did not expose frame copy; UI mirrors iOS PIP-45 + PRD Goals tab bullets.
- After PIP-50 merge: Goals tab navigates to real detail/edit APIs; temporary `GoalDetailStubScreen` removed.
- Sync/Update sheets only hook mock balance updates; credit assignment / open-entry is PIP-48.
- Header chrome (Search, notifications, bar_chart) omitted per R27.

## Out of scope

Header chrome (R27); Goal detail/edit ownership (PIP-50, consumed from main); full Sync/credit sheets (PIP-48).
