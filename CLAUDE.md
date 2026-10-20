# Claude Code entry point.
#
# The canonical contract lives in AGENTS.md so that every agent (Claude Code, Codex, Cursor,
# Gemini CLI) reads the same rules. Import it rather than duplicating it — duplicated rules
# drift, and drifting rules are worse than none.

@AGENTS.md

## Claude-specific notes

- Prefer the `Edit` tool over rewriting whole files; large rewrites hide scope creep.
- Run `./gradlew :app:testDebugUnitTest` after each meaningful edit, not once at the end.
- Before opening a PR, run `scripts/agent-check.sh` locally. If it fails, fix it — do not
  open the PR and let CI explain the problem.
- If you are about to change a file under `app/src/test/`, re-read AGENTS.md §4 first.
