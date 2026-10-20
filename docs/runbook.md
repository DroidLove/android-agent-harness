# Runbook: how a human drives the agent

The agent's behaviour is downstream of two things you control: **the task spec** and **the
verification loop**. Everything else is noise.

---

## 1. The task spec

A prompt is a wish. A spec is a contract. Use this shape — it takes 90 seconds and saves a review
cycle:

```md
### Task
<one sentence: what is different after this>

### Context
<files/modules involved; the pattern to follow; a link to a similar existing implementation>

### Acceptance criteria
- [ ] <observable behaviour 1>
- [ ] <observable behaviour 2>
- [ ] `./gradlew verify` passes

### Out of scope
<the things you know it will be tempted to "improve">

### Constraints
<API compatibility, performance budget, no new deps, don't touch module X>
```

The "Out of scope" section is the highest-value line. Unbounded agents refactor.

---

## 2. Decomposition

An agent's success rate collapses past roughly **250 changed lines** or **4 files**. Split
before you delegate:

| Smell | Split into |
|---|---|
| "Add the feature and the tests" | 1) acceptance test that fails 2) implementation 3) cleanup |
| "Refactor X and add Y" | Two tasks. Never both. |
| "Update all screens to use the new component" | One screen first, verify the pattern, then batch |
| "Fix the flaky test and the bug it found" | Bug first. The flaky test is its own task. |

Rule of thumb: if you can't describe the diff in one sentence, it's two tasks.

---

## 3. The loop you actually run

```
spec → agent plans → you approve the plan (30 seconds, catches 80% of bad outcomes)
     → agent implements → ./gradlew :app:testDebugUnitTest (fast)
     → agent opens PR → CI verify + guards
     → you review the diff, not the prose
```

Approve the plan. Skipping that step is the most common way senior engineers waste an hour: the
agent confidently builds the wrong thing at speed.

---

## 4. Reviewing an agent diff

Review the same things you'd review from a human, plus four:

1. **Is anything outside scope touched?** `git diff --stat` first. Always.
2. **Were tests changed?** If yes, read the test diff before the implementation diff.
3. **Is there a new abstraction for one use?** Agents love premature generalisation.
4. **Does it cite an API you don't recognise?** Verify it exists. Hallucinated APIs compile in
   the agent's imagination and nowhere else.

Then the normal questions: correctness, edge cases, error handling, naming, test quality.

What you should *not* spend time on: formatting, import order, comment wording. Automate those
or ignore them.

---

## 5. When to stop the agent

Stop and take over if:

- It has attempted the same fix three times. The model is in a loop; you need a new hypothesis.
- It proposes changing a test to match the implementation. That's the failure mode this repo
  exists to catch.
- It wants to add a dependency. Almost always the wrong answer.
- The diff keeps growing. Scope is unbounded; re-spec it.
- You can't explain what it did. If you can't explain it, you can't own it in production.

---

## 6. Escalation ladder

| Situation | Action |
|---|---|
| Build broken before your change | Stop. Fix the tree or report it. Don't work around it. |
| Test fails and you think the test is wrong | Stop. Discuss. Never let the agent "fix" it. |
| Needs a new dependency | Human decision, `deps-approved` label, supply-chain review. |
| Needs a module boundary change | Design discussion, then a human writes the plan. |
| Touches payments/auth/signing | Human-authored only. Agent may draft; human owns the diff. |

---

## 7. Weekly ritual (15 minutes)

- Count agent-authored PRs vs human PRs.
- For each agent PR: how many review rounds? Any revert?
- Pick the worst agent failure of the week and ask: **which guardrail would have caught it?**
  Add the guardrail. That's how the harness improves — failure by failure, not model upgrade by
  model upgrade.

Record it in `docs/measurement.md`. Number goes in `README.md`.
