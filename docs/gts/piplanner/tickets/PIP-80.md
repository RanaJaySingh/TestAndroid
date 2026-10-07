# PIP-80 [Android] Goal chat, form, inflation, opening split visual

Ticket: https://linear.app/telco-paytm/issue/PIP-80/android-goal-chat-form-inflation-opening-split-visual

## Delivered

Restyle Goal chat, Goal form, Inflation popup, and Opening split to design cards / proposal treatment / Lock this split CTA — **visual / layout / token / component wiring only**. No ViewModel or product-behaviour changes. Consumes PIP-70 shared components (`ProposalCard`, `PiCard`, `PrimaryCta`, `SecondaryCta`, `LightBlueChip`, `PiSheet`) and PIP-68 `PiPlannerColors` / `PiPlannerDimens` / `PiPlannerTypography`.

| Type / file | Role |
|-------------|------|
| `ui/setup/GoalChatScreen.kt` | Bubbles (light-blue user / white assistant), shared `ProposalCard` (“Grok's proposal”), Use a form link, unavailable + goals-defined `PiCard` / `PrimaryCta` |
| `ui/setup/GoalFormScreen.kt` | Field stack in `PiCard`, valid/invalid chrome, inflation row, live metrics, `PrimaryCta` Save / `SecondaryCta` Cancel |
| `ui/setup/InflationPopup.kt` | `PiSheet` chrome, ± stepper (`LightBlueChip`), live adjusted target, `PrimaryCta` “Use this rate” |
| `ui/setup/OpeningSplitScreen.kt` | Balance + goal rows in `PiCard`, % fields, `PrimaryCta` “Lock this split” (enabled @ 100%) |

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| PRD R9 proposal card | `ProposalCard(title = "Grok's proposal", …)` + `checkedByLabel` |
| PRD R9 form / inflation / split | Field stack + inflation stepper sheet + Lock CTA |
| Spec §3.5 components | Consume shared `ui/components/` — no second proposal shell |
| Spec §5.2 J1 screens 5–8 | Visual-only restyle of existing setup screens |

## Acceptance criteria

- [x] Given Goal chat, When proposal shows, Then “Grok's proposal”, Edit/Confirm, “Checked by PiPlanner. Estimate.”, bubbles, and Use a form link match design
- [x] Given Goal form + Inflation sheet, When rendered, Then field stack, inflation row/stepper, live targets, Use this rate match design
- [x] Given Opening split, When rendered, Then goal rows, %, Lock this split CTA match design
- [x] States: chat idle/proposal; form valid/invalid chrome; split ≠100% vs 100% CTA
- [x] Tests: behaviour tests green; `./gradlew assembleDebug test`

## Reviewer checklist

Compare device/emulator to design frames 5, 5a–5c, 6, 7, 8, 8b:

1. Proposal uses shared `ProposalCard` hierarchy (not a forked shell); title is “Grok's proposal”.
2. Chat bubbles: user = light-blue chip fill; assistant = white card surface; app background `#F5F7FB`.
3. “Use a form” is navy text (secondary treatment).
4. Form invalid: amber behind stroke + helper; Save `PrimaryCta` disabled. Valid: clean card + enabled Save.
5. Inflation sheet: PiSheet handle/title; ± stepper; live adjusted ₹; CTA label **Use this rate**.
6. Opening split: navy Lock this split CTA enabled only when % total 100%; disabled (muted) when ≠100%.

## References

- PRD R9: https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit
- Tech Spec §5.2: https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit
- Design: https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5
- Design note: https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit
- Tracker: https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit
- Drive: https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT
- Blocked by: PIP-70 (shared components)
- Related: iOS twin PIP-79

## Parallel work / base

Started from `33b810b` (main after PIP-70). Touched **only** GoalChat / GoalForm / Inflation / OpeningSplit screen files + strings + this ticket doc. Prefer existing `ui/components/ProposalCard` — no second proposal shell.

## Test results

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL** (behaviour suites green).

## How to run tests

```bash
./gradlew assembleDebug test
```

## Out of scope

Grok stub / validation / lock logic (Done); ViewModel behaviour; iOS twin (PIP-79); Goals home / Ask / other screen tickets.
