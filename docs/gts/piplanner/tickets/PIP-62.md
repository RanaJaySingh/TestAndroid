# PIP-62 — Android Settings screen

Ticket: https://linear.app/telco-paytm/issue/PIP-62/android-implement-settings-screen

## Delivered

Settings under `ui/settings` with `SettingsViewModel` + `SettingsScreen`:

- Linked accounts list (dedicated + spending)
- Automatic balance updates toggle (consent On / Off)
- Off→On (20b) navigates to `settings_consent` reusing PIP-40 `ConsentSheet`; Yes/No return to Settings without setup handoff
- Settings Yes uses `fetchesBalanceOnYes = false` (iOS PIP-61 parity): persists `consentAutoUpdate = true` only — does **not** overwrite dedicated balance with setup mock ₹1,00,000; balance updates stay on Goals Sync
- On→Off persists `consentAutoUpdate = false` so Goals shows Update balance
- Reset demo confirmation → `repository.resetDemo()` → Welcome (clears goals/history/standing split/accounts)
- Gear on Goals (existing PIP-46) → Settings
- Unit tests: consent persistence, Off→On consent navigation, Settings Yes does not replace balance with mock 10_000_000 paisa, reset, untyped gap (20c) as one synced amount

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| R17 Settings (20) | `SettingsScreen` via Goals gear |
| Consent On → Sync | Dedicated `consentAutoUpdate` + `GoalsTabService.balanceAction` |
| Consent Off (20a) → Update balance | Toggle Off persists false |
| Off→On (20b) → Consent sheet | `SETTINGS_CONSENT` + `ConsentViewModel.loadAccounts(fetchesBalanceOnYes = false)` |
| Settings Yes → consent On only | `chooseConsentYes` + `persistConsentFlag` (no `persistConsentAndBalance`) |
| Untyped gap (20c) | Sync delta vs last stored balance = one credit (`CreditEntryService`) |
| Reset demo → Welcome (1) | `confirmResetDemo` + nav `popUpTo(0)` → Welcome |
| Spec §5.2 SettingsScreen / SettingsViewModel | `ui/settings/` |
| Spec §5.3 no Settings tab (A4) | Gear only |

## Acceptance criteria

- [x] Settings (20) via Goals gear → Linked accounts + Automatic balance updates visible
- [x] Consent On → Sync available on Goals
- [x] Consent Off (20a) → Update balance on Goals
- [x] Consent Off→On (20b) → Consent sheet re-opens
- [x] Settings Off→On Yes persists consent On without mock balance overwrite (PIP-61 parity)
- [x] Untyped gap while Off (20c) → when On synced later, one amount
- [x] Reset demo → clear goals/history, show Welcome (1)
- [x] States: Consent On, Off, Reset confirmation
- [x] Tests: consent persistence + reset + Settings Yes balance preserved

## Test result

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL**.

## Assumptions

- Design artifact page did not expose frame copy; UI copy inferred from PRD R17 / J7 + PIP-40 consent bullets.
- Settings re-consent Yes skips Fetched balance → Goal chat; consent is persisted and user returns to Settings.
- Settings Yes does not fetch/overwrite dedicated balance (`fetchesBalanceOnYes = false`); Goals Sync owns post-setup balance deltas (20c).
- Reset clears all `AppState` (including accounts), matching `PersistenceService.resetDemo`.

## Out of scope

Separate Settings tab (A4); real Paytm/UPI backend.
