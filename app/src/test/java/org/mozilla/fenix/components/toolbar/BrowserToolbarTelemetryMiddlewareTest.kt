/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarInteraction.BrowserToolbarEvent.Source
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarStore
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Toolbar
import org.mozilla.fenix.components.toolbar.BrowserToolbarTelemetryMiddleware.ToolbarActionRecord
import org.mozilla.fenix.components.toolbar.DisplayActions.AddBookmarkClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.EditBookmarkClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.EditShortcutClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.HomepageClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.MenuClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.NavigateBackClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.NavigateBackLongClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.NavigateForwardClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.NavigateForwardLongClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.RefreshClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.ShareClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.ShortcutLongClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.StopRefreshClicked
import org.mozilla.fenix.components.toolbar.DisplayActions.TranslateClicked
import org.mozilla.fenix.components.toolbar.PageEndActionsInteractions.ReaderModeClicked
import org.mozilla.fenix.components.toolbar.StartPageActions.SiteInfoClicked
import org.mozilla.fenix.components.toolbar.TabCounterInteractions.AddNewPrivateTab
import org.mozilla.fenix.components.toolbar.TabCounterInteractions.AddNewTab
import org.mozilla.fenix.components.toolbar.TabCounterInteractions.TabCounterClicked
import org.mozilla.fenix.components.toolbar.TabCounterInteractions.TabCounterLongClicked
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.telemetry.ACTION_EDIT_SHORTCUT_CLICKED
import org.mozilla.fenix.telemetry.ACTION_SHORTCUT_LONG_CLICKED
import org.mozilla.fenix.telemetry.SOURCE_ADDRESS_BAR
import org.mozilla.fenix.telemetry.SOURCE_NAVIGATION_BAR
import org.mozilla.fenix.telemetry.SURFACE_BROWSER
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class BrowserToolbarTelemetryMiddlewareTest {
    @get:Rule
    val gleanRule = FenixGleanTestRule(testContext)

    @Test
    fun `WHEN menu button is clicked THEN record telemetry based on browser end or navbar source`() {
        buildStore.dispatch(MenuClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.MenuClicked.action)

        buildStore.dispatch(MenuClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.MenuClicked.action)
    }

    @Test
    fun `WHEN tab counter is clicked THEN record telemetry based on browser end or navbar source`() {
        buildStore.dispatch(TabCounterClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.TabCounterClicked.action)

        buildStore.dispatch(TabCounterClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.TabCounterClicked.action)
    }

    @Test
    fun `WHEN tab counter is long clicked THEN record telemetry based on browser end or navbar source`() {
        buildStore.dispatch(TabCounterLongClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.TabCounterLongClicked.action)

        buildStore.dispatch(TabCounterLongClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.TabCounterLongClicked.action)
    }

    @Test
    fun `WHEN adding a new tab THEN record telemetry based on browser end or navbar source`() {
        buildStore.dispatch(AddNewTab(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.AddNewTab.action)

        buildStore.dispatch(AddNewTab(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.AddNewTab.action)
    }

    @Test
    fun `WHEN adding a new private tab THEN record telemetry based on browser end or navbar source`() {
        buildStore.dispatch(AddNewPrivateTab(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.AddNewPrivateTab.action)

        buildStore.dispatch(AddNewPrivateTab(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.AddNewPrivateTab.action)
    }

    @Test
    fun `WHEN navigating back THEN record telemetry based on browser start, end or navbar source`() {
        buildStore.dispatch(NavigateBackClicked(Source.AddressBar.BrowserStart))
        assertTelemetryRecorded(Source.AddressBar.BrowserStart, item = ToolbarActionRecord.NavigateBackClicked.action)

        buildStore.dispatch(NavigateBackClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.NavigateBackClicked.action)

        buildStore.dispatch(NavigateBackClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.NavigateBackClicked.action)
    }

    @Test
    fun `WHEN navigating back is long clicked THEN record telemetry based on browser start, end or navbar source`() {
        buildStore.dispatch(NavigateBackLongClicked(Source.AddressBar.BrowserStart))
        assertTelemetryRecorded(Source.AddressBar.BrowserStart, item = ToolbarActionRecord.NavigateBackLongClicked.action)

        buildStore.dispatch(NavigateBackLongClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.NavigateBackLongClicked.action)

        buildStore.dispatch(NavigateBackLongClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.NavigateBackLongClicked.action)
    }

    @Test
    fun `WHEN navigating forward THEN record browser start telemetry`() {
        buildStore.dispatch(NavigateForwardClicked)
        assertTelemetryRecorded(Source.AddressBar.BrowserStart, item = ToolbarActionRecord.NavigateForwardClicked.action)
    }

    @Test
    fun `WHEN navigating forward is long clicked THEN record browser start telemetry`() {
        buildStore.dispatch(NavigateForwardLongClicked)
        assertTelemetryRecorded(Source.AddressBar.BrowserStart, item = ToolbarActionRecord.NavigateForwardLongClicked.action)
    }

    @Test
    fun `WHEN refreshing the page THEN record browser start telemetry`() {
        buildStore.dispatch(RefreshClicked(bypassCache = false))
        assertTelemetryRecorded(Source.AddressBar.BrowserStart, item = ToolbarActionRecord.RefreshClicked.action)
    }

    @Test
    fun `WHEN refreshing the page is stopped THEN record browser start telemetry`() {
        buildStore.dispatch(StopRefreshClicked)
        assertTelemetryRecorded(Source.AddressBar.BrowserStart, item = ToolbarActionRecord.StopRefreshClicked.action)
    }

    @Test
    fun `WHEN adding a bookmark THEN record telemetry based on browser end or navbar source`() {
        buildStore.dispatch(AddBookmarkClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.AddBookmarkClicked.action)

        buildStore.dispatch(AddBookmarkClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.AddBookmarkClicked.action)
    }

    @Test
    fun `WHEN navigating to edit a bookmark THEN record telemetry based on browser end or navbar source`() {
        buildStore.dispatch(EditBookmarkClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.EditBookmarkClicked.action)

        buildStore.dispatch(EditBookmarkClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.EditBookmarkClicked.action)
    }

    @Test
    fun `WHEN sharing a page THEN record telemetry based on page end, browser end or navbar source`() {
        buildStore.dispatch(ShareClicked(Source.AddressBar.PageEnd))
        assertTelemetryRecorded(Source.AddressBar.PageEnd, item = ToolbarActionRecord.ShareClicked.action)

        buildStore.dispatch(ShareClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.ShareClicked.action)

        buildStore.dispatch(ShareClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.ShareClicked.action)
    }

    @Test
    fun `WHEN site info is clicked THEN record page start telemetry`() {
        buildStore.dispatch(SiteInfoClicked)
        assertTelemetryRecorded(Source.AddressBar.PageStart, item = ToolbarActionRecord.SecurityIndicatorClicked.action)
    }

    @Test
    fun `WHEN reader mode is clicked THEN record page end telemetry`() {
        buildStore.dispatch(ReaderModeClicked(isActive = false))
        assertTelemetryRecorded(Source.AddressBar.PageEnd, item = ToolbarActionRecord.ReaderModeClicked.action)

        buildStore.dispatch(ReaderModeClicked(isActive = true))
        assertTelemetryRecorded(Source.AddressBar.PageEnd, item = ToolbarActionRecord.ReaderModeClicked.action)
    }

    @Test
    fun `WHEN translating a page THEN record telemetry based on page end, browser end or navbar source`() {
        buildStore.dispatch(TranslateClicked(Source.AddressBar.PageEnd))
        assertTelemetryRecorded(Source.AddressBar.PageEnd, item = ToolbarActionRecord.TranslateClicked.action)

        buildStore.dispatch(TranslateClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.TranslateClicked.action)

        buildStore.dispatch(TranslateClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.TranslateClicked.action)
    }

    @Test
    fun `WHEN homepage is clicked THEN record telemetry based on browser end or navbar source`() {
        buildStore.dispatch(HomepageClicked(Source.AddressBar.BrowserEnd))
        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.HomepageClicked.action)

        buildStore.dispatch(HomepageClicked(Source.NavigationBar))
        assertTelemetryRecorded(Source.NavigationBar, item = ToolbarActionRecord.HomepageClicked.action)
    }

    @Test
    fun `WHEN summarize is clicked THEN record related telemetry`() {
        buildStore.dispatch(DisplayActions.SummarizeClicked(Source.AddressBar.BrowserEnd))

        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.SummarizeClicked.action)
    }

    @Test
    fun `WHEN a shortcut is long clicked THEN record telemetry`() {
        buildStore.dispatch(ShortcutLongClicked(Source.AddressBar.BrowserEnd))

        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.ShortcutLongClicked.action)
    }

    @Test
    fun `WHEN choosing to edit a shortcut THEN record telemetry`() {
        buildStore.dispatch(EditShortcutClicked(Source.AddressBar.BrowserEnd))

        assertTelemetryRecorded(Source.AddressBar.BrowserEnd, item = ToolbarActionRecord.EditShortcutClicked.action)
    }

    @Test
    fun `WHEN recording telemetry for the shortcut interactions THEN use the intended event descriptions`() {
        assertEquals(ACTION_SHORTCUT_LONG_CLICKED, ToolbarActionRecord.ShortcutLongClicked.action)

        assertEquals(ACTION_EDIT_SHORTCUT_CLICKED, ToolbarActionRecord.EditShortcutClicked.action)
    }

    private fun assertTelemetryRecorded(
        source: Source,
        item: String,
    ) {
        assertNotNull(Toolbar.buttonTapped.testGetValue())
        val snapshot = Toolbar.buttonTapped.testGetValue()!!
        val last = snapshot.last()
        val expectedSource = when (source) {
            is Source.AddressBar, Source.Unknown -> SOURCE_ADDRESS_BAR
            Source.NavigationBar -> SOURCE_NAVIGATION_BAR
        }
        assertEquals(item, last.extra?.getValue("item"))
        assertEquals(expectedSource, last.extra?.getValue("source"))
        assertEquals(SURFACE_BROWSER, last.extra?.getValue("surface"))
        if (source is Source.AddressBar) {
            assertEquals(source.telemetryName(), last.extra?.getValue("extra"))
        } else {
            assertNull(last.extra?.get("extra"))
        }
    }

    private val buildStore = BrowserToolbarStore(
        middleware = listOf(BrowserToolbarTelemetryMiddleware()),
    )
}
