# First run

This scaffold was authored without a JDK or Android SDK available, so **it has not been
compiled**. Everything below is the path to a green build on your machine. Budget 30–45 minutes
for the first pass, mostly download time.

> **Step 0 — initialise git.** The scaffold directory was created without a git repository
> (`git` on this machine requires the Xcode command line tools). Run:
>
> ```bash
> cd android-agent-harness
> git init
> git add -A
> git commit -m "Scaffold: verification-first harness for AI-assisted Android delivery"
> ```
>
> Then create the GitHub repo and push. Do not squash your later work into this commit — the
> history is part of the story.

---

## 1. Prerequisites

- JDK 17 (Android Studio's bundled JBR is fine: `Settings → Build Tools → Gradle → Gradle JDK`)
- Android SDK Platform 35 + Build Tools 35
- `ANDROID_HOME` set, or `local.properties` with `sdk.dir=/Users/<you>/Library/Android/sdk`

```bash
java -version          # must be 17.x
echo $ANDROID_HOME
```

---

## 2. Generate the Gradle wrapper

The wrapper JAR is intentionally not committed here (binaries can't be authored as text). CI
checks for it and fails with a clear message if missing.

```bash
# Either open the project in Android Studio and let it sync (it offers to generate the wrapper),
# or, if you have Gradle installed:
gradle wrapper --gradle-version 8.11.1
```

Then commit `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar`.

---

## 3. First build

```bash
./gradlew verify
```

Expect 3–8 minutes on a cold machine. If it passes, you have the baseline.

---

## 4. Record the golden screenshots

Roborazzi needs a golden image before it can compare. Run record once, review the PNGs with your
eyes, then commit them.

```bash
./gradlew :app:recordRoborazziDebug
open app/src/test/screenshots     # macOS: check all three look right
git add app/src/test/screenshots
git commit -m "Add golden screenshots for HomeScreen"
```

From now on:

```bash
./gradlew :app:verifyRoborazziDebug   # fails on any pixel diff
```

**Review the images before committing.** A golden test only protects you if the golden is correct
— committing a broken render locks the bug in.

---

## 5. Benchmark it

```bash
RUNS=3 ./scripts/benchmark-verify.sh
```

Paste the means into `README.md` §Results. Note your machine. A number without a machine is not
a measurement.

---

## 6. If resolution fails

Four versions move fastest. If Gradle can't resolve, bump in this order and re-run:

| Version | File | Notes |
|---|---|---|
| `agp` | `gradle/libs.versions.toml` | Must be compatible with your Gradle version |
| `kotlin` | same | Compose plugin tracks Kotlin version exactly |
| `composeBom` | same | Controls all Compose artifact versions |
| `roborazzi` | same | Screenshot library; plugin and library versions must match |

Check the AGP ↔ Gradle compatibility table before bumping AGP. Android Studio's "AGP Upgrade
Assistant" is the safest route.

**Do not switch to dynamic versions** (`latest.release`, `+`). Reproducibility is the point of
this repo; a silent upgrade is exactly the change the guards exist to prevent.

---

## 7. Known pitfalls

| Symptom | Cause | Fix |
|---|---|---|
| Robolectric downloads `android-all` jars on first test run | Normal; it fetches an Android runtime jar | Allowed network on first run; cached afterwards |
| `No such property: roborazzi` | Plugin not applied to the module running screenshots | Apply `alias(libs.plugins.roborazzi)` in `app/build.gradle.kts` |
| Screenshots differ on CI but not locally | Font rendering / DPI / theme differences | Qualifiers are pinned in `@Config`; force `darkTheme = false` (already done) |
| `verifyRoborazziDebug` fails on a fresh clone | No goldens committed | Run record, review, commit |
| Configuration cache warnings | Some plugins aren't fully compatible | `problems=warn` is set deliberately; fix warnings when convenient |
| `kotlinOptions` deprecation warning | Kotlin 2.x prefers `compilerOptions` | Harmless; migrate when you bump Kotlin |

---

## 8. Before you publish this repo

- [ ] `./gradlew verify` green, and again after a clean clone
- [ ] Goldens committed and visually reviewed
- [ ] `README.md` §Results filled with **your** measurements, machine noted
- [ ] Repository renamed to something specific (`android-agent-harness` is fine; `my-android-app` is not)
- [ ] Description + topics: `android`, `ai-agents`, `compose`, `roborazzi`, `developer-tools`, `ci`
- [ ] CI badge added
- [ ] `LICENSE` copyright line updated
- [ ] No secrets, no employer code, no client names
- [ ] Commit history is honest — no fake "initial commit" then backdated work

**Do not publish numbers you have not measured.** The whole credibility of this repository is
that it reports what actually happened, including what didn't work. One invented metric destroys
that, and interviewers do ask how you measured.

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
