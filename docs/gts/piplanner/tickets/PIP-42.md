# PIP-42 — Android Goal chat and form with Grok stub

Ticket: https://linear.app/telco-paytm/issue/PIP-42/android-implement-goal-chat-and-form-with-grok-stub

## Delivered

Goal chat + form + inflation popup under `ui/setup` with `GoalChatViewModel`, deterministic `StubGrokService` (Spec §3.3), `GoalValidationService` (form gating / 100% Continue / live adjusted target), wiring from Consent/balance → Goal chat → Opening split with defined goals, and unit tests.

## Mapping to Spec / PRD

| Requirement | Implementation |
|-------------|----------------|
| R5 Goal chat (5) → proposal (5b) | `StubGrokService.analyzeGoalInput` → `GoalChatScreen` proposal card |
| Edit / Confirm + checked label | Proposal card buttons; `StubGrokService.CHECKED_BY_LABEL` |
| Vague → ≤2 follow-ups (5a) then form | `GoalChatViewModel` + `GoalValidationService.MAX_FOLLOW_UPS` |
| Goal form Save gating (6) | `GoalValidationService.canSave` / `GoalFormDraft.canSave` |
| Inflation popup default 7% (7) | `InflationPopup` + `GoalValidationService.DEFAULT_INFLATION_RATE` |
| Grok unavailable (5c) | `StubGrokService(isUnavailable = true)` → hand-form CTA |
| Goals defined Continue @ 100% | `GoalValidationService.canContinueWithDefinedGoals` |
| States | `GoalChatPhase`: Chat, FollowUp, Proposal, Form, Unavailable, GoalsDefined |
| Spec §3.3 GrokService | `GrokService.kt` stub (no real AI) |

## Acceptance criteria

- [x] Goal chat: user describes goals → Grok stub returns proposal card (5b)
- [x] Proposal card shows Edit/Confirm and "Checked by PiPlanner. Estimate."
- [x] Vague input → up to two follow-ups (5a), then form
- [x] Goal form: Save disabled for empty name / zero target / invalid dates
- [x] Inflation popup (7): default 7% with live adjusted target
- [x] Grok unavailable (5c) → hand form path offered
- [x] Goals defined: Continue disabled when % sum ≠ 100%
- [x] States: Chat, Proposal, Follow-up, Form, Grok unavailable
- [x] Unit tests for GrokService stub + validation

## Test result

`./gradlew assembleDebug test` — **BUILD SUCCESSFUL**.

## Assumptions

- Design artifact page may not expose frame copy; UI copy inferred from PRD R5 / R19 + Spec §3.3 / §5.2 + iOS PIP-41.
- Happy-path stub proposals match demo persona: Car 60% / Emergency Fund 40% with suggested targets ₹5,00,000 / ₹2,00,000.
- Vague detection is keyword-based (no concrete goal nouns → follow-up); after two follow-ups the form opens.
- `StubGrokService(isUnavailable = true)` forces frame 5c for tests / demos; default stub is available.
- Money remains **Long paisa**; Opening split receives `definedGoals` from chat/form (falls back to `DemoData.sampleOpeningSplitGoals()` only if empty).

## Out of scope

Real Grok/AI integration; Ask tab; Goals tab UI beyond placeholder after Opening lock.
