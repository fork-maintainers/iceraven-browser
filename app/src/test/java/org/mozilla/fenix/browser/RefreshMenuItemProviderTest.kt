/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class RefreshMenuItemProviderTest {
    private val reload: SessionUseCases.ReloadUrlUseCase = mockk(relaxed = true)
    private val stopLoading: SessionUseCases.StopLoadingUseCase = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN there is no selected tab WHEN building the refresh menu item THEN return null`() = runTest {
        val provider = provider(BrowserStore())

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the current page is loading WHEN building the refresh menu item THEN return stop menu item`() = runTest {
        val provider = provider(browserStore(loading = true))

        assertEquals(
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_stop)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_cross_24),
                onClickEvent = MenuAction.Navigate.Stop,
            ),
            provider.itemFlow.value,
        )
    }

    @Test
    fun `GIVEN the current page is not loading WHEN building the refresh menu item THEN return refresh menu item`() =
        runTest {
            val provider = provider(browserStore(loading = false))

            assertEquals(
                StandardMenuItem(
                    title = MenuItemTitle(Text.Resource(R.string.browser_menu_refresh)),
                    icon = MenuItemIconRes(iconsR.drawable.mozac_ic_arrow_clockwise_24),
                    onClickEvent = MenuAction.Navigate.Reload(bypassCache = false),
                    onLongClickEvent = MenuAction.Navigate.Reload(bypassCache = true),
                ),
                provider.itemFlow.value,
            )
        }

    @Test
    fun `GIVEN a selected tab WHEN clicking refresh THEN close the menu and reload the page using the cache`() =
        runTest {
            val flags = slot<LoadUrlFlags>()
            val provider = provider(browserStore(loading = false))

            provider.onEvent(MenuAction.Navigate.Reload(bypassCache = false), menu)

            assertTrue(menu.isDismissed)
            verify { reload(tabId = TAB_ID, flags = capture(flags)) }
            assertEquals(LoadUrlFlags.none().value, flags.captured.value)
        }

    @Test
    fun `GIVEN a selected tab WHEN long clicking refresh THEN close the menu and reload the page bypassing the cache`() =
        runTest {
            val flags = slot<LoadUrlFlags>()
            val provider = provider(browserStore(loading = false))

            provider.onEvent(MenuAction.Navigate.Reload(bypassCache = true), menu)

            assertTrue(menu.isDismissed)
            verify { reload(tabId = TAB_ID, flags = capture(flags)) }
            assertEquals(LoadUrlFlags.select(LoadUrlFlags.BYPASS_CACHE).value, flags.captured.value)
        }

    @Test
    fun `GIVEN a selected tab WHEN clicking stop THEN close the menu and stop loading the page`() = runTest {
        val provider = provider(browserStore(loading = true))

        provider.onEvent(MenuAction.Navigate.Stop, menu)

        assertTrue(menu.isDismissed)
        verify { stopLoading(tabId = TAB_ID) }
    }

    @Test
    fun `GIVEN no selected tab WHEN clicking refresh or stop THEN keep the menu open`() = runTest {
        val provider = provider(BrowserStore())

        provider.onEvent(MenuAction.Navigate.Reload(bypassCache = false), menu)
        provider.onEvent(MenuAction.Navigate.Stop, menu)

        assertFalse(menu.isUsed)
        verify(exactly = 0) {
            reload(any(), any())
            stopLoading(any())
        }
    }

    @Test
    fun `WHEN the page is loading or not THEN handle all events the item can dispatch`() = runTest {
        listOf(browserStore(loading = true), browserStore(loading = false)).forEach { store ->
            val provider = provider(store)

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        }
    }

    @Test
    fun `WHEN asked about the events of other items THEN don't handle them`() = runTest {
        val provider = provider(browserStore(loading = false))

        assertFalse(provider.handles(MenuAction.Navigate.Back(viewHistory = false)))
        assertFalse(provider.handles(MenuAction.Navigate.Settings))
    }

    private fun TestScope.provider(
        browserStore: BrowserStore,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        RefreshMenuItemProvider(
            browserStore = browserStore,
            target = target,
            reload = reload,
            stopLoading = stopLoading,
            scope = backgroundScope,
        )

    private fun browserStore(loading: Boolean) =
        BrowserStore(
            BrowserState(
                tabs =
                    listOf(
                        TabSessionState(
                            id = TAB_ID,
                            content =
                                ContentState(
                                    url = "https://mozilla.org",
                                    loading = loading,
                                ),
                        )
                    ),
                selectedTabId = TAB_ID,
            )
        )

    private companion object {
        const val TAB_ID = "tab1"
    }
}
