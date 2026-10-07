# PIP-74 — Android Welcome visual parity

**Status:** Implemented on `PIP-74-welcome-visual` (from `33b810b`)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-74/android-welcome-visual-parity  
**PRD:** R6 · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §5.2 J1 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** frame 1 · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  
**iOS twin:** PIP-73  

## Acceptance criteria

- [x] Given first-run / Reset demo, When Welcome shows, Then brand/tagline, numbered How-it-works steps 1–3, supporting copy, and navy primary CTA Set up savings match design layout/spacing
- [x] States handled: first launch; after Reset demo
- [x] Tests: Reviewer side-by-side screenshot vs design frame 1; existing Welcome tests green

## Landed

- `WelcomeScreen` / `WelcomeContent` — restyled with PIP-68 tokens + PIP-70 components (parity with iOS PIP-73):
  - App background `PiPlannerColors.BackgroundApp`
  - Brand **PiPlanner** as navy hero (`amountHero`); tagline navy-deep title; supporting subtitle body
  - **How it works** section title in navy; steps 1–3 inside `PiCard` with light-blue numbered circles
  - Primary CTA uses shared `PrimaryCta` (navy fill) with `welcome.cta` content description
  - Spacing from `PiPlannerDimens` (12 / 16 / 20 / 28)
- `PiCard` — optional `contentPadding` (default Space16; Welcome uses Space20)
- Compose previews: first launch + after Reset demo (same Welcome visual root)
- ViewModel / navigation / copy unchanged (greeting remains in `WelcomeUiState` for persona tests; not shown on Welcome visual per design frame 1 / iOS twin)

## Accessibility ids preserved

`welcome.brand`, `welcome.header`, `welcome.howItWorks`, `welcome.steps`, `welcome.step.1..3`, `welcome.cta`

## Out of scope

- Setup navigation/behaviour (PIP-34..66)
- ViewModel / product-behaviour changes
- Shared Theme/Components beyond Welcome wiring + PiCard padding param
- Icon mapping (PIP-72); later screen visual tickets

## Verify

`./gradlew assembleDebug test`
