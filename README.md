# android-agent-harness

[![verify](https://github.com/DroidLove/android-agent-harness/actions/workflows/verify.yml/badge.svg)](https://github.com/DroidLove/android-agent-harness/actions/workflows/verify.yml)

**A reference setup for running AI coding agents safely on a production Android codebase.**

Coding agents are good at writing code and bad at *proving* it works. On Android that gap is
worse than on backend or web: builds are slow, UI verification needs an emulator, and there is
no DOM snapshot to compare against. An agent that gets two attempts per hour isn't engineering,
it's guessing.

This repo is the harness that fixes that: one command to verify, screenshot tests that run on
the JVM in milliseconds, and CI gates that stop an agent from quietly weakening the tests that
judge it.

---

## The four mechanisms

| Mechanism | What it solves | Where |
|---|---|---|
| **One `verify` command** | Agents waste context guessing which tasks to run | `./gradlew verify` |
| **JVM screenshot tests** | No emulator in the loop → UI changes are verifiable in ~1s | `app/src/test/.../*ScreenshotTest.kt` |
| **Test-integrity guard** | Agents delete or `@Ignore` failing tests to make CI pass | `.github/workflows/guards.yml` |
| **Blast-radius rules** | Dependency, CI and payment changes need a human | `AGENTS.md`, `guards.yml` |

Plus: configuration cache and build cache on, module-scoped builds, and a written task protocol
so the agent gets a spec rather than a wish.

---

## Results

Measured on **Apple M2 (8 cores), 16 GB, macOS 26.6.2, JDK 17 (Temurin), Gradle 8.11.1**,
2026-10-20. Rerun with `RUNS=3 ./scripts/benchmark-verify.sh`.

| Measurement | Value | How |
|---|---|---|
| First run on a fresh machine (downloads + SDK components) | **8m 45s** | first `./gradlew verify`, ~1 GB fetched |
| Clean tree, build cache disabled, cold daemon | **17.4s** | `./gradlew clean && ./gradlew verify --no-build-cache` |
| Clean tree, warm build cache | **3.5s** | `./gradlew clean && ./gradlew verify` |
| Warm `verify` (compile + tests + lint + screenshots) | **4s** | 3 runs: 4s / 4s / 4s |
| Module-scoped unit tests while iterating | **3s** | 3 runs: 3s / 4s / 4s |
| **UI edit → compile → render → pixel verdict** | **6.5s** | edit one word, `./gradlew :app:verifyRoborazziDebug`; 2 runs: 6.6s / 6.3s |
| Screenshot render alone (no source change) | **0.6s** | `./gradlew :app:recordRoborazziDebug` |

The number that matters is the second-to-last one. An agent that can see the rendered result of a
UI change in 6.5 seconds can iterate; one waiting on an emulator cannot.

What the gates caught, measured on this repo:

| Scenario | Result |
|---|---|
| Changed one word on the error screen | failed `home_error` only — 1 of 7 tests — with `_actual`/`_compare` images |
| Deleted one `@Test` | `agent-check.sh` failed: `@Test count: 7 -> 6` |
| Edited `gradle/libs.versions.toml` | failed: dependency change requires human approval |
| Pasted an API-key-shaped string | failed: possible credential in the diff |

### What is not measured here

Review-round counts, revert rates and defect escapes are in the previous version of this table as
placeholders. They need a real team and months of history; this repo has neither, so it doesn't
claim them. See `docs/measurement.md` for how to instrument them where the data exists.

---

## Quickstart

Requires **JDK 17** and the **Android SDK (API 35)**.

```bash
# The only command an agent needs to know
./gradlew verify

# Screenshot changes are a test failure, not a review argument
./gradlew :app:verifyRoborazziDebug

# Re-record goldens only when the visual change is intended
./gradlew :app:recordRoborazziDebug

# The same guards as CI, locally, before you push
./scripts/agent-check.sh
```

The wrapper is committed. The first run downloads Gradle plus dependencies (~1 GB) into
`~/.gradle`; after that the loop is seconds. If dependency resolution fails, see
`docs/first-run.md` — four versions move often and may need bumping.

---

## Layout

```
AGENTS.md                  ← the contract with the agent. Read this first.
CLAUDE.md                  ← Claude Code entry point (imports AGENTS.md)
app/                       ← application module
  src/test/screenshots/    ← golden images, committed
core/ui/                   ← theme + design system, no business logic
.github/workflows/
  verify.yml               ← build + unit + lint + screenshots
  guards.yml               ← test-integrity, dependency, secret, diff-size gates
scripts/
  agent-check.sh           ← the same guards, locally, pre-push
  benchmark-verify.sh      ← produces the numbers in the table above
docs/
  runbook.md               ← how a human drives the agent
  failure-taxonomy.md      ← the seven ways agents break Android builds
  measurement.md           ← what to measure, and what lies
  first-run.md             ← setup + version bumping
```

---

## What this is not

- Not a prompt library. Prompts are the least interesting part.
- Not a model wrapper. Works with any agent that can run `bash`.
- Not a claim that agents should run unattended. Autonomy should track reversibility — see
  `docs/failure-taxonomy.md`.

## License

MIT — see `LICENSE`.
