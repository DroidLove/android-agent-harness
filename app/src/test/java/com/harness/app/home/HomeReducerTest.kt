package com.harness.app.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * These run on the JVM in milliseconds — no Robolectric, no device. That is why they can be
 * the first thing an agent runs after every edit.
 */
class HomeReducerTest {

    private val items = listOf(HomeItem(id = "1", title = "One", detail = "First"))

    @Test
    fun `loaded produces content`() {
        val next = HomeReducer.reduce(HomeUiState.Loading, HomeEvent.Loaded(items))

        assertEquals(HomeUiState.Content(items), next)
    }

    @Test
    fun `retry returns to loading`() {
        val next = HomeReducer.reduce(HomeUiState.Error("boom"), HomeEvent.Retry)

        assertEquals(HomeUiState.Loading, next)
    }

    @Test
    fun `refresh failure keeps existing content on screen`() {
        val content = HomeUiState.Content(items)

        val next = HomeReducer.reduce(content, HomeEvent.Failed("timeout"))

        assertSame(content, next)
    }

    @Test
    fun `failure with nothing on screen shows the error`() {
        val next = HomeReducer.reduce(HomeUiState.Loading, HomeEvent.Failed("offline"))

        assertEquals(HomeUiState.Error("offline"), next)
    }
}
