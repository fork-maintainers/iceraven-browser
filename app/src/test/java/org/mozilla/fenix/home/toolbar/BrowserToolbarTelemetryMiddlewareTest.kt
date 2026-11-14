/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.toolbar

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarInteraction.BrowserToolbarEvent.Source
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarStore
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Toolbar
import org.mozilla.fenix.GleanMetrics.ToolbarGoogleLensButton
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.lens.CameraMode
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.mozilla.fenix.home.toolbar.BrowserToolbarTelemetryMiddleware.ToolbarActionRecord
import org.mozilla.fenix.home.toolbar.DisplayActions.MenuClicked
import org.mozilla.fenix.home.toolbar.TabCounterInteractions.AddNewPrivateTab
import org.mozilla.fenix.home.toolbar.TabCounterInteractions.AddNewTab
import org.mozilla.fenix.home.toolbar.TabCounterInteractions.TabCounterClicked
import org.mozilla.fenix.home.toolbar.TabCounterInteractions.TabCounterLongClicked
import org.mozilla.fenix.search.EditPageEndActionsInteractions.LensButtonClicked
import org.mozilla.fenix.search.EditPageEndActionsInteractions.QrScannerClicked
import org.mozilla.fenix.telemetry.ACTION_LENS_CLICKED
import org.mozilla.fenix.telemetry.ACTION_LENS_QR_CLICKED
import org.mozilla.fenix.telemetry.ACTION_QR_CLICKED
import org.mozilla.fenix.telemetry.SOURCE_ADDRESS_BAR
import org.mozilla.fenix.telemetry.SOURCE_NAVIGATION_BAR
import org.mozilla.fenix.telemetry.SURFACE_HOME
import org.mozilla.fenix.utils.Settings

@RunWith(AndroidJUnit4::class)
class BrowserToolbarTelemetryMiddlewareTest {
    @get:Rule val gleanRule = FenixGleanTestRule(testContext)

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

    private fun assertTelemetryRecorded(
        source: Source,
        item: String,
    ) {
        val values = Toolbar.buttonTapped.testGetValue()
        assertNotNull(values)
        val last = values.last()
        val expectedSource =
            when (source) {
                is Source.AddressBar,
                Source.Unknown -> SOURCE_ADDRESS_BAR
                Source.NavigationBar -> SOURCE_NAVIGATION_BAR
            }
        assertEquals(item, last.extra?.get("item"))
        assertEquals(expectedSource, last.extra?.get("source"))
        assertEquals(SURFACE_HOME, last.extra?.get("surface"))
        if (source is Source.AddressBar) {
            assertEquals(source.telemetryName(), last.extra?.get("extra"))
        } else {
            assertNull(last.extra?.get("extra"))
        }
    }

    @Test
    fun `WHEN the QR scanner button is clicked THEN record the QR button tap`() {
        buildStore.dispatch(QrScannerClicked)

        assertEditButtonTapRecorded(ACTION_QR_CLICKED)
    }

    @Test
    fun `GIVEN Lens is the last Lens camera mode WHEN the Lens button is clicked THEN record the Lens button tap`() {
        settings.lensCameraLastMode = CameraMode.LENS

        buildStore.dispatch(LensButtonClicked)

        assertEditButtonTapRecorded(ACTION_LENS_CLICKED)
        assertNotNull(ToolbarGoogleLensButton.tapped.testGetValue())
    }

    @Test
    fun `GIVEN QR is the last Lens camera mode WHEN the Lens button is clicked THEN record the Lens QR button tap`() {
        settings.lensCameraLastMode = CameraMode.QR

        buildStore.dispatch(LensButtonClicked)

        assertEditButtonTapRecorded(ACTION_LENS_QR_CLICKED)
        assertNull(ToolbarGoogleLensButton.tapped.testGetValue())
    }

    private fun assertEditButtonTapRecorded(item: String) {
        val event = Toolbar.buttonTapped.testGetValue()?.single()
        assertEquals(item, event?.extra?.get("item"))
        assertEquals(SOURCE_ADDRESS_BAR, event?.extra?.get("source"))
        assertEquals(SURFACE_HOME, event?.extra?.get("surface"))
    }

    private val settings = Settings(testContext)
    private val appStore = AppStore()
    private val buildStore =
        BrowserToolbarStore(middleware = listOf(BrowserToolbarTelemetryMiddleware(appStore, settings)))
}
