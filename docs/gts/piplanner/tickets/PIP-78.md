# PIP-78 — Android Update balance / UPI Demo chrome visual

**Status:** Implemented on `PIP-78-update-upi-visual` (rebased onto `1fcbdff` / PIP-74)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-78/android-update-balance-upi-demo-chrome-visual  
**PRD:** R8 · https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit  
**Spec:** §5.2 · https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit  
**Design:** frames 4, 4a–4e, 11a–11c · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Design note:** https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit  
**Tracker:** https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit  
**Drive:** https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT  
**iOS twin:** PIP-77  

## Acceptance criteria

- [x] Given Update balance sheet, When shown, Then Manually / Balance sync choice rows match Paytm-like design
- [x] Given Manual amount, When ₹0, Then disabled Continue styling matches design; non-zero enables navy CTA look
- [x] Given UPI PIN Demo, When shown, Then Demo label, bank masked line, mock pad, Check balance / Cancel match design
- [x] States handled: choice; ₹0 vs amount; wrong PIN visual (no logic change)
- [x] Tests: Reviewer screenshots / Compose previews; existing PIN/update tests green

## Landed

| Type / file | Role |
|-------------|------|
| `ui/setup/UpdateBalanceUPIChrome.kt` | Shared choice rows, DEMO badge, bank masked line, PIN dots, mock pad |
| `ui/setup/ConsentSheet.kt` | `UpdateBalanceSheet` choice rows; `OtherAppScreen` / `WrongPinScreen` CTAs + wrong-PIN visual |
| `ui/setup/ManualBalanceScreen.kt` | ₹ amount in `PiCard`; navy `PrimaryCta` enabled / disabled at ₹0 |
| `ui/setup/UPIPinScreen.kt` | Demo badge, bank masked line, mock pad, Check balance / Cancel |
| `ui/goals/GoalsUpdateBalanceSheet.kt` | `PiSheet` + navy ₹ `PiCard` + `PrimaryCta` Apply (11a–11c visual; same apply flow) |

## Visual summary

| State | Chrome |
|-------|--------|
| Choice (4) | Title + helper; white choice rows (Manually / Balance sync) with icon, subtitle, chevron, soft card shadow |
| Manual ₹0 (4a / 11b) | Large navy ₹ field on `PiCard`; `PrimaryCta` Continue muted / disabled |
| Manual non-zero | Same field; Continue filled navy |
| UPI PIN Demo (4b) | DEMO chip; bank masked line; navy PIN dots; card mock pad; Check balance primary + Cancel outline |
| Wrong PIN (4d / 4e) | Destructive title + error dots; Try again primary / Enter manually outline |
| Other app (4c) | Primary CTA Enter balance manually |
| Goals Update (11) | PiSheet chrome; navy ₹ amount card; Apply muted at ₹0 |

## Out of scope

PIN validation / sync behaviour; ViewModel product logic; Welcome / Accounts / Consent Yes / Goals home / Sync Previous·Fetched·New (later tickets); iOS twin (PIP-77 Done).

## Verify

`./gradlew assembleDebug test`
