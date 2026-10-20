package com.harness.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.harness.app.home.HomeEvent
import com.harness.app.home.HomeReducer
import com.harness.app.home.HomeScreen
import com.harness.app.home.HomeUiState
import com.harness.app.home.sampleItems
import com.harness.core.ui.theme.HarnessTheme

/**
 * Thin shell. The Activity owns one piece of state and delegates every transition to the
 * reducer, so behaviour lives in a file that tests can reach without an emulator.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HarnessTheme {
                var uiState by remember { mutableStateOf<HomeUiState>(HomeUiState.Loading) }

                HomeScreen(
                    uiState = uiState,
                    onRetry = {
                        uiState = HomeReducer.reduce(uiState, HomeEvent.Retry)
                        // Stands in for the repository call. Replace with a real data source;
                        // keep the reducer pure.
                        uiState = HomeReducer.reduce(uiState, HomeEvent.Loaded(sampleItems()))
                    },
                )
            }
        }
    }
}
