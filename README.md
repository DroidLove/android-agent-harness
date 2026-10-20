# android-agent-harness

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

> Replace this table with your own measurements. Do not publish numbers you haven't measured —
> see `docs/measurement.md`.

| Metric | Before | After | How measured |
|---|---|---|---|
| Warm `verify` duration | _e.g. 11m 40s_ | _e.g. 48s_ | `scripts/benchmark-verify.sh` |
| Cold build | _e.g. 14m_ | _e.g. 6m 10s_ | clean clone + `./gradlew verify` |
| UI change feedback | emulator, _e.g. 3–5 min_ | JVM screenshot, _e.g. <2s_ | `./gradlew :app:recordRoborazziDebug` |
| Agent PRs needing a second review round | _e.g. 62%_ | _e.g. 18%_ | count review rounds per PR |
| Revert rate (agent cohort vs human) | _e.g. 4.1%_ | _e.g. 4.3%_ | `git log --grep="Agent-PR"` |
| Defect escapes per release | — | — | post-release hotfix count |

---

## Quickstart

Requires **JDK 17** and the **Android SDK (API 35)**.

```bash
# 1. Generate the Gradle wrapper (not committed in this scaffold)
gradle wrapper --gradle-version 8.11.1

# 2. Verify everything — this is the only command an agent needs to know
./gradlew verify

# 3. Record golden screenshots (first time only), then commit them
./gradlew :app:recordRoborazziDebug
git add app/src/test/screenshots

# 4. From then on, screenshot changes are a test failure, not a review argument
./gradlew :app:verifyRoborazziDebug
```

If dependency resolution fails on first run, see `docs/first-run.md` — four versions move often
and may need bumping.

---

## Layout

```
AGENTS.md                  ← the contract with the agent. Read this first.
CLAUDE.md                  ← Claude Code entry point (imports AGENTS.md)
app/                       ← application module
core/ui/                   ← theme + design system, no business logic
  src/test/screenshots/    ← golden images, committed
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
