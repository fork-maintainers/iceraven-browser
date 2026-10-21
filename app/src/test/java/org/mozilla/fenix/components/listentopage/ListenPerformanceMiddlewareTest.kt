/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.listentopage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mozilla.components.feature.listentopage.ChunkState
import mozilla.components.feature.listentopage.ListenAction
import mozilla.components.feature.listentopage.ListenState
import mozilla.components.feature.listentopage.ListenStore
import mozilla.components.feature.listentopage.PlaybackPhase
import mozilla.components.feature.listentopage.PlaybackState
import mozilla.components.feature.listentopage.listenReducer
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.ListenToPage
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ListenPerformanceMiddlewareTest {

    @get:Rule val gleanTestRule = FenixGleanTestRule(testContext)

    private var now = 0L
    private val profiler = FakeProfiler(clock = { now })

    @Test
    fun `WHEN audio starts after listening is requested THEN a time to first audio marker is recorded`() {
        val store = createStore()

        store.dispatch(LISTEN_REQUESTED)
        now += 250
        store.dispatch(ListenAction.Playback.PlaybackStarted(ChunkState(index = 0), positionMs = 0))

        assertEquals(
            listOf(Marker(TIME_TO_FIRST_AUDIO, startTime = 0.0, endTime = 250.0, text = null)),
            profiler.markers,
        )
        assertEquals(1L, ListenToPage.timeToFirstAudio.testGetValue()?.count)
    }

    @Test
    fun `WHEN the player reports playing THEN time to first audio ends`() {
        val store = createStore()

        store.dispatch(LISTEN_REQUESTED)
        store.dispatch(ListenAction.Playback.StateChangeObserved(PlaybackState(phase = PlaybackPhase.Playing)))

        assertEquals(TIME_TO_FIRST_AUDIO, profiler.markers.single().name)
    }

    @Test
    fun `WHEN listening is requested again before audio starts THEN the time is measured from the new request`() {
        val store = createStore()

        store.dispatch(LISTEN_REQUESTED)
        now += 100
        store.dispatch(LISTEN_REQUESTED)
        now += 100
        store.dispatch(ListenAction.Playback.PlaybackStarted(ChunkState(index = 0), positionMs = 0))

        assertEquals(100.0, profiler.markers.single().startTime)
    }

    @Test
    fun `WHEN later chunks start playing THEN time to first audio is only recorded once`() {
        val store = createStore()

        store.dispatch(LISTEN_REQUESTED)
        store.dispatch(ListenAction.Playback.PlaybackStarted(ChunkState(index = 0), positionMs = 0))
        store.dispatch(ListenAction.Playback.PlaybackStarted(ChunkState(index = 1), positionMs = 0))

        assertEquals(1, profiler.markers.size)
        assertEquals(1L, ListenToPage.timeToFirstAudio.testGetValue()?.count)
    }

    @Test
    fun `WHEN play is pressed THEN time to first audio is not started`() {
        val store = createStore()

        store.dispatch(ListenAction.Controls.PlayPauseClicked)
        store.dispatch(ListenAction.Playback.PlaybackStarted(ChunkState(index = 0), positionMs = 0))

        assertTrue(profiler.markers.isEmpty())
        assertNull(ListenToPage.timeToFirstAudio.testGetValue())
    }

    @Test
    fun `WHEN the session fails before audio starts THEN a failed marker is recorded without telemetry`() {
        val store = createStore()

        store.dispatch(LISTEN_REQUESTED)
        now += 300
        store.dispatch(ListenAction.Synthesis.SynthesisFailed)

        assertEquals(
            listOf(
                Marker(
                    name = "$TIME_TO_FIRST_AUDIO.Failed",
                    startTime = 0.0,
                    endTime = 300.0,
                    text = "failed=SynthesisFailed",
                )
            ),
            profiler.markers,
        )
        assertNull(ListenToPage.timeToFirstAudio.testGetValue())
    }

    @Test
    fun `WHEN the session stops before audio starts THEN nothing is recorded`() {
        val store = createStore()

        store.dispatch(LISTEN_REQUESTED)
        store.dispatch(ListenAction.Session.StopRequested())
        store.dispatch(ListenAction.Playback.PlaybackStarted(ChunkState(index = 0), positionMs = 0))

        assertTrue(profiler.markers.isEmpty())
        assertNull(ListenToPage.timeToFirstAudio.testGetValue())
    }

    @Test
    fun `WHEN the profiler is not running THEN telemetry is still recorded`() {
        profiler.active = false
        val store = createStore()

        store.dispatch(LISTEN_REQUESTED)
        store.dispatch(ListenAction.Playback.PlaybackStarted(ChunkState(index = 0), positionMs = 0))

        assertTrue(profiler.markers.isEmpty())
        assertEquals(1L, ListenToPage.timeToFirstAudio.testGetValue()?.count)
    }

    private fun createStore(initialState: ListenState = ListenState()) =
        ListenStore(
            initialState = initialState,
            reducer = ::listenReducer,
            middleware =
                listOf(
                    ListenPerformanceMiddleware(
                        profiler = profiler,
                        scope = CoroutineScope(UnconfinedTestDispatcher()),
                        elapsedMs = { now },
                    )
                ),
        )

    private companion object {
        const val TIME_TO_FIRST_AUDIO = "ListenToPage.TimeToFirstAudio"
        val LISTEN_REQUESTED = ListenAction.Session.ListenRequested(tabId = "tab", url = "https://example.com")
    }
}
