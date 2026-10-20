# First run

This repo is built, tested and benchmarked. It is not a scaffold that "should" work — the numbers
in `README.md` §Results came from the commands below on the machine named there.

What follows is the path from a clean clone to a green build, plus an honest record of what broke
the first time, because that record is more useful to you than a tidy list of steps.

---

## 1. Prerequisites

- **JDK 17** (Android Studio's bundled JBR is fine: `Settings → Build Tools → Gradle → Gradle JDK`)
- **Android SDK Platform 35** + **Build Tools 35**
- `ANDROID_HOME` set, or `local.properties` containing `sdk.dir=/Users/<you>/Library/Android/sdk`

```bash
java -version          # must be 17.x
echo $ANDROID_HOME
```

The Gradle wrapper (8.11.1) is committed, so you do not need Gradle installed. First run downloads
the distribution plus dependencies — about 1 GB, once, into `~/.gradle`.

---

## 2. First build

```bash
./gradlew verify
```

`verify` is compile + unit tests + lint + screenshot comparison. Nothing else needs to be known.
On a cold machine expect several minutes of downloading and then a build in the low minutes; see
§Results in the README for the measured figure on an M2.

If it fails on dependency resolution, jump to §5.

---

## 3. Screenshot goldens

Three goldens are committed in `app/src/test/screenshots/` — `home_loading`, `home_content`,
`home_error`, each 822×1782 (411×891 dp at xhdpi). They are the fixture every UI change is judged
against.

Re-record only when the visual change is intended:

```bash
./gradlew :app:recordRoborazziDebug
open app/src/test/screenshots      # look at them. all three.
git add app/src/test/screenshots
```

A golden test protects you only if the golden is correct. Committing a broken render locks the bug
in and makes the next real regression invisible.

---

## 4. What actually broke on first run

Recorded because "it worked first time" is not a thing, and because these are the failure modes
the repo exists to make visible.

| What happened | Why | Fix |
|---|---|---|
| `Unable to resolve activity for Intent ... androidx.activity.ComponentActivity` on all 3 screenshot tests | Compose tests need a host activity registered in the **debug** manifest. The `ui-test-manifest` artifact does that; without it Robolectric has nothing to launch. | `debugImplementation("androidx.compose.ui:ui-test-manifest")` |
| Local `agent-check.sh` reported `@Test count: 7 -> 7` after a test was deleted, and passed | The guard counted `git grep '@Test' HEAD` on both sides — comparing a commit to itself. Correct in CI (HEAD *is* the PR), wrong locally. | Count `git ls-files '*.kt'` from the working tree |
| Build Tools 34 installed automatically | AGP 8.7.3 defaults to 34 even with `compileSdk 35` | Harmless; pin `buildToolsVersion` if you care about download size |

The second one is the interesting one. A guard that passes when it should fail is worse than no
guard, because it converts "we don't check" into "we checked". It was found by running the guard
against a deliberately weakened suite, which is the only way to know a guard works.

---

## 5. If resolution fails

Four versions move fastest. If Gradle can't resolve, bump in this order and re-run:

| Version | File | Notes |
|---|---|---|
| `agp` | `gradle/libs.versions.toml` | Must be compatible with your Gradle version |
| `kotlin` | same | Compose plugin tracks Kotlin version exactly |
| `composeBom` | same | Controls all Compose artifact versions |
| `roborazzi` | same | Plugin and library versions must match |

Check the AGP ↔ Gradle compatibility table before bumping AGP. Android Studio's "AGP Upgrade
Assistant" is the safest route.

**Do not switch to dynamic versions** (`latest.release`, `+`). Reproducibility is the point of
this repo; a silent upgrade is exactly the change the guards exist to prevent.

---

## 6. Known pitfalls

| Symptom | Cause | Fix |
|---|---|---|
| Robolectric downloads `android-all` jars on first test run | Normal; it fetches an Android runtime jar | Allowed network on first run; cached afterwards |
| `No such property: roborazzi` | Plugin not applied to the module running screenshots | Apply `alias(libs.plugins.roborazzi)` in `app/build.gradle.kts` |
| Screenshots differ on CI but not locally | Font rendering / DPI / theme differences | Qualifiers are pinned in `@Config`; `darkTheme = false` is forced |
| `verifyRoborazziDebug` fails on a fresh clone | No goldens committed | Run record, review, commit |
| Configuration cache warnings | Some plugins aren't fully compatible | `problems=warn` is set deliberately; fix warnings when convenient |
| `kotlinOptions` deprecation warning | Kotlin 2.x prefers `compilerOptions` | Harmless; migrate when you bump Kotlin |

---

## 7. Benchmark it yourself

```bash
RUNS=3 ./scripts/benchmark-verify.sh
```

Paste the means into `README.md` §Results, with the date and the machine. A number without a
machine is not a measurement, and a before/after table with no methodology is a marketing claim.

If the warm number is above ~60s, the loop is too slow for an agent. Fix in this order:

1. Is the configuration cache actually being reused? (`./gradlew verify --configuration-cache`)
2. Is the build cache local-only? Consider a remote cache.
3. Are you running the full multi-module build while iterating? Use the module-scoped task.
4. Is KSP/annotation processing the bottleneck? Check `--profile`.

---

## 8. Before you publish

- [x] `./gradlew verify` green
- [x] Goldens committed and visually reviewed
- [x] `README.md` §Results filled with measured values, machine noted
- [ ] Create the GitHub repository, push, add description + topics:
      `android`, `ai-agents`, `compose`, `roborazzi`, `developer-tools`, `ci`
- [ ] CI badge added to the README
- [ ] `LICENSE` copyright line updated
- [ ] No secrets, no employer code, no client names
- [ ] Commit history is honest — no backdated work

**Do not publish numbers you have not measured.** The whole credibility of this repository is that
it reports what actually happened, including what didn't work. One invented metric destroys that,
and interviewers do ask how you measured.

---

## 9. Where to take it next

In rough order of impact:

1. **Remote build cache.** Local cache gets you most of the way; a remote cache makes CI and
   agent runners share it. Biggest remaining win on a large repo.
2. **Gradle Managed Devices** for a real (non-Robolectric) smoke test on the API levels you
   support — the rung above screenshot tests.
3. **Mutation testing** (`pitest` / `gradle-mutation-testing`) nightly on changed files. The
   only real defence against subtly weakened assertions.
4. **Baseline Profiles + Macrobenchmark** so performance regressions fail a build instead of
   being noticed by a user.
5. **MCP server** exposing `verify`, screenshot diff and release status as tools, so the agent
   calls structured tools rather than parsing bash output.
