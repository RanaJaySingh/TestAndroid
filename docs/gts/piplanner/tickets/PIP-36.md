# PIP-36 — Android Welcome screen

## Landed

- `WelcomeViewModel` + `WelcomeScreen`: brand, tagline, three "How it works" steps, CTA **Set up savings** (copy parity with iOS PIP-35 / design frame 1).
- `AppLaunchRouter`: first-run and post–Reset (`AppState.EMPTY` / cleared goals+history) → Welcome; `hasCompletedSetup` or locked Opening balance → Goals tab.
- `PiPlannerNavHost` resolves start route from persisted state; CTA navigates to existing `AccountsScreen` (PIP-38). Opening split + Consent routes remain registered.
- Unit tests: step copy + CTA navigation (`WelcomeViewModelTest`); first-run / post-reset / setup-complete routing (`AppLaunchRouterTest`).

## Out of scope

- Loading state UI.
- Settings Reset demo wiring (returns to Welcome once Settings lands; router already treats post-reset empty state as Welcome).
