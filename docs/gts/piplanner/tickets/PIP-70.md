# PIP-70 — Android Shared visual components (Card, CTA, Chip, Sheet, Proposal)

**Status:** Implemented on `PIP-70-shared-visual-components` (from `77d4752`)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-70/android-shared-visual-components-card-cta-chip-sheet-proposal  
**PRD:** R2, §9 component inventory · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §3.5, §5.1 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  

## Acceptance criteria

- [x] Given design cards, when PiCard renders, Then white surface + corner radius 20–24dp + soft elevation
- [x] Given chips/CTAs/sheets/proposal shell, when rendered in previews, Then navy primary button, outline/text secondary, light-blue chip fill, Paytm-like sheet chrome, and ProposalCard Edit/Confirm + “Checked by PiPlanner. Estimate.” match design hierarchy
- [x] States handled: enabled/disabled primary CTA; chip selected/unselected
- [x] Tests: Compose previews and/or screenshot smoke; existing ViewModel tests green

## Landed

- `ui/components/PiCard` — white `SurfaceCard`, `RadiusCard` 22dp, soft `ElevationCard` 2dp
- `ui/components/PrimaryCta` / `SecondaryCta` — navy filled primary; outline + text secondary
- `ui/components/LightBlueChip` — iOS PIP-69 contract (Reviewer r1):
  - **selected:** `ChipLightBlue` fill + navy stroke (`ChipSelectedStroke` 1.5dp) + navy semibold label
  - **unselected:** fill-only `ChipLightBlue` (no navy stroke)
  - Must **not** invert to navy-fill / white-label
- `ui/components/PiSheet` + `PiSheetChrome` — ModalBottomSheet wrapper + in-content Paytm-like chrome (handle, top radius, title spacing)
- `ui/components/ProposalCard` — shell with body slot, Edit/Confirm, checked-by caption (default `StubGrokService.CHECKED_BY_LABEL`)
- `SharedVisualComponentsPreview` — Compose preview gallery (demo only)
- Unit smoke: `SharedVisualComponentsTest` (includes selected-chip contract)
- Dimens: `ElevationCard`, `ChipSelectedStroke`, `SheetHandleWidth` / `SheetHandleHeight`

## Reviewer must-fix

| # | Finding | Fix |
| :- | :---- | :---- |
| r1 | Selected LightBlueChip used navy fill + white label | Match iOS PIP-69: soft-blue fill + navy stroke + navy label; unselected fill-only |

## Out of scope

- Per-screen adoption beyond demos (PIP-74+); behaviour from PIP-34..66; NavyBalanceCard / GoalCard / TabBar (later tickets); icon mapping (PIP-72).

## Verify

`./gradlew assembleDebug test`
