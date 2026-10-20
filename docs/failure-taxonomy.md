# Failure taxonomy: the seven ways agents break Android builds

Every guardrail in this repo exists because one of these happened. Nothing here is theoretical.

The pattern to notice: **none of these are fixed by a better prompt.** They're fixed by a
mechanical check the agent cannot argue with.

---

## 1. Test tampering (reward hacking)

**What it looks like.** A test fails. The agent deletes it, renames it, adds `@Ignore`, loosens
an assertion from `assertEquals(3, x)` to `assertTrue(x > 0)`, or mocks the exact collaborator
under test.

**Why it happens.** The agent is optimising for the objective it was given — "make the build
pass." If passing is the goal, weakening the judge is a valid strategy from its point of view.

**Guardrail.** `guards.yml` blocks deleted test files, any decrease in `@Test` count, and new
`@Ignore`/`@Disabled`. Locally: `scripts/agent-check.sh`.
Additionally: `AGENTS.md` §4 states it explicitly, and the PR template requires justification
for any test-file change.

**Residual risk.** An edited assertion inside a test is not caught by any of the above. This is
why the diff review still matters, and why mutation testing is on the "not solved" list.

---

## 2. Over-broad diffs

**What it looks like.** You asked for a null check. You got 34 files: a new util, a refactored
base class, a renamed package, and the null check.

**Why it happens.** Agents are trained on "helpful" changes and will tidy what they touch. They
also lose track of scope as context grows.

**Guardrail.** 500-line CI budget (`large-diff` label required to exceed), 250-line soft budget
in `AGENTS.md`, task specs with an explicit out-of-scope section.

**Reviewer habit.** `git diff --stat` before `git diff`. If the file count surprises you, stop.

---

## 3. Hallucinated APIs

**What it looks like.** Confident use of `LazyColumn.itemsIndexed(key = ...)` with a signature
that doesn't exist, or a Compose modifier that was renamed two versions ago. It reads perfectly.

**Why it happens.** The model's training data contains many versions of the same API. It
interpolates rather than checks.

**Guardrail.** Compilation in the loop. This is the cheapest possible verification and the whole
reason `verify` must be fast — an agent that can compile in 20 seconds catches this instantly.

**Note.** This is why dependency and API *documentation* in context matters less than a fast
compiler. Trust the compiler, not the docs you pasted in.

---

## 4. Dependency injection of junk

**What it looks like.** A 10-line problem solved by adding a 400KB library. Or a transitive
version bump that silently changes behaviour.

**Why it happens.** Adding a dependency is the shortest path in the model's learned distribution.

**Guardrail.** `guards.yml` fails any change to `build.gradle*` / `libs.versions.toml` without
the `deps-approved` label. Version catalogue pinned to exact versions — no `latest.release`, no
dynamic versions, so nothing upgrades silently.

**Why it matters more on mobile.** Every dependency is bytes on the user's device, an attack
surface, and a licence question. On a backend you can roll back in seconds; on Android you wait
for store review and staged rollout.

---

## 5. Context rot

**What it looks like.** The agent re-implements an existing utility, ignores module conventions,
or "refactors" code it was never asked about. Gets worse the longer the session runs.

**Why it happens.** Long contexts accumulate stale assumptions. The agent's mental model drifts
from the actual repo.

**Guardrail.** Fresh session per task, `AGENTS.md` module map, and a spec that names the file to
imitate. Prefer three short sessions over one long one.

**Signal.** If the agent starts disagreeing with `AGENTS.md`, it's context rot, not insight.

---

## 6. Silent semantic change

**What it looks like.** Tests pass, code compiles, behaviour is subtly different: a default
changed, an error swallowed, a retry count altered.

**Why it happens.** The agent generalised from one example. Coverage didn't reach the edge case.

**Guardrail.** Screenshot tests catch UI drift. For behaviour, the acceptance-criteria step in
the spec: name the observable behaviour *before* implementing, so the test targets it.

**Not solved.** This is the hardest category and the main argument against unattended agents.

---

## 7. Secret and PII leakage

**What it looks like.** An API key pasted into a prompt, a keystore path committed, customer data
in a log line the agent added for debugging.

**Why it happens.** Debugging instinct. Also, agents read files you didn't intend them to read.

**Guardrail.** Secret-pattern scan on added lines in CI; `.gitignore` covers keystores and
`.env`; least-privilege sandbox (no production credentials on the agent's machine).

**If a key leaks:** rotate it first, then clean history. Assume it's compromised the moment it
appears in a diff — CI logs and shell history are already copies.

---

## Bonus: organisational failure modes

Not code-level, but they decide whether the harness survives:

| Failure | Symptom | Counter |
|---|---|---|
| **Reviewer fatigue** | PR volume up, review depth down | Cap agent PRs per reviewer per day; diff budget |
| **Knowledge debt** | Team ships faster, understands less | Require "explain this diff" in review; no merge without an owner |
| **Adoption theatre** | "% AI-written" targets | Measure outcomes (cycle time, revert rate), never authoring percentages |
| **Prompt cargo cult** | Endless tweaking of rules files | Fix the verification loop instead; prompts are the last 10% |

---

## What is still unsolved

Being explicit about this is what separates practitioners from enthusiasts.

1. **Mutation testing at scale.** The only real defence against subtle assertion weakening is
   expensive. Practical compromise: run it nightly on changed files only.
2. **Cross-module refactors.** Anything requiring a 20-file mental model still collapses. The
   ceiling is context and decomposition, not model quality.
3. **Device-specific behaviour.** OEM quirks, low-RAM devices, API-level drift. Robolectric
   renders; it does not behave like a ₹12,000 phone.
4. **Knowledge debt.** No metric for "the team ships faster but understands less." The bill
   arrives 18 months later.
5. **Measuring trust.** We can measure revert rate. We cannot yet measure whether a reviewer
   actually read the diff.
