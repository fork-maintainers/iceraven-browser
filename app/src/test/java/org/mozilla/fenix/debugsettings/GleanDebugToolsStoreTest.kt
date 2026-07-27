/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings

import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.ClipboardHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.debugsettings.gleandebugtools.GleanDebugToolsAction
import org.mozilla.fenix.debugsettings.gleandebugtools.GleanDebugToolsMiddleware
import org.mozilla.fenix.debugsettings.gleandebugtools.GleanDebugToolsState
import org.mozilla.fenix.debugsettings.gleandebugtools.GleanDebugToolsStorage
import org.mozilla.fenix.debugsettings.gleandebugtools.GleanDebugToolsStore
import org.mozilla.fenix.debugsettings.gleandebugtools.PING_PREVIEW_URL

@RunWith(AndroidJUnit4::class)
class GleanDebugToolsStoreTest {

    private lateinit var gleanDebugToolsStorage: FakeGleanDebugToolsStorage
    private lateinit var clipboardHandler: ClipboardHandler

    @Before
    fun setup() {
        gleanDebugToolsStorage = FakeGleanDebugToolsStorage()
        clipboardHandler = ClipboardHandler(testContext)
    }

    @Test
    fun `WHEN there is no debug view tag THEN the debug view tag related buttons should be disabled`() {
        val initialState = initializeGleanDebugToolsState(
            debugViewTag = "",
        )
        assertFalse(initialState.isDebugTagButtonEnabled)
    }

    @Test
    fun `WHEN the debug view tag length is larger than the limit THEN the debug view tag related buttons should be disabled`() {
        val initialState = initializeGleanDebugToolsState(
            debugViewTag = "123456789123456789123",
        )
        assertFalse(initialState.isDebugTagButtonEnabled)
    }

    @Test
    fun `WHEN the the debug view tag length is smaller than the limit and it is not empty THEN the debug view tag related buttons should be enabled`() {
        val initialState = initializeGleanDebugToolsState(
            debugViewTag = "test",
        )
        assertTrue(initialState.isDebugTagButtonEnabled)
    }

    @Test
    fun `GIVEN the log pings to console preference is off WHEN said preference is toggled THEN the preference should be enabled`() {
        gleanDebugToolsStorage = FakeGleanDebugToolsStorage(isSetLogPingsEnabled = false)
        val store = GleanDebugToolsStore(
            initialState = initializeGleanDebugToolsState(
                logPingsToConsoleEnabled = false,
            ),
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                ),
            ),
        )
        assertFalse(store.state.logPingsToConsoleEnabled)
        assertFalse(gleanDebugToolsStorage.isSetLogPingsEnabled)
        store.dispatch(GleanDebugToolsAction.LogPingsToConsoleToggled)
        assertTrue(store.state.logPingsToConsoleEnabled)
        assertTrue(gleanDebugToolsStorage.isSetLogPingsEnabled)
    }

    @Test
    fun `GIVEN the log pings to console preference is on WHEN said preference is toggled THEN the preference should be enabled`() {
        gleanDebugToolsStorage = FakeGleanDebugToolsStorage(isSetLogPingsEnabled = true)
        val store = GleanDebugToolsStore(
            initialState = initializeGleanDebugToolsState(
                logPingsToConsoleEnabled = true,
            ),
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                ),
            ),
        )
        assertTrue(store.state.logPingsToConsoleEnabled)
        assertTrue(gleanDebugToolsStorage.isSetLogPingsEnabled)
        store.dispatch(GleanDebugToolsAction.LogPingsToConsoleToggled)
        assertFalse(store.state.logPingsToConsoleEnabled)
        assertFalse(gleanDebugToolsStorage.isSetLogPingsEnabled)
    }

    @Test
    fun `WHEN the change debug view tag action is dispatched with an appropriate debug view tag THEN the debug view tag should be changed accordingly and hasDebugViewTagError should be false`() {
        val initialDebugViewTag = ""
        val newDebugViewTag = "Test"
        val store = GleanDebugToolsStore(
            initialState = initializeGleanDebugToolsState(
                debugViewTag = initialDebugViewTag,
            ),
        )
        store.dispatch(GleanDebugToolsAction.DebugViewTagChanged(newTag = newDebugViewTag))
        assertEquals(newDebugViewTag, store.state.debugViewTag)
        assertFalse(store.state.hasDebugViewTagError)
    }

    @Test
    fun `WHEN the change debug view tag action is dispatched with a debug view tag that is too long THEN the debug view tag should be changed accordingly and hasDebugViewTagError should be true`() {
        val initialDebugViewTag = ""
        val newDebugViewTag = "123456789123456789123"
        val store = GleanDebugToolsStore(
            initialState = initializeGleanDebugToolsState(
                debugViewTag = initialDebugViewTag,
            ),
        )
        store.dispatch(GleanDebugToolsAction.DebugViewTagChanged(newTag = newDebugViewTag))
        assertEquals(newDebugViewTag, store.state.debugViewTag)
        assertTrue(store.state.hasDebugViewTagError)
    }

    @Test
    fun `WHEN the change debug view tag action is dispatched THEN any persisted debug view tag is cleared`() {
        val store = GleanDebugToolsStore(
            initialState = initializeGleanDebugToolsState(),
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                ),
            ),
        )
        assertFalse(gleanDebugToolsStorage.persistedDebugViewTagCleared)
        store.dispatch(GleanDebugToolsAction.DebugViewTagChanged(newTag = "test"))
        assertTrue(gleanDebugToolsStorage.persistedDebugViewTagCleared)
    }

    @Test
    fun `WHEN the send ping action is dispatched THEN the corresponding ping should be sent`() {
        val initialState = initializeGleanDebugToolsState()
        val store = GleanDebugToolsStore(
            initialState = initialState,
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                ),
            ),
        )
        assertEquals(initialState, store.state)
        assertFalse(gleanDebugToolsStorage.pingSent)
        store.dispatch(GleanDebugToolsAction.SendPing)
        assertEquals(initialState, store.state)
        assertTrue(gleanDebugToolsStorage.pingSent)
    }

    @Test
    fun `WHEN the send ping action is dispatched THEN a toast is shown`() {
        var toastShown = false
        val store = GleanDebugToolsStore(
            initialState = initializeGleanDebugToolsState(),
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                    showToast = { pingType ->
                        assertEquals("metrics", pingType)
                        toastShown = true
                    },
                ),
            ),
        )
        assertFalse(toastShown)
        store.dispatch(GleanDebugToolsAction.SendPing)
        assertTrue(toastShown)
    }

    @Test
    fun `GIVEN the debug view tag should be used WHEN the open debug view action is dispatched THEN the appropriate lambda is called with the appropriate debug view URL`() {
        var openDebugViewInvoked = false
        val debugViewTag = "test"
        val expectedDebugViewLink = "${PING_PREVIEW_URL}pings/$debugViewTag"
        val initialState = initializeGleanDebugToolsState(
            debugViewTag = debugViewTag,
        )
        val store = GleanDebugToolsStore(
            initialState = initialState,
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                    openDebugView = { debugViewLink ->
                        assertEquals(expectedDebugViewLink, debugViewLink)
                        openDebugViewInvoked = true
                    },
                ),
            ),
        )
        assertEquals(initialState, store.state)
        assertFalse(openDebugViewInvoked)
        store.dispatch(GleanDebugToolsAction.OpenDebugView(useDebugViewTag = true))
        assertEquals(initialState, store.state)
        assertTrue(openDebugViewInvoked)
    }

    @Test
    fun `GIVEN the debug view tag should not be used WHEN the open debug view action is dispatched THEN the appropriate lambda is called with the default debug view URL`() {
        val initialState = initializeGleanDebugToolsState()
        var openDebugViewInvoked = false
        val store = GleanDebugToolsStore(
            initialState = initialState,
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                    openDebugView = { debugViewLink ->
                        TestCase.assertEquals(PING_PREVIEW_URL, debugViewLink)
                        openDebugViewInvoked = true
                    },
                ),
            ),
        )
        assertEquals(initialState, store.state)
        assertFalse(openDebugViewInvoked)
        store.dispatch(GleanDebugToolsAction.OpenDebugView(useDebugViewTag = false))
        assertEquals(initialState, store.state)
        assertTrue(openDebugViewInvoked)
    }

    @Test
    fun `GIVEN the debug view tag should be used WHEN the copy debug view action is dispatched THEN the state should remain the same`() {
        val debugViewTag = "test"
        val expectedDebugViewLink = "${PING_PREVIEW_URL}pings/$debugViewTag"
        clipboardHandler.text = null
        val initialState = initializeGleanDebugToolsState(
            debugViewTag = debugViewTag,
        )
        val store = GleanDebugToolsStore(
            initialState = initialState,
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                ),
            ),
        )
        assertEquals(initialState, store.state)
        assertNotEquals(expectedDebugViewLink, clipboardHandler.text)
        store.dispatch(GleanDebugToolsAction.CopyDebugViewLink(useDebugViewTag = true))
        assertEquals(initialState, store.state)
        assertEquals(expectedDebugViewLink, clipboardHandler.text)
    }

    @Test
    fun `GIVEN the debug view tag should not be used WHEN the copy debug view action is dispatched THEN the state should remain the same`() {
        val initialState = initializeGleanDebugToolsState()
        clipboardHandler.text = null
        val store = GleanDebugToolsStore(
            initialState = initialState,
            middlewares = listOf(
                createMiddleware(
                    gleanDebugToolsStorage = gleanDebugToolsStorage,
                ),
            ),
        )
        assertEquals(initialState, store.state)
        assertNotEquals(PING_PREVIEW_URL, clipboardHandler.text)
        store.dispatch(GleanDebugToolsAction.CopyDebugViewLink(useDebugViewTag = false))
        assertEquals(initialState, store.state)
        assertEquals(PING_PREVIEW_URL, clipboardHandler.text)
    }

    private fun createMiddleware(
        gleanDebugToolsStorage: GleanDebugToolsStorage,
        openDebugView: (String) -> Unit = { _ -> },
        showToast: (String) -> Unit = { _ -> },
    ) = GleanDebugToolsMiddleware(
        gleanDebugToolsStorage = gleanDebugToolsStorage,
        clipboardHandler = clipboardHandler,
        openDebugView = openDebugView,
        showToast = showToast,
    )

    private fun initializeGleanDebugToolsState(
        logPingsToConsoleEnabled: Boolean? = null,
        pingType: String = "metrics",
        debugViewTag: String? = null,
    ) = GleanDebugToolsState(
        logPingsToConsoleEnabled = logPingsToConsoleEnabled ?: false,
        pingType = pingType,
        debugViewTag = debugViewTag ?: "",
        pingTypes = listOf("metrics", "crash"),
    )

    class FakeGleanDebugToolsStorage(
        var isSetLogPingsEnabled: Boolean = false,
    ) : GleanDebugToolsStorage {

        var pingSent = false
        var persistedDebugViewTagCleared = false

        override fun setLogPings(enabled: Boolean) {
            isSetLogPingsEnabled = enabled
        }

        override fun sendPing(pingType: String, debugViewTag: String) {
            pingSent = true
        }

        override fun clearPersistedDebugViewTag() {
            persistedDebugViewTagCleared = true
        }
    }
}
