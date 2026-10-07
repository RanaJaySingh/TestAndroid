# PIP-66 — Android Demo persona data seeding

**Status:** Implemented on `PIP-66-demo` (based on `e95618b` / PIP-56 merge)  
**Linear:** https://linear.app/telco-paytm/issue/PIP-66/android-implement-demo-persona-data-seeding  
**PRD:** R25 (Spending account payments not seen), §10 Data and Integrations · https://docs.google.com/document/d/16cthPNS-djP0KLlH9Lak3OTDrottiD5bMnTJwW_cRDc/edit  
**Spec:** Section 3.1 Account model, Section 5 · https://docs.google.com/document/d/1xg3FrN6802Ya8hEoiwiuE-Id3m-fPCHN929bVr0FW_k/edit  
**Design:** Persona setup · https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5  

## Acceptance criteria

- [x] Given app launch, when demo persona available, then Rahul name visible in greeting
- [x] Given demo accounts, when seeded, then HDFC ••4821 ₹1,00,000 (dedicated) and SBI ••7730 ₹72,000 (spending) exist
- [x] Given spending account (SBI), when payments occur, then PiPlanner shows nothing for those payments
- [x] Given Grok stub, when goal proposals shown, then happy path matches design (Car / Emergency fund)
- [x] Given "Good evening, Rahul" greeting, when time-of-day varies, then greeting adjusts appropriately
- [x] States handled: First launch, Post-reset
- [x] Tests: Unit test for demo data initialization

## Landed

- `DemoData`: persona constants (Rahul / HDFC ••4821 / SBI ••7730), `sampleAccounts()` (setup, none dedicated), `seededPersonaAccounts()` (HDFC dedicated), `initializeDemo()`, time-of-day `greeting()`, R25 helpers (`trackedAccountIds`, spending note).
- Welcome + Goals: persona greeting ("Good morning/afternoon/evening, Rahul").
- Accounts: spending-account note (R25); BalanceSync Hilt mocks track only dedicated savings ID.
- `StubGrokService.HAPPY_PATH_PROPOSALS` aligned with `DemoData` Car / Emergency Fund names.
- Unit tests: `DemoDataTest`, Welcome greeting assertion, BalanceSync spending-not-tracked.

## Out of scope

- Real backend data, real user authentication.
- Settings Reset demo UI (PIP-62); reset still clears to empty `AppState` via persistence.

## Verify

`./gradlew assembleDebug test` — run on tip before ready-for-review.
