/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.print

import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class PrintMenuItemProviderTest {
    private val printContent: SessionUseCases.PrintContentUseCase = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN the app runs on Android Automotive WHEN building the menu item THEN return null`() {
        assertNull(createProvider(isAndroidAutomotiveAvailable = true).itemFlow.value)
    }

    @Test
    fun `GIVEN a non Automotive device WHEN building the menu item THEN return a properly configured menu button`() {
        assertEquals(
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_print_2)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_print_24),
                onClickEvent = MenuAction.PrintRequested,
            ),
            createProvider(isAndroidAutomotiveAvailable = false).itemFlow.value,
        )
    }

    @Test
    fun `WHEN clicking the item THEN close the menu and print the current page`() {
        val provider = createProvider(isAndroidAutomotiveAvailable = false)

        provider.onEvent(MenuAction.PrintRequested, menu)

        assertTrue(menu.isDismissed)
        verify { printContent(tabId = TAB_ID) }
    }

    @Test
    fun `WHEN building the item THEN handle all events it can dispatch and no others`() {
        val provider = createProvider(isAndroidAutomotiveAvailable = false)

        val events = requireNotNull(provider.itemFlow.value).reachableEvents()

        assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        assertFalse(provider.handles(MenuAction.SaveAsPdfRequested))
    }

    private fun singleTabStore() =
        BrowserStore(BrowserState(tabs = listOf(createTab("https://mozilla.org", id = TAB_ID)), selectedTabId = TAB_ID))

    private fun createProvider(
        isAndroidAutomotiveAvailable: Boolean,
        browserStore: BrowserStore = singleTabStore(),
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        PrintMenuItemProvider(
            isAndroidAutomotiveAvailable = isAndroidAutomotiveAvailable,
            browserStore = browserStore,
            target = target,
            printContent = printContent,
        )

    private companion object {
        const val TAB_ID = "tab1"
    }
}
