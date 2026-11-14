/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.readermode

import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction.ReaderViewAction
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class ReaderViewMenuItemProviderTest {
    private val appStore: AppStore = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN reader view is not active WHEN building the menu item THEN return null`() = runTest {
        val provider = provider(browserStore(isReaderViewActive = false))

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN reader view is active WHEN building the menu item THEN return a properly configured one`() = runTest {
        val provider = provider(browserStore(isReaderViewActive = true))

        assertEquals(
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_customize_reader_view_2)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_tool_24),
                onClickEvent = MenuAction.CustomizeReaderView,
            ),
            provider.itemFlow.value,
        )
    }

    @Test
    fun `GIVEN there is no selected tab WHEN building the menu item THEN return null`() = runTest {
        val provider = provider(BrowserStore())

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `WHEN clicking the item THEN close the menu and show the reader view controls`() = runTest {
        val provider = provider(browserStore(isReaderViewActive = true))

        provider.onEvent(MenuAction.CustomizeReaderView, menu)

        assertTrue(menu.isDismissed)
        verify { appStore.dispatch(ReaderViewAction.ReaderViewControlsShown) }
    }

    @Test
    fun `WHEN building the item THEN handle all events it can dispatch and no others`() = runTest {
        val provider = provider(browserStore(isReaderViewActive = true))

        val events = requireNotNull(provider.itemFlow.value).reachableEvents()

        assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        assertFalse(provider.handles(MenuAction.FindInPage))
    }

    // The item is kept up to date on a scope that runTest cancels at the end of each test.
    private fun TestScope.provider(
        browserStore: BrowserStore,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        ReaderViewMenuItemProvider(
            browserStore = browserStore,
            target = target,
            appStore = appStore,
            scope = backgroundScope,
        )

    private fun browserStore(isReaderViewActive: Boolean) =
        BrowserStore(
            BrowserState(
                tabs =
                    listOf(
                        createTab(
                            url = "https://mozilla.org",
                            id = TAB_ID,
                            readerState = ReaderState(active = isReaderViewActive),
                        )
                    ),
                selectedTabId = TAB_ID,
            )
        )

    private companion object {
        const val TAB_ID = "tab1"
    }
}
