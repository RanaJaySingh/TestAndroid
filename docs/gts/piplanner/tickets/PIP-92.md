# PIP-92 — Android History tab list visual

**Status:** Implemented on `PIP-92-history-list-visual` (from `1fcbdff` / PIP-74)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-92/android-history-tab-list-visual  
**PRD:** R15 · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §5.2 J6 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** frames 12, 12a · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  
**iOS twin:** PIP-91  

## Acceptance criteria

- [x] Given History tab, When entries list, Then newest-first rows show type labels, amounts, and lock icons on saved entries consistent with design
- [x] States handled: empty list; open vs locked row chrome
- [x] Tests: Reviewer screenshot; History tests green

## Landed

| Type / file | Role |
|-------------|------|
| `ui/history/HistoryEntryRow.kt` | `PiCard` rows; light-blue type icon wells; `PiIcons.lock` beside type label on saved; open vs locked chrome (navy accent + Assign-now chip vs quieter locked row); navy amount on open |
| `ui/history/HistoryTab.kt` | `BackgroundApp`; tokenised empty well (`PiIcons.historyTab`); card list without separators |
| `HistoryListVisualTest` | Token / open-vs-locked chrome / icon catalog smoke |
| `docs/gts/piplanner/tickets/PIP-92.md` | This ticket note |

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| PRD R15 History tab list | Rows: type label + amount + lock on saved |
| Spec §3.5 HistoryRow | Same visual contract via `HistoryEntryRow` |
| Spec §5.2 J6 | Frames 12 / 12a list chrome |
| Spec §3.3 Lock | `PiIcons.lock` (Material filled lock) |
| Open vs locked chrome | Open: navy leading accent + light-blue “Assign now” chip + navy amount; Locked: lock icon + quieter well |
| Empty list | Tokenised empty state with history icon in light-blue well |
| PIP-68 / PIP-70 / PIP-72 | Tokens, `PiCard` (`contentPadding` from PIP-74), `PiIcons` — no new hex / MainTab chrome |

## Reviewer checklist (History list)

Compare device/emulator to design artifact frames 12 / 12a:

1. List sits on light app background (`#F5F7FB`), white card rows (radius 22, soft shadow).
2. Each row: type icon in light-blue circle, type label (semibold), trailing ₹ amount (Indian grouping unchanged).
3. Saved / locked rows show lock next to the type label; no navy leading accent bar.
4. Open New credit row: navy leading accent + “Assign now” light-blue chip; amount in navy; **no** lock icon.
5. Empty History: clock/history metaphor (`PiIcons.historyTab`) in light-blue well + empty copy.
6. Tapping open credit still opens editable entry; tapping locked still opens read-only detail (behaviour unchanged).

## Out of scope

History ordering / lock / destination logic (PIP-60 Done); History entry detail open vs locked visual (PIP-86); MainTab chrome; ViewModel / product behaviour; iOS twin (PIP-91).

## Base

`1fcbdff` (PIP-74 Welcome on main after PIP-72). Touches **only** History list UI + ticket doc + visual smoke test — prefer main for shared `PiCard` / tokens / icons.

## Verify

`./gradlew assembleDebug test`
