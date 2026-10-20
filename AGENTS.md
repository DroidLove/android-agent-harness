# AGENTS.md — the contract

You are working in an Android codebase. This file is not documentation for humans; it is the
operating contract. Read it fully before your first edit. If something here conflicts with the
task you were given, stop and ask.

---

## 1. Orient before acting

1. Read this file.
2. Read `docs/runbook.md` for the task protocol.
3. Run `./gradlew verify` **before** you change anything, and record the duration. If it fails
   on a clean tree, stop — the tree is broken, not you.
4. Find the module you're changing. Read at least one existing file in it that does something
   similar. Match it. Do not invent a new pattern.

---

## 2. The only commands you need

```bash
./gradlew verify                      # compile + unit tests + lint. THE gate.
./gradlew :app:testDebugUnitTest      # unit tests, app module only
./gradlew :core:ui:testDebugUnitTest  # unit tests, ui module only
./gradlew :app:recordRoborazziDebug   # write golden screenshots (only when intended)
./gradlew :app:verifyRoborazziDebug   # compare against committed goldens
./gradlew :app:lintDebug              # lint only
```

Prefer module-scoped tasks. `:app:testDebugUnitTest` is seconds; `verify` is the full gate.
Run the scoped task while iterating, `verify` before you hand back.

---

## 3. Definition of Done

A task is done only when **all** of these are true:

- [ ] `./gradlew verify` passes.
- [ ] If UI changed: `./gradlew :app:verifyRoborazziDebug` passes, and the diff in
      `app/src/test/screenshots/` is intentional and explained in the PR body.
- [ ] Tests exist for new behaviour, written **before** the implementation where practical.
- [ ] No test was deleted, renamed away, `@Ignore`d, or weakened to make the build pass.
- [ ] No new dependency was added.
- [ ] Diff is under ~250 changed lines. If it isn't, the task was too big — stop and propose a split.
- [ ] You can explain every changed file in one sentence.

If you cannot tick a box, say so explicitly in your summary rather than working around it.

---

## 4. Never do these without explicit human approval

Enforced by `.github/workflows/guards.yml`, so attempting them wastes a CI cycle:

| Area | Paths | Why |
|---|---|---|
| Dependencies | `**/build.gradle*`, `gradle/libs.versions.toml` | Supply-chain risk; agents add libraries to solve 10-line problems |
| CI / release | `.github/**`, `scripts/**` | An agent must not be able to weaken its own gate |
| Secrets | any `.env`, keystore, `*.jks`, `local.properties` | Credential leakage |
| Test integrity | deleting or disabling tests | Reward hacking — the most common failure |

Also never:
- Run `git push`, `git reset --hard`, `git clean -fd`, or rewrite history.
- Edit `gradle.properties` to disable the configuration cache, R8, or lint.
- Add `@Suppress` / `@SuppressLint` to silence a warning you didn't read.
- Change `minSdk` / `targetSdk` / `compileSdk`.
- Touch anything outside the task's stated scope, including "while I was here" cleanups.

---

## 5. How to work a task

Given a task spec (see `docs/runbook.md` for the format):

1. **Restate it.** One paragraph: what changes, what doesn't, how we'll know it worked.
   If the spec is ambiguous, ask — do not pick an interpretation silently.
2. **Find the acceptance test.** Write or identify the test that will fail before your change and
   pass after. If you can't articulate one, the task isn't a task.
3. **Plan, briefly.** 3–6 bullets of files and approach. No essay.
4. **Implement in small steps.** Run the scoped test after each step. Do not batch five changes
   and then run the build once.
5. **Self-review the diff** as if someone else wrote it: scope creep, dead code, silent behaviour
   changes, unnecessary abstraction.
6. **Report.** What changed, why, what you verified, what you're unsure about. Uncertainty is
   expected; hidden uncertainty is not.

---

## 6. Code conventions

- **Kotlin**, 4-space indent, trailing commas, explicit return types on public functions.
- **Compose** for all UI. No new XML layouts.
- **State**: immutable `data class` / `sealed interface` UI state, hoisted to the caller.
  Composables are pure functions of state; side effects go in `LaunchedEffect` / the caller.
- **No business logic in composables.** A composable may format; it may not decide.
- **Naming**: `XxxScreen` for a screen, `XxxState` for state, `XxxReducer` for pure transitions.
- **Comments** explain *why*, never *what*. If the code needs a "what" comment, rewrite the code.
- Follow the module boundaries in §7. Do not create a module.

---

## 7. Module map

```
:app          Application shell, navigation, DI wiring. May depend on :core:ui.
:core:ui      Theme, design tokens, shared composables. Depends on nothing in this repo.
```

Rules:
- `:core:ui` must never depend on `:app`.
- Screenshot tests live beside the code they test.
- If you think a new module is needed, propose it — don't create it.

---

## 8. When you are stuck

- The build fails for reasons unrelated to your change → report it, don't fix it silently.
- A test fails and you believe the test is wrong → say so and **stop**. Do not edit it.
  Changing a test to match your implementation is the failure mode this repo exists to prevent.
- The task needs more than ~250 lines → propose a split.

---

## 9. Honesty clauses

- Never claim a command passed if you didn't run it.
- Never say "tests pass" without pasting the result summary.
- If you skipped a step, name the step.
- If you're guessing at an API, say "I'm not certain this API exists" before using it.

A confident wrong answer costs more than a hedged correct one.
