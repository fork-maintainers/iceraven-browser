/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.speech.RecognizerIntent
import io.mockk.every
import io.mockk.mockk
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor
import org.mozilla.fenix.widget.VoiceSearchActivity.Companion.SPEECH_PROCESSING
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SearchWidgetIntentsTest {

    @Test
    fun `WHEN a normal text search intent is built THEN it opens to search without private mode`() {
        val intent = SearchWidgetIntents.textSearch(testContext)

        assertEquals(IntentReceiverActivity::class.java.name, intent.component!!.className)
        assertEquals(
            StartSearchIntentProcessor.SEARCH_WIDGET,
            intent.getStringExtra(HomeActivity.OPEN_TO_SEARCH),
        )
        assertFalse(intent.hasExtra(HomeActivity.PRIVATE_BROWSING_MODE))
    }

    @Test
    fun `GIVEN voice search is disabled WHEN a voice search intent is built THEN it is null`() {
        every { testContext.components.settings.shouldShowVoiceSearch } returns false
        stubSpeechRecognizer()

        assertNull(SearchWidgetIntents.maybeVoiceSearch(testContext))
    }

    @Test
    fun `GIVEN no speech recognizer is installed WHEN a voice search intent is built THEN it is null`() {
        every { testContext.components.settings.shouldShowVoiceSearch } returns true

        assertNull(SearchWidgetIntents.maybeVoiceSearch(testContext))
    }

    @Test
    fun `GIVEN voice search is enabled and a speech recognizer is installed WHEN a voice search intent is built THEN it starts a voice search`() {
        every { testContext.components.settings.shouldShowVoiceSearch } returns true
        stubSpeechRecognizer()

        val intent = SearchWidgetIntents.maybeVoiceSearch(testContext)!!

        assertEquals(VoiceSearchActivity::class.java.name, intent.component!!.className)
        assertTrue(intent.getBooleanExtra(SPEECH_PROCESSING, false))
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, intent.flags)
    }

    @Test
    fun `GIVEN the lens integration is disabled WHEN a lens search intent is built THEN it is null`() {
        val context: Context = mockk {
            every { components.settings.googleLensIntegrationEnabled } returns false
        }

        assertNull(SearchWidgetIntents.maybeLensSearch(context))
    }

    @Test
    fun `GIVEN the user disabled the lens integration WHEN a lens search intent is built THEN it is null`() {
        val context: Context = mockk {
            every { components.settings.googleLensIntegrationEnabled } returns true
            every { components.settings.googleLensIntegrationUserEnabled } returns false
        }

        assertNull(SearchWidgetIntents.maybeLensSearch(context))
    }

    @Test
    fun `GIVEN a non-Google default engine WHEN a lens search intent is built THEN it is null`() {
        assertNull(SearchWidgetIntents.maybeLensSearch(contextWithDefaultEngine("bing")))
    }

    @Test
    fun `GIVEN lens is enabled and Google is the default engine WHEN a lens search intent is built THEN it starts a lens search`() {
        val intent = SearchWidgetIntents.maybeLensSearch(contextWithDefaultEngine("google-b-1-m"))!!

        assertEquals(LensSearchActivity::class.java.name, intent.component!!.className)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, intent.flags)
    }

    private fun stubSpeechRecognizer() {
        val info =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "fake.voice.recognizer"
                        name = "FakeRecognizerActivity"
                    }
            }
        @Suppress("Deprecation")
        shadowOf(testContext.packageManager)
            .addResolveInfoForIntent(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), info)
    }

    private fun contextWithDefaultEngine(engineId: String): Context {
        val searchState =
            SearchState(
                region = RegionState("US", "US"),
                regionSearchEngines =
                    listOf(SearchEngine(engineId, "Engine", mockk(), type = SearchEngine.Type.BUNDLED)),
                regionDefaultSearchEngineId = engineId,
            )
        every { testContext.components.settings.googleLensIntegrationEnabled } returns true
        every { testContext.components.settings.googleLensIntegrationUserEnabled } returns true
        every { testContext.components.core.store } returns BrowserStore(BrowserState(search = searchState))
        return testContext
    }
}
