package com.harness.app.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.harness.core.ui.theme.HarnessTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual regression without an emulator.
 *
 * Robolectric renders the real Compose UI with native graphics on the JVM, so a UI change is a
 * ~1 second test instead of a 3–5 minute emulator cycle. This is the piece that makes an agent
 * loop viable on Android: without it the agent cannot *see* what it changed.
 *
 * Record:  ./gradlew :app:recordRoborazziDebug   (commit the PNGs)
 * Compare: ./gradlew :app:verifyRoborazziDebug   (fails on any pixel diff)
 *
 * Determinism rules — do not remove:
 *  - fixed device qualifiers, so the viewport never depends on the host
 *  - `darkTheme = false`, so goldens don't flip with the machine's theme
 *  - no network, no clock, no randomness in the composition
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class HomeScreenScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun home_content() {
        capture("home_content") {
            HomeScreen(uiState = HomeUiState.Content(sampleItems()))
        }
    }

    @Test
    fun home_loading() {
        capture("home_loading") {
            HomeScreen(uiState = HomeUiState.Loading)
        }
    }

    @Test
    fun home_error() {
        capture("home_error") {
            HomeScreen(uiState = HomeUiState.Error("Could not reach the server"))
        }
    }

    private fun capture(fileName: String, content: @Composable () -> Unit) {
        compose.setContent {
            HarnessTheme(darkTheme = false) {
                content()
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$fileName.png")
    }
}
