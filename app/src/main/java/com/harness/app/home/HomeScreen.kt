package com.harness.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.harness.core.ui.theme.HarnessTheme

/**
 * Pure function of state. No business logic, no side effects, no Android framework calls —
 * which is precisely what lets it be rendered and screenshotted on the JVM.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is HomeUiState.Loading -> LoadingState()
                is HomeUiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
                is HomeUiState.Content -> ContentState(items = uiState.items)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    CircularProgressIndicator()
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun ContentState(items: List<HomeItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = items, key = { it.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = item.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun HomeScreenContentPreview() {
    HarnessTheme {
        HomeScreen(uiState = HomeUiState.Content(sampleItems()))
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun HomeScreenErrorPreview() {
    HarnessTheme {
        HomeScreen(uiState = HomeUiState.Error("Could not reach the server"))
    }
}

internal fun sampleItems(): List<HomeItem> = listOf(
    HomeItem("1", "Verification loop", "Warm verify under 60 seconds"),
    HomeItem("2", "Screenshot tests", "UI regressions caught on the JVM"),
    HomeItem("3", "Test integrity", "Weakened tests fail CI"),
)
