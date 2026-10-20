#!/usr/bin/env bash
#
# Produces the numbers for the README results table.
# Do not publish a claim you have not run this for.
#
#   RUNS=3 ./scripts/benchmark-verify.sh
#   MODULE=:core:ui RUNS=5 ./scripts/benchmark-verify.sh
#
set -uo pipefail

RUNS="${RUNS:-3}"
MODULE="${MODULE:-:app}"
LOG="$(mktemp -t harness-bench)"
trap 'rm -f "$LOG"' EXIT

now() { date +%s; }

bench() {
  local label="$1"; shift
  local total=0 i s e
  printf '  %-24s' "$label"
  for i in $(seq 1 "$RUNS"); do
    s=$(now)
    if ! "$@" >"$LOG" 2>&1; then
      printf ' [run %d failed]' "$i"
    fi
    e=$(now)
    printf ' %ss' "$((e - s))"
    total=$((total + e - s))
    # Reset between runs so the next measurement starts from the same state.
    ./gradlew --stop >/dev/null 2>&1 || true
  done
  printf '  → mean %ss\n' "$((total / RUNS))"
}

echo
echo "Harness benchmark — $RUNS run(s) per scenario, module $MODULE"
echo "------------------------------------------------------------"

echo "Cold-ish (no build cache, tasks forced):"
bench "cold verify" ./gradlew verify --no-build-cache --rerun-tasks

echo "Warm (cache enabled, daemon running):"
bench "warm verify" ./gradlew verify

echo "Module-scoped (what an agent should use while iterating):"
bench "scoped unit tests" ./gradlew "$MODULE:testDebugUnitTest"

cat <<'EOF'

------------------------------------------------------------
Paste the means into README.md §Results, with the date and machine
(class of machine matters — say "M2 Pro, 32GB" or "CI runner, 4 vCPU").

If the warm number is above ~60s, the loop is too slow for an agent.
Fix in this order:
  1. Is the configuration cache actually being reused?  (./gradlew verify --configuration-cache)
  2. Is the build cache local-only? Consider a remote cache (Develocity, Gradle Enterprise-compatible).
  3. Are you running the full multi-module build while iterating? Use the module-scoped task.
  4. Is KSP/annotation processing the bottleneck? Check --profile.
EOF
