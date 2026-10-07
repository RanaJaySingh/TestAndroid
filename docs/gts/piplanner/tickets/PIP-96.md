# PIP-96 [Android] Ask tab visual parity

Ticket: https://linear.app/telco-paytm/issue/PIP-96/android-ask-tab-visual-parity

## Delivered

Restyled Ask tab idle / answer / proposal / unavailable chrome (frames 19, 19a–19d) under `ui/ask/AskTab.kt` to design chips, Ask Grok input, plain answer card, and shared ProposalCard — using PIP-68 tokens and PIP-70 components. Visual / layout only — Grok stub and confirm routing unchanged. Removed the forked `ui/ask/ProposalCard.kt` in favour of shared `ui/components/ProposalCard`.

| Surface | Visual treatment |
|---------|------------------|
| Screen chrome | `PiPlannerColors.BackgroundApp`; intro title + subtitle |
| Suggestion chips (idle) | `LightBlueChip` soft-blue fill; selected = navy stroke |
| Ask Grok input | White field, chip radius, navy border; `PrimaryCta` Ask |
| Plain answer (19a) | `PiCard` — Answer title + body hierarchy |
| Proposal (19b) | Shared `ProposalCard` Edit/Confirm + “Checked by PiPlanner. Estimate.” |
| Unavailable (19c) | `PiCard` + template `LightBlueChip`s + Use a form / Standing split CTAs |

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| PRD R17 Ask tab visual parity | Intro copy, light-blue chips, Ask Grok input, answer, proposal card |
| Tech Spec §5.2 J8 | Frames 19 / 19a–19d visual must-match |
| Spec §3.5 ProposalCard / LightBlueChip | Consume shared `ui/components/` — no second proposal shell |
| Design tokens | `PiPlannerColors`, `PiPlannerDimens`, `PiPlannerTypography` |
| Out: Grok stub / confirm routing | `AskViewModel` untouched |

## Acceptance criteria

- [x] Given Ask idle, When shown, Then intro copy, light-blue suggestion chips, Ask Grok input match design
- [x] Given plain answer, When shown, Then answer card hierarchy matches design
- [x] Given proposal, When shown, Then ProposalCard Edit/Confirm + footer match design
- [x] States handled: idle / answer / proposal / unavailable chrome (visual only)
- [x] Tests: Ask behaviour tests green; `./gradlew assembleDebug test` green

## Reviewer checklist (Ask)

Compare device/emulator to design artifact frames 19 / 19a–19d:

1. Idle: intro subtitle; suggestion chips are light-blue (not outlined Material buttons).
2. Input placeholder reads **Ask Grok**; Ask CTA is filled navy.
3. Plain answer uses white `PiCard` (radius 22) with Answer title + body — not surfaceVariant gray.
4. Proposal uses shared `ProposalCard` hierarchy (Edit outline / Confirm navy + checked-by footer).
5. Unavailable: card chrome + light-blue template chips + Use a form / Standing split — not inventing extra marketing blocks.
6. App background is light (`#F5F7FB`).

## Parallel work / base

Started from `1f3bb8fa926265df96f4324af374522d1b2da7db` (main after PIP-94). Touched AskTab (+ deleted ask-local ProposalCard) + this ticket doc so parallel visual tickets rebase cleanly.

**PASS_HOLD tip-lag rebases (2026-10-07):**
1. Onto `1a77a3b87855d3b6dfa548db903d9d4503396f56` (PIP-90 MERGED) — clean.
2. Onto `4552dca40dc3e4802d9141ed33b133e3bdb18f3c` (PIP-78 MERGED) — clean.
3. Onto `5f938820da093121a2ccdcdb126c01d65c7a53a5` (PIP-92 MERGED) — clean.
4. Onto `6f5ab4b7a54ceab331f079877a81b1ee0fb4c16f` (PIP-86 MERGED) — clean.
5. Onto `7aa7299159142612818b6c28152b1181211ec494` (PIP-76 MERGED) — clean. Do not treat `6f5ab4b7` / `9e84e960` as tip-current.

## Test results

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL** (Ask + shared-component suites green).
Post-rebase onto `7aa72991`: local assemble required on new head before pr_ready.

## Out of scope

Grok stub / confirm routing (PIP-64 Done); inventing new Ask chrome (R18); iOS twin (PIP-95); ViewModel / domain behaviour.

## References

- PRD R17: https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit
- Tech Spec §5.2 J8: https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit
- Design frames 19, 19a–19d: https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5
- Design note: https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit
- Tracker: https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit
- Drive: https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT
- Blocked by: PIP-70 (shared components), PIP-72 (icons/tab chrome)
- Related: PIP-95 (iOS twin); uses PIP-68 tokens
