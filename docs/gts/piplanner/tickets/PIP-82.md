# PIP-82 — Android Goals home navy card, quick actions, banner, header chrome

**Status:** Implemented on `cursor/pip-82-goals-home-visual-590e` (from `1f3bb8f`)  
**PR:** https://github.com/RanaJaySingh/TestAndroid/pull/33 (ready, not merged)  
**Head SHA:** `d7ba46fb8616f712e500b56b3f72478f2268013a` · **Base tip:** `1f3bb8fa926265df96f4324af374522d1b2da7db`  
**Linear:** https://linear.app/telco-paytm/issue/PIP-82/android-goals-home-navy-card-quick-actions-banner-header-chrome  
**PRD:** R10, R20 · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §5.2 J2 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** frames 9, 9b, 9c, 11 · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  
**iOS twin:** PIP-81  

## Acceptance criteria

- [x] Given Goals tab after setup, When home renders, Then navy balance card with last-synced/updated line; quick actions Sync or Update, New goal, Transfer, History; goal cards show saved/of target, status, monthly need, % of credits per design
- [x] Given open credit, When banner shows, Then Assign now treatment matches 9b (light-blue + navy CTA)
- [x] Given header Search/notifications/bar_chart, When shown, Then chrome-only (no new flows) per A2/R20
- [x] States handled: consent On (Sync) vs Off (Update); with/without open entry
- [x] Tests: Goals behaviour + presentation helpers green; `./gradlew assembleDebug test`

## Landed

| Type / file | Role |
|-------------|------|
| `ui/goals/BalanceCard.kt` | Navy gradient card; white amount; last-synced / last-updated line; Sync / Update balance CTA |
| `ui/goals/QuickActionRow.kt` | Sync/Update · New goal · Transfer · History chips (`PiIcons`) |
| `ui/goals/GoalCard.kt` | `PiCard` surface; saved/of target; status chip; monthly need; % of credits |
| `ui/goals/OpenEntryBanner.kt` | Frame 9b Assign now (light-blue surface + navy CTA) |
| `ui/goals/GoalsTab.kt` | Layout wiring + header Search / notifications / chart chrome |
| `ui/navigation/MainTabsScreen.kt` | History quick action → History tab; New goal → Ask tab |
| `domain/GoalsTabService.kt` | Presentation helpers (JVM-testable labels only) |
| `GoalsTabServiceTest` + `GoalsHomeVisualTest` | Label / token contract cases |

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| R10 navy balance card + last-synced/updated | `BalanceCard` + `GoalsTabService.lastBalanceActivityLine` |
| R10 quick actions Sync/Update, New goal, Transfer, History | `QuickActionRow` → existing Sync/Update sheets, Ask tab, Transfer route, History tab |
| R10 goal cards saved/of target, status, monthly need, % credits | `GoalCard` + `GoalsTabService` label helpers |
| R10 / 9b open-credit banner | `OpenEntryBanner` Assign now treatment |
| R20 / A2 header Search / notifications / bar_chart | `PiIcons.header*` — chrome only, no flows |
| Consent On vs Off | Sync vs Update (card CTA “Update balance”; quick action “Update”) |

## Reviewer checklist (Goals home)

Compare device/emulator to design frames 9 / 9b / 9c / 11:

1. Balance card is navy (not gray/surfaceVariant); amount in white; last-synced (consent On) or last-updated (consent Off) caption present.
2. Quick-action row: Sync/Update, New goal, Transfer, History with Material icons from `PiIcons`.
3. Goal cards white radius ~22; show “₹ saved of ₹ target”, On track/Behind chip (token green/amber), monthly need, “N% of credits”.
4. Open credit → light-blue banner + Assign now (still opens existing credit entry).
5. Header shows Search, notifications, chart (non-interactive chrome) + Settings gear (existing).
6. No new navigation from header chrome icons.

## Out of scope

- Sync/credit logic changes; new header behaviours/flows
- ViewModel product behaviour beyond presentation label helpers
- iOS twin (PIP-81); Goal detail / Sync sheet visual tickets

## Verify

```bash
./gradlew assembleDebug test
```
