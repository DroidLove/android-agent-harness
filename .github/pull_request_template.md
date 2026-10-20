<!--
  The point of this template is evidence, not prose.
  A PR that says "tests pass" is worth less than one that links the run.
-->

## What changed

<!-- One paragraph. What is different for the user or the build after this PR. -->

## Why

<!-- Link the task spec / issue. If there is no spec, say where the requirement came from. -->

## Verification

- [ ] `./gradlew verify` passes — paste the summary line below
- [ ] `./gradlew :app:verifyRoborazziDebug` passes (if UI changed)
- [ ] Screenshot diffs are intentional and described in "What changed"
- [ ] `./scripts/agent-check.sh` passes

```
<paste verify output summary here>
```

## Test changes

<!--
  Any change under src/test or src/androidTest MUST be justified here.
  Deleting or weakening a test is the single most common agent failure mode.
-->

- [ ] No test files modified
- [ ] Tests modified — justification:

## Scope

- [ ] Diff is under ~250 changed lines
- [ ] I did not touch files outside the stated scope
- [ ] No new dependencies

## Agent involvement

- [ ] Human-authored
- [ ] Agent-assisted — the agent's summary is below, unedited
- [ ] Agent-authored — add the literal string `Agent-PR` to the body so CI labels the cohort

<details>
<summary>Agent summary (unedited)</summary>

```
<paste the agent's own report: what it changed, what it verified, what it was unsure about>
```

</details>

## Rollback

<!-- How do we undo this in under five minutes? Feature flag? Revert commit? -->
