# PIP-38 — Android Accounts screen with dedicated toggle

## Landed

- `DedicatedAccountService`: BR-1 exclusivity — toggling Dedicated ON clears others; Continue requires exactly one dedicated (R2 / R21).
- `AccountsViewModel` + `AccountsScreen`: lists HDFC ••4821 and SBI ••7730; Dedicated switches; Continue gated; persists accounts then navigates to Consent placeholder.
- `ConsentPlaceholder`: minimal PIP-40 stand-in (same pattern as `GoalsTabPlaceholder`).
- `PiPlannerNavHost` starts at Accounts; Opening split + Goals routes remain registered (PIP-44 intact).
- Demo seed `DemoData.sampleAccounts()` with none dedicated initially.
- Unit tests: toggle exclusivity + Continue gating (`DedicatedAccountServiceTest`, `AccountsViewModelTest`).

## Out of scope

- Add/remove accounts.
- Consent sheet / balance entry (PIP-40).
- Welcome screen.
