# PIP-90 — Android Standing split, Transfer, Withdrawal, Delete sheets visual

**Status:** Implemented on `PIP-90-sheets-visual` (rebased onto `1fcbdff` / PIP-74; prefers main for PIP-68/70/72 shared tokens/icons/chrome)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-90/android-standing-split-transfer-withdrawal-delete-sheets-visual  
**PRD:** R14 (+ Delete frame 17) · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §5.2 J3–J5 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** frames 15, 16, 16b, 17, 18, 18a · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  

## Acceptance criteria

- [x] Given Standing split, When shown, Then % fields and Save at 100% chrome match design
- [x] Given Transfer, When shown, Then From/To, chips ₹1,000/₹5,000/₹10,000 (light-blue), After transfer preview match design
- [x] Given Withdrawal, When shown, Then goal reductions and negative amounts match design; Save and lock CTA styling matches
- [x] Given Delete goal, When shown, Then release amount, destination split, confirm destructive layout match design
- [x] States handled: invalid totals (CTA disabled look); valid totals
- [x] Tests: Reviewer Compose previews; transfer/withdrawal/delete/standing tests green

## Landed

- `StandingSplitScreen` — Paytm-like sheet chrome (`PiSheetHandle`, title/helper), `PiCard` % rows, `PrimaryCta` Save (disabled when ≠100%), tokens from PIP-68
- `TransferScreen` — `LightBlueChip` for ₹1k/₹5k/₹10k, `PiCard` After transfer preview, `PrimaryCta` Move
- `WithdrawalScreen` — negative-amount presentation (`−₹…` + `Destructive` color), summary/`PiCard` rows, `PrimaryCta` Save and lock
- `DeleteGoalScreen` — release amount card, destination rows, destructive filled confirm CTA + confirm dialog
- Compose `@Preview`s on each screen for Reviewer side-by-side
- Unit smoke: `SheetsVisualPresentationTest` (negative INR chrome + token refs)

## Out of scope

- Allocation / validation / ViewModel logic (Done in PIP-52/54/56/58)
- Navigation / sheet host API changes (full-screen destinations keep existing routes; content hierarchy Paytm-like)

## Verify

`./gradlew assembleDebug test`
