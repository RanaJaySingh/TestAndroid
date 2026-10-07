# PIP-76 [Android] Accounts + Consent + opening balance visual

Ticket: https://linear.app/telco-paytm/issue/PIP-76/android-accounts-consent-opening-balance-visual

## Delivered

Restyle setup Accounts (frame 2), Consent sheet (frame 3), and Opening balance fetched (frame 3a) to design structure and Paytm-like sheet chrome — **visual / layout / token / component wiring only**. Toggle exclusivity and consent logic unchanged (already Done).

| Type / file | Role |
|-------------|------|
| `ui/setup/AccountsScreen.kt` | Step 1 of 3 (navy caption), `PiCard` account rows + navy-tinted Dedicated switches, gated `PrimaryCta` Continue, `BackgroundApp` |
| `ui/setup/ConsentSheet.kt` (`ConsentSheetContent`) | `PiSheetChrome` (handle/title/helper); four navy check bullets; Yes `PrimaryCta` / No `SecondaryCta` outline; Settings 20b hides step via `showsSetupStep` |
| `ui/setup/ConsentSheet.kt` (`FetchedBalanceContent`) | Step 2 of 3; large `amountHero` ₹ amount in `PiCard` (navy); `PrimaryCta` Continue |
| `ui/navigation/PiPlannerNav.kt` | Settings consent route passes `showsSetupStep = false` |
| `AccountsConsentVisualTest` | Token / bullet / amountHero smoke |

Frame 4 / 4c / 4d–4e views in the same Consent file (`UpdateBalanceSheet`, `OtherAppScreen`, `WrongPinScreen`) are **not** restyled here — owned by PIP-78.

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| PRD R7 Accounts (2) | Step indicator + account cards/toggles + Continue gated chrome |
| PRD R7 Consent (3) | Sheet chrome over context; four check bullets; Yes / No buttons |
| PRD R7 Opening balance (3a) | Step 2 of 3 + large ₹1,00,000 treatment (`amountHero` + navy) |
| Spec §3.5 / PIP-70 | Consumes `PiCard`, `PrimaryCta`, `SecondaryCta`, `PiSheetChrome` + PIP-68 tokens |
| Spec §5.2 J1 Setup | Screens 2, 3, 3a only |

## Acceptance criteria

- [x] Given setup Accounts, When shown, Then step indicator, account cards/toggles, and Continue gated chrome match design
- [x] Given Consent, When presented, Then sheet over context with four check bullets and Yes/No buttons match design
- [x] Given Opening balance fetched (3a), When shown, Then Step 2 of 3 and large ₹1,00,000 treatment match design
- [x] States: Continue disabled/enabled; Consent Yes/No visual
- [x] Tests: behaviour tests green; Compose previews for key states; `./gradlew assembleDebug test` green

## Reviewer checklist (frames 2 / 3 / 3a)

Compare device/emulator (or Compose previews) to design artifact:

1. **Accounts (2):** “Step 1 of 3” caption (navy); white account cards radius 22; Dedicated toggles navy tint; Continue uses navy `PrimaryCta` — disabled until exactly one dedicated, enabled when one is on.
2. **Consent (3):** Paytm-like sheet chrome (handle, title “Allow balance checks?”, helper); four checkmark bullets; Yes filled navy / No outline secondary.
3. **Fetched (3a):** “Step 2 of 3”; large navy ₹ amount (Indian grouping, e.g. ₹1,00,000); Continue primary CTA.
4. No ViewModel / toggle-exclusivity / consent-fetch behaviour changes.

## Previews

- `AccountsScreen` — none dedicated · Continue disabled; one dedicated · Continue enabled
- `ConsentSheet` — Consent · Yes / No; Consent · Settings 20b
- `FetchedBalanceContent` — Fetched balance 3a · ₹1,00,000

## References

- PRD R7: https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit
- Tech Spec §5.2: https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit
- Design frames 2, 3, 3a: https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5
- Design note: https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit
- Tracker: https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit
- Drive: https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT
- Blocked by: PIP-70 (shared components on main)
- iOS twin: PIP-75

## Parallel work / base

Started from `33b810ba` (main after PIP-70 #20). Previously rebased onto `1fcbdff7` (PIP-74). **Current tip:** rebased onto `1f3bb8fa` (main after PIP-94 Settings #25; also includes PIP-80 / PIP-88 / PIP-84). Keep-both on shared `strings.xml` (PIP-76 role labels + main goal-detail / sync / settings strings). Touches Accounts / Consent / FetchedBalance UI (+ nav flag + GTS doc). Avoids Theme/, components foundation, Welcome (PIP-74), Update/UPI (PIP-78).

## Test results

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL** after rebase onto `1f3bb8fa` (includes `AccountsConsentVisualTest` + existing behaviour suites).

## How to run tests

```bash
./gradlew assembleDebug test
```

## Out of scope

Toggle exclusivity / consent logic; ViewModel / money behaviour; Update balance / UPI chrome (PIP-78); Welcome (PIP-74); Goal chat/split (PIP-80); Goals home (PIP-82); iOS twin (PIP-75).
