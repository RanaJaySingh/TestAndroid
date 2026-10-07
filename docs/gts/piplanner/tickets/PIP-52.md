# PIP-52 — Android Standing split screen

**Status:** Implemented — rebased onto `f2e95c2` (PIP-48); `./gradlew assembleDebug test` BUILD SUCCESSFUL  
 
**Linear:** https://linear.app/telco-paytm/issue/PIP-52/android-implement-standing-split-screen  
**PRD:** R12 — Standing split always usable at 100% · https://docs.google.com/document/d/16cthPNS-djP0KLlH9Lak3OTDrottiD5bMnTJwW_cRDc/edit  
**Spec:** Section 3.2 BR-2, Section 5 · https://docs.google.com/document/d/1xg3FrN6802Ya8hEoiwiuE-Id3m-fPCHN929bVr0FW_k/edit  
**Design:** Frame 15 · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  

## Acceptance criteria

- [x] Given two+ goals, when Standing split (15) opened, then percentages editable
- [x] Given percentages, when they sum to 100%, then Save is enabled
- [x] Given percentages, when they sum ≠ 100%, then Save disabled with running total shown
- [x] Given one goal, when standing split UI would show, then it is skipped (100% automatic)
- [x] Given standing split saved, when next credit arrives, then it uses these percentages (`StandingSplitService.fractionsForNextCredit`)
- [x] Copy: "Saved money stays put" message visible
- [x] States: Multi-goal edit, One goal skip, Valid, Invalid
- [x] Unit test for 100% validation

## Landed

- `StandingSplitService`: BR-2 100% validation, shortfall messages, one-goal skip, `applyStandingSplit` (updates `standingSplits` + `shareOfNewCredits`, leaves saved/History alone), `fractionsForNextCredit` for PIP-48.
- `StandingSplitViewModel` + `StandingSplitScreen`: multi-goal % editing; Save gated at 100%; caption “Saved money stays put”; one-goal auto 100% skip.
- Navigation: Goals tab → Standing split (`PiPlannerRoutes.STANDING_SPLIT`).
- Unit tests: `StandingSplitServiceTest`, `StandingSplitViewModelTest`.

## Out of scope

- This-credit-only overrides / “Use this split” checkbox (PIP-48).
- Delete renormalize (PIP-54) — will call into `StandingSplitService`.

## Branch

`PIP-52-standing-split` rebased onto `f2e95c2` (main after PIP-48 Sync/Update + credit entry).

Conflict merge retained PIP-52 Standing Split API plus PIP-48 `percentagesForNextCredit` / credit + withdrawal nav.

Entry point is Goals tab → Standing split (placeholder removed with PIP-46).
