# PIP-64 — Android Ask tab with Grok answers and proposals

**Status:** Implemented on `PIP-64-ask` (rebased onto `edb62e3` / PIP-66 Demo; keep-both with Demo seeding + History + Ask)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-64/android-implement-ask-tab-with-grok-answers-and-proposals  
**PRD:** R18 (Ask answers and proposal cards), R19 (Grok unavailable and invalid draft fallbacks) · https://docs.google.com/document/d/16cthPNS-djP0KLlH9Lak3OTDrottiD5bMnTJwW_cRDc/edit  
**Spec:** Section 3.3 GrokService, Section 5 · https://docs.google.com/document/d/1xg3FrN6802Ya8hEoiwiuE-Id3m-fPCHN929bVr0FW_k/edit  
**Design:** Frames 19, 19a, 19b, 19c, 19d · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  
**Review FAIL r1:** https://github.com/RanaJaySingh/TestAndroid/pull/17#pullrequestreview-5437236663 — Edit=Confirm for Transfer/ChangeSplit; unavailable templates open fallbacks without Grok (iOS PIP-63 parity).  

## Acceptance criteria

- [x] Given Ask (19), when opened, then chips and input field shown
- [x] Given question with no action, when answered (19a), then plain-text answer uses engine numbers
- [x] Given action sentence, when detected (19b), then proposal card with Edit/Confirm and "Checked by PiPlanner. Estimate." shown
- [x] Given proposal Confirm, when tapped, then matching sheet opens pre-filled (13f Goal form / 16c Transfer; ChangeSplit → Standing split)
- [x] Given Grok unavailable (19c), when detected, then fallback forms/sliders/templates offered
- [x] Given invalid draft (19d), when received, then user asked once more, then Goal form; invalid drafts never shown
- [x] States handled: Input, Plain answer, Proposal, Unavailable, Invalid draft
- [x] Tests: Unit tests for GrokService stub responses and fallbacks

## Landed

- `StubGrokService.askQuestion`: plain answers for chips/questions; Transfer / AddGoal / ChangeSplit action proposals; InvalidDraft / Unavailable errors (never display invalid drafts).
- `AskAnswerService`: plain answers from live goal/standing-split engine numbers (R18 / 19a).
- `AskTab` + `AskViewModel` + `ProposalCard`: chips, input, phases Input / PlainAnswer / Proposal / Unavailable / InvalidDraft / GoalForm.
- Confirm **and Edit** share `openSheet` — Transfer / Standing / Goal form (FAIL r1 / iOS PIP-63).
- Confirm/Edit ChangeSplit seeds Standing via `AskStandingSplitSeed`.
- Unavailable (19c): template tap opens Transfer ₹5k / Vacation Goal form / Standing **without** calling Grok; Use a form CTA retained.
- Unit tests: `GrokServiceTest` Ask cases, `AskAnswerServiceTest`, `AskViewModelTest` (Edit + unavailable template no-Grok).

## Out of scope

- Real Grok/AI integration.
- Full credit-entry return path for mid-entry Ask (13f from open History entry) — Confirm opens Goal form from Ask tab.

## Verify

`./gradlew assembleDebug test`
