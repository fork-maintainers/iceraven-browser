/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class ForwardMenuItemProviderTest {
    private val goForward: SessionUseCases.GoForwardUseCase = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN there is no selected tab WHEN building the forward menu item THEN return a disabled menu item`() =
        runTest {
            val provider = createProvider(BrowserStore(), backgroundScope)

            assertEquals(expectedItem(canGoForward = false), provider.itemFlow.value)
        }

    @Test
    fun `GIVEN the current page cannot navigate forward WHEN building the forward menu item THEN return a disabled menu item`() =
        runTest {
            val provider = createProvider(browserStore(canGoForward = false), backgroundScope)

            assertEquals(expectedItem(canGoForward = false), provider.itemFlow.value)
        }

    @Test
    fun `GIVEN the current page can navigate forward WHEN building the forward menu item THEN return an enabled menu item`() =
        runTest {
            val provider = createProvider(browserStore(canGoForward = true), backgroundScope)

            assertEquals(expectedItem(canGoForward = true), provider.itemFlow.value)
        }

    @Test
    fun `GIVEN a selected tab WHEN clicking the item THEN close the menu and navigate forward in that tab`() = runTest {
        var isDismissedWhenGoingForward = false
        every { goForward(tabId = TAB_ID) } answers { isDismissedWhenGoingForward = menu.isDismissed }
        val provider = createProvider(browserStore(canGoForward = true), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Forward(viewHistory = false), menu)

        verify { goForward(tabId = TAB_ID) }
        assertTrue(isDismissedWhenGoingForward)
    }

    @Test
    fun `GIVEN a selected tab WHEN long clicking the item THEN show its history in place of the menu`() = runTest {
        val provider = createProvider(browserStore(canGoForward = true), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Forward(viewHistory = true), menu)

        assertEquals(NavGraphDirections.actionGlobalTabHistoryDialogFragment(activeSessionId = null), menu.directions)
        verify(exactly = 0) { goForward(any()) }
    }

    @Test
    fun `GIVEN a custom tab WHEN long clicking the item THEN show its history with custom tab session ID in place of the menu`() =
        runTest {
            val provider =
                createProvider(
                    customTabBrowserStore(canGoForward = true),
                    backgroundScope,
                    target = MenuTarget.CustomTab("customTabId"),
                )

            provider.onEvent(MenuAction.Navigate.Forward(viewHistory = true), menu)

            assertEquals(
                NavGraphDirections.actionGlobalTabHistoryDialogFragment(activeSessionId = "customTabId"),
                menu.directions,
            )
            verify(exactly = 0) { goForward(any()) }
        }

    @Test
    fun `GIVEN no selected tab WHEN clicking the item THEN keep the menu open`() = runTest {
        val provider = createProvider(BrowserStore(), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Forward(viewHistory = false), menu)

        assertFalse(menu.isUsed)
        verify(exactly = 0) { goForward(any()) }
    }

    @Test
    fun `GIVEN no selected tab WHEN long clicking the item THEN keep the menu open`() = runTest {
        val provider = createProvider(BrowserStore(), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Forward(viewHistory = true), menu)

        assertFalse(menu.isUsed)
    }

    @Test
    fun `WHEN the item can or cannot navigate forward THEN handle all events it can dispatch`() = runTest {
        listOf(BrowserStore(), browserStore(canGoForward = false), browserStore(canGoForward = true)).forEach { store ->
            val provider = createProvider(store, backgroundScope)

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        }
    }

    @Test
    fun `WHEN asked about the events of other items THEN don't handle them`() = runTest {
        val provider = createProvider(browserStore(canGoForward = true), backgroundScope)

        assertFalse(provider.handles(MenuAction.Navigate.Back(viewHistory = false)))
        assertFalse(provider.handles(MenuAction.Navigate.Settings))
    }

    private fun createProvider(
        browserStore: BrowserStore,
        scope: CoroutineScope,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        ForwardMenuItemProvider(
            browserStore = browserStore,
            target = target,
            goForward = goForward,
            scope = scope,
        )

    private fun browserStore(canGoForward: Boolean) =
        BrowserStore(
            BrowserState(
                tabs =
                    listOf(
                        TabSessionState(
                            id = TAB_ID,
                            content =
                                ContentState(
                                    url = "https://mozilla.org",
                                    canGoForward = canGoForward,
                                ),
                        )
                    ),
                selectedTabId = TAB_ID,
            )
        )

    private fun customTabBrowserStore(canGoForward: Boolean) =
        BrowserStore(
            BrowserState(
                customTabs =
                    listOf(
                        createCustomTab(
                                url = "https://custom.org",
                                id = "customTabId",
                            )
                            .let { tab ->
                                tab.copy(content = tab.content.copy(canGoForward = canGoForward))
                            }
                    ),
                selectedTabId = null,
            )
        )

    private fun expectedItem(canGoForward: Boolean) =
        StandardMenuItem(
            title = Text.Resource(R.string.browser_menu_forward),
            icon = MenuItemIconRes(iconsR.drawable.mozac_ic_forward_24),
            onClickEvent = MenuAction.Navigate.Forward(viewHistory = false),
            onLongClickEvent = MenuAction.Navigate.Forward(viewHistory = true),
            state = if (canGoForward) MenuItemState.DEFAULT else MenuItemState.DISABLED,
        )

    private companion object {
        const val TAB_ID = "tab1"
    }
}
