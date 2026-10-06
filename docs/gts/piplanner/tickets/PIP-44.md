# PIP-44 — Android Opening split screen

## Landed

- `OpeningSplitService`: BR-2 100% validation (BigDecimal fractions), largest-remainder paisa allocation, locked `HistoryEntry` creation, apply lock to goals / standing splits / history (BR-3); sets `hasCompletedSetup`.
- `OpeningSplitViewModel` + `OpeningSplitScreen`: multi-goal % editing; single-goal auto 100% with no % fields; confirm → lock; read-only mode with “Locked amounts never change”.
- Navigation to `GoalsTabPlaceholder` (PIP-46 placeholder) after lock.
- Demo seed in `DemoData.sampleOpeningSplitGoals()` so the screen is reachable before earlier setup tickets land.
- Unit tests: 100% validation + History entry creation (`OpeningSplitServiceTest`); ViewModel lock / single-goal / read-only (`OpeningSplitViewModelTest`).

## Out of scope

- Edit of opening entry after lock.
- Full Goals tab UI (PIP-46).
- Welcome / Accounts / Goal form setup screens.
