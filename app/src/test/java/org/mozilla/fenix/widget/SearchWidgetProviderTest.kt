/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertNotNull
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.utils.Settings
import org.mozilla.gecko.search.SearchWidgetProvider
import org.mozilla.gecko.search.SearchWidgetProviderSize
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchWidgetProviderTest {
    @get:Rule val gleanRule = FenixGleanTestRule(testContext)

    @Test
    fun testGetLayoutSize() {
        val sizes =
            mapOf(
                0 to SearchWidgetProviderSize.EXTRA_SMALL_V1,
                10 to SearchWidgetProviderSize.EXTRA_SMALL_V1,
                63 to SearchWidgetProviderSize.EXTRA_SMALL_V1,
                64 to SearchWidgetProviderSize.EXTRA_SMALL_V2,
                99 to SearchWidgetProviderSize.EXTRA_SMALL_V2,
                100 to SearchWidgetProviderSize.SMALL,
                191 to SearchWidgetProviderSize.SMALL,
                192 to SearchWidgetProviderSize.MEDIUM,
                255 to SearchWidgetProviderSize.MEDIUM,
                256 to SearchWidgetProviderSize.LARGE,
                1000 to SearchWidgetProviderSize.LARGE,
            )

        for ((dp, layoutSize) in sizes) {
            assertEquals(layoutSize, SearchWidgetProvider.getLayoutSize(dp))
        }
    }

    @Test
    fun testGetLargeLayout() {
        assertEquals(
            R.layout.search_widget_large,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.LARGE, showMic = false),
        )
        assertEquals(
            R.layout.search_widget_large,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.LARGE, showMic = true),
        )
    }

    @Test
    fun testGetMediumLayout() {
        assertEquals(
            R.layout.search_widget_medium,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.MEDIUM, showMic = false),
        )
        assertEquals(
            R.layout.search_widget_medium,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.MEDIUM, showMic = true),
        )
    }

    @Test
    fun testGetSmallLayout() {
        assertEquals(
            R.layout.search_widget_small_no_mic,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.SMALL, showMic = false),
        )
        assertEquals(
            R.layout.search_widget_small,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.SMALL, showMic = true),
        )
    }

    @Test
    fun testGetExtraSmall2Layout() {
        assertEquals(
            R.layout.search_widget_extra_small_v2,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.EXTRA_SMALL_V2, showMic = false),
        )
        assertEquals(
            R.layout.search_widget_extra_small_v2,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.EXTRA_SMALL_V2, showMic = true),
        )
    }

    @Test
    fun testGetExtraSmall1Layout() {
        assertEquals(
            R.layout.search_widget_extra_small_v1,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.EXTRA_SMALL_V1, showMic = false),
        )
        assertEquals(
            R.layout.search_widget_extra_small_v1,
            SearchWidgetProvider.getLayout(SearchWidgetProviderSize.EXTRA_SMALL_V1, showMic = true),
        )
    }

    @Test
    fun testGetText() {
        val context = mockk<Context>()
        every { context.getString(R.string.search_widget_text_short) } returns "Search"
        every { context.getString(R.string.search_widget_text_long) } returns "Search the web"

        assertEquals(
            "Search the web",
            SearchWidgetProvider.getText(SearchWidgetProviderSize.LARGE, context),
        )
        assertEquals(
            "Search",
            SearchWidgetProvider.getText(SearchWidgetProviderSize.MEDIUM, context),
        )
        assertNull(SearchWidgetProvider.getText(SearchWidgetProviderSize.SMALL, context))
        assertNull(SearchWidgetProvider.getText(SearchWidgetProviderSize.EXTRA_SMALL_V1, context))
        assertNull(SearchWidgetProvider.getText(SearchWidgetProviderSize.EXTRA_SMALL_V2, context))
    }

    @Test
    fun `GIVEN voice search is disabled WHEN createVoiceSearchIntent is called THEN it returns null`() {
        val widgetProvider = SearchWidgetProvider()
        val context: Context = mockk {
            every { components.settings.shouldShowVoiceSearch } returns false
        }

        val result = widgetProvider.createVoiceSearchIntent(context)

        assertNull(result)
    }

    @Test
    fun `GIVEN lens integration disabled WHEN createLensSearchIntent is called THEN it returns null`() {
        val widgetProvider = SearchWidgetProvider()
        val context: Context = mockk {
            every { components.settings.googleLensIntegrationEnabled } returns false
        }

        assertNull(widgetProvider.createLensSearchIntent(context))
    }

    @Test
    fun `GIVEN lens user pref disabled WHEN createLensSearchIntent is called THEN it returns null`() {
        val widgetProvider = SearchWidgetProvider()
        val context: Context = mockk {
            every { components.settings.googleLensIntegrationEnabled } returns true
            every { components.settings.googleLensIntegrationUserEnabled } returns false
        }

        assertNull(widgetProvider.createLensSearchIntent(context))
    }

    @Test
    fun `GIVEN a non-Google default engine WHEN createLensSearchIntent is called THEN it returns null`() {
        val widgetProvider = SearchWidgetProvider()
        val context = contextWithDefaultEngine("bing")

        assertNull(widgetProvider.createLensSearchIntent(context))
    }

    @Test
    fun `GIVEN lens enabled and Google is the default engine WHEN createLensSearchIntent is called THEN it returns an intent`() {
        val widgetProvider = SearchWidgetProvider()
        val context = contextWithDefaultEngine("google-b-1-m")

        assertNotNull(widgetProvider.createLensSearchIntent(context))
    }

    @Test
    fun `GIVEN the widget was drawn without the lens button WHEN it is redrawn with and then without a lens intent THEN the lens button is shown and then hidden`() {
        val widgetProvider = SearchWidgetProvider()
        val view =
            widgetProvider.createLargeRemoteViews(lensSearchIntent = null).apply(testContext, FrameLayout(testContext))
        assertEquals(View.GONE, view.findViewById<View>(R.id.button_search_widget_lens).visibility)

        widgetProvider.createLargeRemoteViews(lensSearchIntent = pendingIntent()).reapply(testContext, view)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.button_search_widget_lens).visibility)

        widgetProvider.createLargeRemoteViews(lensSearchIntent = null).reapply(testContext, view)
        assertEquals(View.GONE, view.findViewById<View>(R.id.button_search_widget_lens).visibility)
    }

    private fun SearchWidgetProvider.createLargeRemoteViews(lensSearchIntent: PendingIntent?) =
        createRemoteViews(
            testContext,
            R.layout.search_widget_large,
            pendingIntent(),
            null,
            lensSearchIntent,
            "Search",
        )

    private fun pendingIntent() = PendingIntent.getActivity(testContext, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)

    private fun contextWithDefaultEngine(engineId: String): Context {
        val searchState =
            SearchState(
                region = RegionState("US", "US"),
                regionSearchEngines =
                    listOf(SearchEngine(engineId, "Engine", mockk(), type = SearchEngine.Type.BUNDLED)),
                regionDefaultSearchEngineId = engineId,
            )
        val store = BrowserStore(BrowserState(search = searchState))
        every { testContext.components.settings.googleLensIntegrationEnabled } returns true
        every { testContext.components.settings.googleLensIntegrationUserEnabled } returns true
        every { testContext.components.core.store } returns store
        return testContext
    }

    @Test
    fun `GIVEN widgets set on screen shown WHEN updateAllWidgets is called THEN it sends a broadcast to update all widgets`() {
        val widgetManager: AppWidgetManager = mockk()
        val componentNameCaptor = slot<ComponentName>()
        val widgetsToUpdate = intArrayOf(1, 2)
        every { widgetManager.getAppWidgetIds(capture(componentNameCaptor)) } returns widgetsToUpdate
        val context: Context = mockk(relaxed = true)
        val intentCaptor = slot<Intent>()
        every { context.sendBroadcast(capture(intentCaptor)) } just Runs

        SearchWidgetProvider.updateAllWidgets(context, widgetManager)

        verify { context.sendBroadcast(any()) }
        assertEquals(SearchWidgetProvider::class.java.name, componentNameCaptor.captured.className)
        assertEquals(
            SearchWidgetProvider::class.java.name,
            intentCaptor.captured.component!!.className,
        )
        assertEquals(AppWidgetManager.ACTION_APPWIDGET_UPDATE, intentCaptor.captured.action)
        @Suppress("DEPRECATION")
        assertEquals(
            widgetsToUpdate,
            intentCaptor.captured.extras!!.get(AppWidgetManager.EXTRA_APPWIDGET_IDS),
        )
    }

    @Test
    fun `GIVEN no widgets set shown WHEN updateAllWidgets is called THEN it does not try to update widgets`() {
        val widgetManager: AppWidgetManager = mockk()
        val componentNameCaptor = slot<ComponentName>()
        val widgetsToUpdate = intArrayOf()
        every { widgetManager.getAppWidgetIds(capture(componentNameCaptor)) } returns widgetsToUpdate
        val context: Context = mockk(relaxed = true)
        val intentCaptor = slot<Intent>()
        every { context.sendBroadcast(capture(intentCaptor)) } just Runs

        SearchWidgetProvider.updateAllWidgets(context, widgetManager)

        verify(exactly = 0) { context.sendBroadcast(any()) }
    }

    @Test
    fun `WHEN the search widget is added on homescreen THEN record telemetry and persist that the widget is installed`() {
        val settings = Settings(testContext)
        every { testContext.components.settings } returns settings
        val widgetProvider = SearchWidgetProvider()
        assertFalse(settings.searchWidgetInstalled)

        widgetProvider.onEnabled(testContext)

        assertTrue(settings.searchWidgetInstalled)
        assertEquals(true, Metrics.searchWidgetInstalled.testGetValue())
    }

    @Test
    fun `WHEN the search widget is removed from the homescreen THEN record telemetry and persist that the widget is uninstalled`() {
        val settings = Settings(testContext)
        every { testContext.components.settings } returns settings
        val widgetProvider = SearchWidgetProvider()
        settings.searchWidgetInstalled = true

        widgetProvider.onDisabled(testContext)

        assertFalse(testContext.components.settings.searchWidgetInstalled)
        assertEquals(false, Metrics.searchWidgetInstalled.testGetValue())
    }

    @Test
    fun `GIVEN not knowing search widget is installed WHEN a widget is updated THEN record telemetry and persist that the widget is installed`() {
        val settings = Settings(testContext)
        every { testContext.components.settings } returns settings
        every { testContext.components.core.store } returns BrowserStore(BrowserState())
        val widgetProvider = SearchWidgetProvider()
        assertFalse(settings.searchWidgetInstalled)

        widgetProvider.onUpdate(testContext, mockk(), intArrayOf())

        assertTrue(testContext.components.settings.searchWidgetInstalled)
        assertEquals(true, Metrics.searchWidgetInstalled.testGetValue())
    }
}
