package com.harness.app.home

/**
 * Screen state. Immutable, exhaustive, and the only thing the UI is allowed to read.
 *
 * Keeping this a sealed interface means the compiler — not a code review — tells you when a
 * state is unhandled. That is what makes agent edits safe: adding a state breaks the build
 * until every branch is updated.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(val items: List<HomeItem>) : HomeUiState
}

data class HomeItem(
    val id: String,
    val title: String,
    val detail: String,
)

/** Events the UI can send. Deliberately small and closed. */
sealed interface HomeEvent {
    data object Retry : HomeEvent
    data class Loaded(val items: List<HomeItem>) : HomeEvent
    data class Failed(val message: String) : HomeEvent
}

/**
 * Pure state transition: no I/O, no coroutines, no Android types.
 *
 * That is the point. The rules that matter most — "a failed refresh must not blank the screen
 * the user is reading" — are testable on the JVM in milliseconds, so they are tested on every
 * commit rather than discovered in production.
 */
object HomeReducer {

    fun reduce(state: HomeUiState, event: HomeEvent): HomeUiState = when (event) {
        is HomeEvent.Retry -> HomeUiState.Loading
        is HomeEvent.Loaded -> HomeUiState.Content(event.items)
        is HomeEvent.Failed ->
            // A background refresh failure must not destroy content already on screen.
            if (state is HomeUiState.Content) state else HomeUiState.Error(event.message)
    }
}
