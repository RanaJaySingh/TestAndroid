# PIP-88 — Android Goal detail visual parity

**Status:** Implemented on `PIP-88-goal-detail-visual` (rebased onto `1fcbdff` / PIP-74)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-88/android-goal-detail-visual-parity  
**PRD:** R13 · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §5.2 / §4.2 J3 V11 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** frame 14 · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  
**iOS twin:** [PIP-87](https://linear.app/telco-paytm/issue/PIP-87/ios-goal-detail-visual-parity) (PASS)

## Acceptance criteria

- [x] Given Goal detail, When opened, Then large saved amount, Behind/On track, adjusted target, % reached, monthly need, dates, inflation, share line, From History list, and Transfer/Edit/Delete actions match design layout
- [x] States handled: On track vs Behind status chrome
- [x] Tests: Reviewer screenshot (Compose previews On track / Behind); goal detail behaviour tests green

## Landed

- `GoalDetailScreen` restyle (frame 14 / R13) mirroring iOS PIP-87 hierarchy:
  - App background `BackgroundApp`
  - Held-edit banner (13g) on light-blue chip fill
  - Hero `PiCard`: caption “Saved” + `amountHero` navy + On track (green) / Behind (amber) status chip
  - Metrics `PiCard`: Target, Adjusted target, **% reached** (view-local `saved ÷ adjusted`), Monthly need, Start/End (month-year), Inflation, Share of new credits
- Outline action row: Transfer / Edit / Delete (destructive) with PIP-72 `PiIcons` (transfer / edit / delete)
- From History: titled section + `PiCard` rows with `PiIcons.lock` on locked entries
- `GoalDetailViewModel`: presentation wiring only — `formattedTarget`, `startDateLabel` / `endDateLabel` (MMM yyyy). No CRUD / transfer / delete / held-edit logic changes.
- Compose previews: On track / Behind
- Unit smoke: `GoalDetailVisualParityTest`

## Out of scope

- CRUD / transfer / delete / standing-split behaviour (Done in PIP-50..56)
- Goals home / GoalCard restyle (later tickets)
- Sheet chrome for Transfer/Delete destinations (PIP-90)

## Verify

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL**.
