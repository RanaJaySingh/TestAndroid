# PIP-34 — Android project setup, data models, persistence

## Landed

- Greenfield Jetpack Compose app (`com.piplanner`), minSdk 26, Gradle version catalog, Hilt DI.
- Shared-contract models (Spec §3.1): `Account`, `Goal` (+ computed adjustedTarget/monthlyNeed/status), `HistoryEntry`, `GoalAllocation`, `StandingSplit`, enums/status types.
- `PersistenceService` (DataStore JSON) + `PiPlannerRepository`; `resetDemo()` clears all state (fresh / post-reset → empty `AppState`).
- `FormattingService`: Long paisa → `₹X,XX,XXX` Indian grouping (R20 / BR-10).
- MVVM scaffolding: `AppViewModel` with `StateFlow<AppState>`; Compose host only — **no UI screens**.
- Unit tests: INR formatting, model JSON serialization, persistence reset.

## Out of scope (later tickets)

Welcome/Accounts/Goals UI, BalanceSync/Grok stubs beyond package layout, demo persona seeding (PIP-66).
