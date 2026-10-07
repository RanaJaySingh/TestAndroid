# PIP-68 — Android Design tokens & theme (navy, radii, typography, spacing)

**Status:** Implemented on `PIP-68-design-tokens-theme` (from `b9c342b`)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-68/android-design-tokens-and-theme-navy-radii-typography-spacing  
**PRD:** R1, R2 (token values) · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §3.1 Design tokens, §5.1 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  

## Acceptance criteria

- [x] Given approved navy range #0A2A6B–#003A8C, when Theme.kt / PiPlannerColors ship, Then primary is navy (not #0B3D2E), background is not cream #F7F4EF, and light-blue chip + positive green + behind tokens exist
- [x] Given card/chip/sheet radii, when Dimens/constants ship, Then card radius is in 20–24dp (prefer 22) and spacing scale 8/12/16/20/24/28 is available
- [x] States handled: Material3 light ColorScheme remapped; grep shows no remaining brand-green primary on chrome
- [x] Tests: theme/token unit smoke; existing domain/UI ViewModel tests remain green

## Landed

- `PiPlannerColors` — navy primary/deep, chip light-blue, positive green, behind, destructive, white card surface, light app background (not cream).
- `PiPlannerDimens` — card/sheet radius 22dp, chip 12dp, spacing 8/12/16/20/24/28.
- `PiPlannerTypography` — amountHero / title / body / caption + Material3 Typography wiring.
- `PiPlannerLightColorScheme` + `PiPlannerTheme` — Material3 light scheme remapped to navy tokens.
- Launcher chrome `ic_launcher_background` updated from forest green to navy.
- Unit smoke: `ThemeTokensTest`.

## Out of scope

- Per-screen restyles (PIP-70+); behaviour from PIP-34..66; new product flows; shared components/icons (PIP-70, PIP-72).

## Verify

`./gradlew assembleDebug test`
