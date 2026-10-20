// Root build file. Intentionally thin: no logic lives here.
//
// The two tasks below are the public interface of this repo. An agent should never need to
// memorise a longer command list than this.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
}

/**
 * The gate. Every agent task, every local pre-push, and CI all use this one command.
 *
 * Deliberately excludes screenshot comparison: goldens have to exist before you can compare
 * against them, and on a fresh clone they don't. Use `verifyAll` once goldens are committed.
 */
tasks.register("verify") {
    group = "verification"
    description = "Compile + unit tests + lint. The single gate for agents and CI."
    dependsOn(
        ":core:ui:assembleDebug",
        ":app:assembleDebug",
        ":app:testDebugUnitTest",
        ":app:lintDebug",
    )
}

/**
 * Full gate including visual regression. Requires committed goldens in
 * app/src/test/screenshots/. CI switches to this automatically once goldens exist.
 */
tasks.register("verifyAll") {
    group = "verification"
    description = "verify + screenshot comparison against committed goldens."
    dependsOn("verify", ":app:verifyRoborazziDebug")
}
