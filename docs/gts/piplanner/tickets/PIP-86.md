# PIP-86 [Android] History entry open vs locked visual

Ticket: https://linear.app/telco-paytm/issue/PIP-86/android-history-entry-open-vs-locked-visual

## Delivered

Restyle of History credit-entry open / locked / typed treatments to design badges and split blocks (frames 13, 13a–13t). Visual / layout / token / component only — no ViewModel or lock/assign behaviour changes.

| Type / file | Role |
|-------------|------|
| `ui/goals/CreditEntryScreen.kt` | Open Assign now · locked Saved and locked + lock · Typed/Custom badges · already-saved + this-credit `PiCard` blocks · `PrimaryCta` |
| `ui/history/HistoryEntryDetailScreen.kt` | Locked New credit detail: Saved and locked + badges + this-credit allocations card; other types get token chrome |
| `ui/components/EntryBadge.kt` | Light-blue Typed / Custom badge |
| Lock icon | PIP-72 `PiIcons.lock` (prefer main shared icon chrome) |

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| PRD R12 open Assign now | Header status `CreditEntryService.ASSIGN_NOW_TITLE` + navy title treatment |
| PRD R12 Saved and locked + lock | Locked header + `PiIcons.lock` (PIP-72) |
| PRD R12 Typed / Custom badges | Light-blue `EntryBadge`; Typed when `isTyped`; Custom when locked && !typed (13d) |
| Already-saved / this-credit blocks | Separate `PiCard` sections — “Already saved, not changing” vs “Split ₹… · This credit only” |
| Spec §5.2 J2 History entry | Consumes PIP-68 tokens + PIP-70/74 `PiCard` / `PrimaryCta` + PIP-72 `PiIcons` |
| 13t no Balance now | Typed path omits Previous / Balance now rows (unchanged behaviour) |

## Acceptance criteria

- [x] Given open entry, When shown, Then Assign now treatment matches design
- [x] Given saved/locked entry, When shown, Then Saved and locked + lock icon match design
- [x] Given Typed/Custom, When badges show, Then match design; already-saved and this-credit split blocks match layout
- [x] States handled: open / locked / typed
- [x] Tests: credit entry behaviour tests green; `./gradlew assembleDebug test` green

## Visual summary (Reviewer)

1. **Open** — navy “Assign now” title; amount card; already-saved card; this-credit split card; Save and lock primary CTA; caption “You can change this split once.”
2. **Locked** — “Saved and locked” + lock icon; Custom badge (non-typed) or Typed badge; Done CTA; locked amounts caption.
3. **Typed (13t)** — Typed badge; Previous / Balance now omitted; New amount still shown.
4. Cards use white `PiCard` radius 22 + soft elevation; badges use light-blue token; app background `#F5F7FB`.

## Parallel work / base

Started from `33b810b` (PIP-70); rebased onto `1fcbdff` (PIP-74 Welcome; includes PIP-72 icons). Prefer main for shared tokens/icons/chrome/`PiCard`. Touches credit-entry / history-detail entry files + shared `EntryBadge`. No ViewModel / Service / History list row changes (list chrome is PIP-92).

## Test results

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL** (includes `CreditEntryServiceTest`, `HistoryDetailViewModelTest`, `HistoryEntryVisualTest`).

## How to run tests

```bash
./gradlew assembleDebug test
```

## Out of scope

Lock/assign logic (Done in PIP-48); History list rows (PIP-92); Goals home banner (PIP-82); ViewModel behaviour; iOS twin PIP-85.

## References

- PRD R12: https://docs.google.com/document/d/18r0wSKMTpePcjCRYypcKbtabghuCyd_TPGWhLEee0AU/edit
- Tech Spec §5.2: https://docs.google.com/document/d/1pvhxAPCyLrLIzEBNkiUh5-lOA8y7onLiTgl_eJTnlhk/edit
- Design frames 13, 13a–13t: https://claude.ai/artifact/VtHM2P9uhkH8o8o2kqFHE5
- Design note: https://docs.google.com/document/d/1Gk4Y77wpibYVTeysdDCK4zVY38t7tI9Q35FEbdZlt_U/edit
- Tracker: https://docs.google.com/spreadsheets/d/1ybQaMoz4FWHJoZ_5dXjKISegefE_6PyqmMZ9kX2bnSg/edit
- Drive: https://drive.google.com/drive/folders/1poNYECBNKrLgQ46LtcZQ3cKGllyuL5aT
- Linear: https://linear.app/telco-paytm/issue/PIP-86/android-history-entry-open-vs-locked-visual
