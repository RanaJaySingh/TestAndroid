# PIP-52 — Android Standing split screen

**Status:** In progress  
**Linear:** https://linear.app/telco-paytm/issue/PIP-52/android-implement-standing-split-screen  
**PRD:** R12 — Standing split always usable at 100% · https://docs.google.com/document/d/16cthPNS-djP0KLlH9Lak3OTDrottiD5bMnTJwW_cRDc/edit  
**Spec:** Section 3.2 BR-2, Section 5 · https://docs.google.com/document/d/1xg3FrN6802Ya8hEoiwiuE-Id3m-fPCHN929bVr0FW_k/edit  
**Design:** Frame 15 · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  

## Acceptance criteria

- [ ] Given two+ goals, when Standing split (15) opened, then percentages editable
- [ ] Given percentages, when they sum to 100%, then Save is enabled
- [ ] Given percentages, when they sum ≠ 100%, then Save disabled with running total shown
- [ ] Given one goal, when standing split UI would show, then it is skipped (100% automatic)
- [ ] Given standing split saved, when next credit arrives, then it uses these percentages
- [ ] Copy: "Saved money stays put" message visible
- [ ] States: Multi-goal edit, One goal skip, Valid, Invalid
- [ ] Unit test for 100% validation

## Scope

In: StandingSplitScreen, StandingSplitService (reusable for PIP-54 / PIP-48), persistence, navigation, strings, unit tests.  
Out: This-credit-only overrides (PIP-48).

## Branch

`PIP-52-standing-split` from `dac84393` (main after PIP-42 / PIP-50).
