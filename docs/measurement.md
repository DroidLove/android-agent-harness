# Measurement

If you publish a productivity claim without a baseline, you have published marketing. This file
is how to avoid that.

---

## 1. Baseline first

Before changing anything, record one week of the current state. You cannot retrofit a baseline,
and interviewers — and good managers — will ask how you know.

| Metric | Where it comes from | Baseline | After |
|---|---|---|---|
| Release cadence (releases/month) | release tags, Play Console | | |
| Change failure rate | failed releases ÷ total releases | | |
| Rollbacks / hotfixes per month | incident log, git history | | |
| PR cycle time (median, open → merge) | GitHub/GitLab API | | |
| Review turnaround (median, ready → first review) | PR timestamps | | |
| CI duration (p50/p90) | CI logs | | |
| Warm `verify` duration | `scripts/benchmark-verify.sh` | | |
| Crash-free users % | Play Vitals / Crashlytics | | |
| ANR rate | Play Vitals | | |
| Defect escapes (bugs found post-release) | bug tracker, release notes | | |

---

## 2. Cohort labelling (the part everyone skips)

To compare agent output with human output you need to be able to split the data later. Two
mechanisms, both cheap:

1. **Label the PR.** The `guards.yml` workflow adds `agent-authored` when the PR body contains
   `Agent-PR`.
2. **Annotate the commit.** Ask the agent to append a trailer:

```
Agent-PR: true
Agent-Model: <model>
Agent-Task: <task id or one-line description>
```

Then:

```bash
# per-cohort revert rate
git log --grep="Agent-PR" --oneline | wc -l
git log --grep="Revert" --grep="Agent-PR" --all-match --oneline | wc -l
```

Do this from day one. Retrofitting provenance is impossible, and without it the only claim you
can make is "it feels faster."

---

## 3. Metrics that move fast and metrics that matter

| | Leading (use in the pilot) | Lagging (use for the story) |
|---|---|---|
| What | task cycle time, review rounds, diff size, CI duration | change failure rate, revert rate, defect escapes, crash-free %, cadence |
| Why | fast feedback, lets you adjust the harness | what leadership and interviewers care about |
| Trap | improving while shipping worse code | too slow to guide a 3-week pilot |

Both, in that order. A pilot that only reports lagging metrics can't tell you *why* anything
changed. A programme that only reports leading metrics eventually ships worse code and calls it
faster.

---

## 4. Vanity metrics — call these out when you see them

| Metric | Why it lies |
|---|---|
| "% of code written by AI" | Gameable, and rewards verbosity. A 300-line solution to a 20-line problem scores better. |
| Lines of AI-generated code | Rewards volume. Volume is the problem, not the goal. |
| Tokens consumed | A cost line, not an outcome. |
| Number of agents running | Infrastructure theatre. |
| Developer satisfaction alone | Necessary, not sufficient. Pair with a system metric. |

If someone above you asks for "% AI-written," offer cycle time, revert rate and defect escapes
instead. Explain that the first metric will go up whether or not anything got better.

---

## 5. Confounders to declare

Honest measurement means naming what else changed:

- Team size or seniority shifts during the window.
- A dependency or Compose upgrade landed mid-pilot.
- Seasonal traffic (for a consumer app, festive peaks change both load and release pressure).
- You personally reviewed more carefully because it was a pilot — the Hawthorne effect.

Write these down in the write-up. Naming a confounder increases credibility; hiding one destroys
it when someone spots it later.

---

## 6. The sentence to be able to say

> "Median PR cycle time for comparable tasks went from 2.1 days to 1.3, revert rate stayed flat
> at 4%, and the screenshot suite caught 11 UI regressions that would previously have reached
> staged rollout. Sample was 84 agent-authored PRs over 9 weeks; two confounders were a Compose
> upgrade in week 3 and a new hire in week 6."

That's a claim you can defend in an interview, because every number has a source and a caveat.
