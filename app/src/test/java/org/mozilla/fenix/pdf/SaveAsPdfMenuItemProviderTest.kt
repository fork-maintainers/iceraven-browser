/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.pdf

import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.ui.icons.R as iconsR
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class SaveAsPdfMenuItemProviderTest {
    private val saveToPdf: SessionUseCases.SaveToPdfUseCase = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `WHEN building the menu item THEN returned a properly configured one`() {
        val provider = createProvider()
        val expectedItem =
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_save_as_pdf_2)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_save_file_24),
                onClickEvent = MenuAction.SaveAsPdfRequested,
            )

        val item = provider.itemFlow.value

        assertEquals(expectedItem, item)
    }

    @Test
    fun `WHEN clicking the item THEN close the menu and save the current page as a PDF`() {
        val provider = createProvider()

        provider.onEvent(MenuAction.SaveAsPdfRequested, menu)

        assertTrue(menu.isDismissed)
        verify { saveToPdf(tabId = TAB_ID) }
    }

    @Test
    fun `WHEN building the item THEN handle all events it can dispatch and no others`() {
        val provider = createProvider()

        val events = requireNotNull(provider.itemFlow.value).reachableEvents()

        assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        assertFalse(provider.handles(MenuAction.PrintRequested))
    }

    private fun singleTabStore() =
        BrowserStore(BrowserState(tabs = listOf(createTab("https://mozilla.org", id = TAB_ID)), selectedTabId = TAB_ID))

    private fun createProvider(
        browserStore: BrowserStore = singleTabStore(),
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        SaveAsPdfMenuItemProvider(
            browserStore = browserStore,
            target = target,
            saveToPdf = saveToPdf,
        )

    private companion object {
        const val TAB_ID = "tab1"
    }
}
