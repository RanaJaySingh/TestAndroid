# PIP-72 — Android Material-style icon mapping + tab bar chrome

**Status:** Implemented on `PIP-72-icon-mapping-tab-bar` (from `77d4752`)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-72/android-material-style-icon-mapping-tab-bar-chrome  
**PRD:** R4, R5 · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §3.3 Icon mapping, §3.4 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  

## Acceptance criteria

- [x] Given post-setup shell, When NavigationBar shows, Then exactly three tabs Goals · History · Ask with Material icons (not `◎`/`◷`/`?`) and selected-state styling per design
- [x] Given locks/sync/transfer/settings metaphors, When icons render, Then Material icons match Spec §3.3
- [x] States handled: selected vs unselected tab
- [x] Tests: Compose UI check on MainTabsScreen; existing tests green

## Landed

| Type / file | Role |
|-------------|------|
| `ui/theme/PiIcons.kt` | Spec §3.3 Material catalog + `MainTabChrome` (3 tabs, selected/unselected, navy tint) |
| `ui/navigation/MainTabsScreen.kt` | `MainTabsBottomBar` — Material Icons, navy selected tint, light-blue indicator |
| `GoalsTab` / History views / `HistoryService` | Consume catalog for settings / lock / history type icons (no behaviour change) |
| `PiIconsTest` + `MainTabsScreenTest` | Catalog contract + Compose UI check |

## Icon map (Spec §3.3 → Material)

| Metaphor | Material (unselected / selected) |
|----------|----------------------------------|
| Goals tab | `Icons.Outlined.Flag` / `Icons.Filled.Flag` |
| History tab | `Icons.Outlined.History` / `Icons.Filled.History` |
| Ask tab | `Icons.AutoMirrored.Outlined.Chat` / `Filled.Chat` |
| Settings gear | `Icons.Outlined.Settings` |
| Lock (saved) | `Icons.Filled.Lock` |
| Sync | `Icons.Outlined.Sync` |
| Transfer | `Icons.Outlined.SwapHoriz` |
| Withdrawal / down | `Icons.Outlined.ArrowDownward` |
| New credit / add | `Icons.Outlined.AddCircle` |
| Header Search / notifications / chart | Search / Notifications / BarChart (catalog only; chrome later) |

## Tab chrome

- Exactly three tabs: **Goals · History · Ask** (no Settings tab).
- Selected: filled Material weight + navy tint `#0A2A6B` + light-blue indicator (`PiPlannerColors` from PIP-68).
- Unselected: outline-leaning catalog icons; muted on-surface.

## Reviewer checklist (MainTabsScreen)

1. Tab order left→right: Goals, History, Ask only (no Settings).
2. Selected tab label/icon uses navy (not forest green, not purple).
3. Selected icon weight is filled; unselected is outline.
4. Gear on Goals still opens Settings (existing behaviour); gear uses `PiIcons.settings`.
5. No new navigation from catalogued header Search / notifications / chart (not wired this ticket).

## Out of scope

New destinations; behaviour changes; header chrome behaviour (later tickets); shared visual components (PIP-70).

## Verify

`./gradlew assembleDebug test`
