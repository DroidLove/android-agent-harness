#!/usr/bin/env bash
#
# Local mirror of .github/workflows/guards.yml.
# Run before pushing. If this passes and `verify` passes, CI should pass.
#
#   ./scripts/agent-check.sh              # checks + full verify
#   SKIP_VERIFY=1 ./scripts/agent-check.sh  # checks only (fast, for WIP)
#   BASE=origin/develop ./scripts/agent-check.sh
#
set -uo pipefail

BASE="${BASE:-}"
SKIP_VERIFY="${SKIP_VERIFY:-0}"
FAIL=0

bold() { printf '\n\033[1m%s\033[0m\n' "$1"; }
pass() { printf '  \033[32m✓\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m✗\033[0m %s\n' "$1"; FAIL=1; }
info() { printf '  · %s\n' "$1"; }

if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Not a git repository." >&2
  exit 2
fi

# Resolve a base ref: explicit BASE > upstream > local main/master > HEAD.
if [ -z "$BASE" ]; then
  for ref in origin/main origin/master main master; do
    if git rev-parse --verify -q "$ref" >/dev/null 2>&1; then BASE="$ref"; break; fi
  done
fi
if [ -z "$BASE" ]; then
  BASE="HEAD"
  info "No remote or main branch found; checking uncommitted changes only."
fi

MERGE_BASE="$(git merge-base "$BASE" HEAD 2>/dev/null || echo "$BASE")"
if ! git rev-parse --verify -q "$MERGE_BASE" >/dev/null 2>&1; then
  echo "Cannot resolve base '$BASE'. Set BASE=<ref>." >&2
  exit 2
fi

CHANGED="$(git diff --name-only "$MERGE_BASE" || true)"
if [ -z "$CHANGED" ]; then
  echo "No changes against $BASE. Nothing to check."
  exit 0
fi

bold "Changed files"
echo "$CHANGED" | sed 's/^/  /'

bold "Guard: dependencies"
DEPS="$(echo "$CHANGED" | grep -E '(^|/)(build\.gradle(\.kts)?|libs\.versions\.toml|settings\.gradle(\.kts)?)$' || true)"
if [ -n "$DEPS" ]; then
  echo "$DEPS" | sed 's/^/  /'
  fail "Build/dependency files changed — remove the change or get explicit human approval (CI label: deps-approved)"
else
  pass "No dependency or build-config changes"
fi

bold "Guard: CI and guard scripts"
CIHITS="$(echo "$CHANGED" | grep -E '^(\.github/|scripts/)' || true)"
if [ -n "$CIHITS" ]; then
  echo "$CIHITS" | sed 's/^/  /'
  fail "CI/guard files changed — an agent must not edit the gate that judges it"
else
  pass "No CI or guard changes"
fi

bold "Guard: test integrity"
DELETED="$(echo "$CHANGED" | grep -E '/src/(test|androidTest)/.*\.kt$' | while read -r f; do [ -f "$f" ] || echo "$f"; done || true)"
if [ -n "$DELETED" ]; then
  echo "$DELETED" | sed 's/^/  /'
  fail "Test files deleted"
else
  pass "No test files deleted"
fi

BASE_COUNT="$(git grep -h '@Test' "$MERGE_BASE" -- '*.kt' 2>/dev/null | wc -l | tr -d ' ')"
HEAD_COUNT="$(git grep -h '@Test' HEAD -- '*.kt' 2>/dev/null | wc -l | tr -d ' ')"
info "@Test count: $BASE_COUNT -> $HEAD_COUNT"
if [ "$HEAD_COUNT" -lt "$BASE_COUNT" ]; then
  fail "Test count decreased — adding a test elsewhere does not license deleting one"
else
  pass "Test count did not decrease"
fi

NEW_IGNORES="$(git diff "$MERGE_BASE" -- '*.kt' | grep -E '^\+' | grep -Ec '@Ignore|@Disabled' || true)"
if [ "${NEW_IGNORES:-0}" -gt 0 ]; then
  fail "$NEW_IGNORES new @Ignore/@Disabled annotation(s) added"
else
  pass "No new @Ignore/@Disabled"
fi

bold "Guard: secrets"
SECRETS="$(git diff "$MERGE_BASE" -U0 \
  | grep -E '^\+' | grep -Ev '^\+\+\+' \
  | grep -E 'AIza[0-9A-Za-z_-]{35}|sk-[A-Za-z0-9]{20,}|ghp_[A-Za-z0-9]{36}|-----BEGIN [A-Z ]*PRIVATE KEY-----|xox[baprs]-[A-Za-z0-9-]{10,}' || true)"
if [ -n "$SECRETS" ]; then
  fail "Possible credential in the diff — remove it and rotate the key"
else
  pass "No credential patterns in added lines"
fi

bold "Guard: diff size"
LINES="$(git diff "$MERGE_BASE" | grep -Ec '^[+-][^+-]' || true)"
info "$LINES changed lines (budget 500, soft)"
if [ "${LINES:-0}" -gt 500 ]; then
  fail "Diff exceeds 500 lines — split the task or justify it to a human"
else
  pass "Within budget"
fi

if [ "$SKIP_VERIFY" = "1" ]; then
  bold "Skipping ./gradlew verify (SKIP_VERIFY=1)"
else
  bold "Running ./gradlew verify"
  if ./gradlew verify; then
    pass "verify passed"
  else
    fail "verify failed"
  fi
fi

echo
if [ "$FAIL" -eq 0 ]; then
  printf '\033[32mAll checks passed.\033[0m\n'
  exit 0
fi
printf '\033[31mChecks failed — do not open a PR until these are resolved.\033[0m\n'
exit 1
