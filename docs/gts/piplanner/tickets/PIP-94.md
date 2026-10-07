# PIP-94 [Android] Settings visual parity

Ticket: https://linear.app/telco-paytm/issue/PIP-94/android-settings-visual-parity

## Delivered

Restyled Settings (gear from Goals — frames 20 / 20a–20c) under `ui/settings/SettingsScreen.kt` to design grouping using PIP-68 tokens and PIP-70 components. Visual / layout only — consent/reset ViewModel behaviour unchanged. Settings remains gear entry only (not a fourth tab).

| Surface | Visual treatment |
|---------|------------------|
| Screen chrome | `PiPlannerColors.BackgroundApp`; Done + title bar |
| Linked accounts | Uppercase section header + `PiCard` rows (navy role caption, title, ₹, Paytm link line) |
| Automatic balance updates | `PiCard` + navy-tinted `Switch`; On/Off chip (light-blue when On); On/Off subtitle + Off untyped-gap hint |
| Reset demo | Destructive label in `PiCard` under Demo group |
| Reset confirm | `PiSheet` chrome + destructive confirm + `SecondaryCta` Cancel (replaces system `AlertDialog` chrome only) |

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| PRD R16 Settings visual parity | Grouped Linked accounts · Automatic balance updates · Reset demo |
| Tech Spec §5.2 J7 | Frames 20 / 20a–20c visual must-match |
| PRD R5 / A6 — not a fourth tab | Gear entry unchanged; MainTabs untouched |
| Design tokens / components | `PiPlannerColors`, `PiPlannerDimens`, `PiCard`, `PiSheet`, `SecondaryCta` |
| Consent On (20) / Off (20a) | Switch tint + On/Off chip + design-aligned subtitle copy |
| Untyped gap (20c) | Hint shown while Off |
| Reset confirm chrome | `PiSheet`; same `showResetConfirmation` / `confirmResetDemo` wiring |

## Acceptance criteria

- [x] Given Settings via Goals gear, When opened, Then Linked accounts, Automatic balance updates copy/toggle, and Reset demo match design grouping (not a fourth tab)
- [x] States handled: consent toggle On/Off visual; Reset confirm chrome
- [x] Tests: Settings tests green; `./gradlew assembleDebug test` green
- [x] Out: Consent/reset behaviour; inventing a Settings tab

## Reviewer checklist (Settings)

Compare device/emulator to design artifact frames 20 / 20a–20c:

1. Entry is Goals gear only — Settings is not a tab.
2. App background is light (`#F5F7FB`); sections are white cards radius 22 with soft shadow.
3. Linked accounts: dedicated first, role / bank / ₹ / “Linked in Paytm” hierarchy.
4. Automatic balance updates: navy switch; **On** chip + Sync subtitle; **Off** chip + Update-balance subtitle + untyped-gap hint.
5. Reset demo is destructive-styled; confirm uses Paytm-like sheet (title, helper, Reset demo / Cancel) — not a plain system alert.
6. Done dismisses back to Goals.

## Parallel work / base

Started from `33b810ba51238f5ba453a692a69141b8f8307d4a` (PIP-70 on main). Touch Settings screen (+ ViewModel display fields for role/link labels, strings, this ticket doc) so parallel visual tickets rebase cleanly.

## Test results

`./gradlew assembleDebug test` — see CI / local run on this branch.

## Out of scope

ConsentSheet / Reset demo product behaviour (PIP-62 Done); inventing Settings tab or Standing-split entry in Settings (Android keeps standing split elsewhere); iOS twin (PIP-93); inventing extra chrome (R18).

## References

- PRD R16: https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit
- Tech Spec §5.2 J7: https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit
- Design frames 20, 20a–20c: https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5
- Design note: https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit
- Tracker: https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit
- Drive: https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT
- Blocked by: PIP-70 (shared components)
- Related: PIP-93 (iOS twin)
