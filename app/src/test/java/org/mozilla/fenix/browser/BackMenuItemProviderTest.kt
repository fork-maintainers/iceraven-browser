/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import androidx.test.ext.junit.runners.AndroidJUnit4
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
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuFragmentDirections
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.utils.Stories.markAsOpenedFromHomeScreen
import org.mozilla.fenix.utils.Stories.markAsOpenedFromStoriesScreen

@RunWith(AndroidJUnit4::class)
class BackMenuItemProviderTest {
    private val goBack: SessionUseCases.GoBackUseCase = mockk(relaxed = true)
    private val settings: Settings = mockk { every { enableHomepageAsNewTab } returns false }
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN there is no selected tab WHEN building the back menu item THEN return a disabled menu item`() = runTest {
        val provider = createProvider(BrowserStore(), backgroundScope)

        assertEquals(expectedItem(canGoBack = false), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the current page cannot navigate back WHEN building the back menu item THEN return a disabled menu item`() =
        runTest {
            val provider = createProvider(browserStore(canGoBack = false), backgroundScope)

            assertEquals(expectedItem(canGoBack = false), provider.itemFlow.value)
        }

    @Test
    fun `GIVEN the current page can navigate back WHEN building the back menu item THEN return an enabled menu item`() =
        runTest {
            val provider = createProvider(browserStore(canGoBack = true), backgroundScope)

            assertEquals(expectedItem(canGoBack = true), provider.itemFlow.value)
        }

    @Test
    fun `GIVEN a selected tab WHEN clicking the item THEN close the menu and navigate back in that tab`() = runTest {
        var isDismissedWhenGoingBack = false
        every { goBack(tabId = TAB_ID) } answers { isDismissedWhenGoingBack = menu.isDismissed }
        val provider = createProvider(browserStore(canGoBack = true), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

        verify { goBack(tabId = TAB_ID) }
        assertTrue(isDismissedWhenGoingBack)
    }

    @Test
    fun `GIVEN a story opened from the home screen WHEN clicking the item THEN go back to the home screen`() = runTest {
        val menu = FakeMenuHost(previousScreens = setOf(R.id.homeFragment))
        val provider = createProvider(browserStore(url = HOME_SCREEN_STORY_URL), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

        assertEquals(R.id.homeFragment, menu.backDestinationId)
        verify(exactly = 0) { goBack(any()) }
    }

    @Test
    fun `GIVEN a story opened from the home screen that was since closed WHEN clicking the item THEN go back to the home screen`() =
        runTest {
            val provider = createProvider(browserStore(url = HOME_SCREEN_STORY_URL), backgroundScope)

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

            assertEquals(NavGraphDirections.actionGlobalHome(), menu.directions)
            verify(exactly = 0) { goBack(any()) }
        }

    @Test
    fun `GIVEN a story opened from the stories screen WHEN clicking the item THEN go back to the stories screen`() =
        runTest {
            val menu = FakeMenuHost(previousScreens = setOf(R.id.homeFragment, R.id.storiesFragment))
            val provider = createProvider(browserStore(url = STORIES_SCREEN_STORY_URL), backgroundScope)

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

            assertEquals(R.id.storiesFragment, menu.backDestinationId)
            verify(exactly = 0) { goBack(any()) }
        }

    @Test
    fun `GIVEN a story opened from the stories screen that was since closed WHEN clicking the item THEN go back to the stories screen`() =
        runTest {
            val provider = createProvider(browserStore(url = STORIES_SCREEN_STORY_URL), backgroundScope)

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

            assertEquals(MenuFragmentDirections.actionMenuFragmentToStoriesFragment(), menu.directions)
            verify(exactly = 0) { goBack(any()) }
        }

    @Test
    fun `GIVEN the homepage is shown in a tab and a story opened from it WHEN clicking the item THEN close the menu and navigate back in that tab`() =
        runTest {
            every { settings.enableHomepageAsNewTab } returns true
            val menu = FakeMenuHost(previousScreens = setOf(R.id.homeFragment))
            val provider = createProvider(browserStore(url = HOME_SCREEN_STORY_URL), backgroundScope)

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

            verify { goBack(tabId = TAB_ID) }
            assertTrue(menu.isDismissed)
        }

    @Test
    fun `GIVEN the homepage is shown in a tab and a story opened from the stories screen WHEN clicking the item THEN go back to the stories screen`() =
        runTest {
            every { settings.enableHomepageAsNewTab } returns true
            val menu = FakeMenuHost(previousScreens = setOf(R.id.homeFragment, R.id.storiesFragment))
            val provider = createProvider(browserStore(url = STORIES_SCREEN_STORY_URL), backgroundScope)

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

            assertEquals(R.id.storiesFragment, menu.backDestinationId)
            verify(exactly = 0) { goBack(any()) }
        }

    @Test
    fun `GIVEN the homepage is shown in a tab restored from a previous session and a story opened from the stories screen WHEN clicking the item THEN go back to the stories screen`() =
        runTest {
            every { settings.enableHomepageAsNewTab } returns true
            val menu = FakeMenuHost(previousScreens = setOf(R.id.homeFragment, R.id.storiesFragment))
            val provider =
                createProvider(browserStore(url = STORIES_SCREEN_STORY_URL, restored = true), backgroundScope)

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

            assertEquals(R.id.storiesFragment, menu.backDestinationId)
            verify(exactly = 0) { goBack(any()) }
        }

    @Test
    fun `GIVEN the homepage is shown in a tab and a story opened from a stories screen that was since closed WHEN clicking the item THEN close the menu and navigate back in that tab`() =
        runTest {
            every { settings.enableHomepageAsNewTab } returns true
            val provider = createProvider(browserStore(url = STORIES_SCREEN_STORY_URL), backgroundScope)

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

            verify { goBack(tabId = TAB_ID) }
            assertTrue(menu.isDismissed)
        }

    @Test
    fun `GIVEN a custom tab showing a story WHEN clicking the item THEN close the menu and navigate back in that custom tab`() =
        runTest {
            val menu = FakeMenuHost(previousScreens = setOf(R.id.homeFragment))
            val provider =
                createProvider(
                    customTabBrowserStore(canGoBack = true, url = HOME_SCREEN_STORY_URL),
                    backgroundScope,
                    target = MenuTarget.CustomTab("customTabId"),
                )

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

            verify { goBack(tabId = "customTabId") }
            assertTrue(menu.isDismissed)
        }

    @Test
    fun `GIVEN a selected tab WHEN long clicking the item THEN show its history in place of the menu`() = runTest {
        val provider = createProvider(browserStore(canGoBack = true), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Back(viewHistory = true), menu)

        assertEquals(NavGraphDirections.actionGlobalTabHistoryDialogFragment(activeSessionId = null), menu.directions)
        verify(exactly = 0) { goBack(any()) }
    }

    @Test
    fun `GIVEN a custom tab WHEN long clicking the item THEN show its history with custom tab session ID in place of the menu`() =
        runTest {
            val provider =
                createProvider(
                    customTabBrowserStore(canGoBack = true),
                    backgroundScope,
                    target = MenuTarget.CustomTab("customTabId"),
                )

            provider.onEvent(MenuAction.Navigate.Back(viewHistory = true), menu)

            assertEquals(
                NavGraphDirections.actionGlobalTabHistoryDialogFragment(activeSessionId = "customTabId"),
                menu.directions,
            )
            verify(exactly = 0) { goBack(any()) }
        }

    @Test
    fun `GIVEN no selected tab WHEN clicking the item THEN keep the menu open`() = runTest {
        val provider = createProvider(BrowserStore(), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Back(viewHistory = false), menu)

        assertFalse(menu.isUsed)
        verify(exactly = 0) { goBack(any()) }
    }

    @Test
    fun `GIVEN no selected tab WHEN long clicking the item THEN keep the menu open`() = runTest {
        val provider = createProvider(BrowserStore(), backgroundScope)

        provider.onEvent(MenuAction.Navigate.Back(viewHistory = true), menu)

        assertFalse(menu.isUsed)
    }

    @Test
    fun `WHEN the item can or cannot navigate back THEN handle all events it can dispatch`() = runTest {
        listOf(BrowserStore(), browserStore(canGoBack = false), browserStore(canGoBack = true)).forEach { store ->
            val provider = createProvider(store, backgroundScope)

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        }
    }

    @Test
    fun `WHEN asked about the events of other items THEN don't handle them`() = runTest {
        val provider = createProvider(browserStore(canGoBack = true), backgroundScope)

        assertFalse(provider.handles(MenuAction.Navigate.Forward(viewHistory = false)))
        assertFalse(provider.handles(MenuAction.Navigate.Settings))
    }

    private fun createProvider(
        browserStore: BrowserStore,
        scope: CoroutineScope,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        BackMenuItemProvider(
            browserStore = browserStore,
            target = target,
            goBack = goBack,
            settings = settings,
            scope = scope,
        )

    private fun browserStore(
        canGoBack: Boolean = false,
        url: String = "https://mozilla.org",
        restored: Boolean = false,
    ) =
        BrowserStore(
            BrowserState(
                tabs =
                    listOf(
                        TabSessionState(
                            id = TAB_ID,
                            content =
                                ContentState(
                                    url = url,
                                    canGoBack = canGoBack,
                                ),
                            restored = restored,
                        )
                    ),
                selectedTabId = TAB_ID,
            )
        )

    private fun customTabBrowserStore(
        canGoBack: Boolean,
        url: String = "https://custom.org",
    ) =
        BrowserStore(
            BrowserState(
                customTabs =
                    listOf(
                        createCustomTab(
                                url = url,
                                id = "customTabId",
                            )
                            .let { tab ->
                                tab.copy(content = tab.content.copy(canGoBack = canGoBack))
                            }
                    ),
                selectedTabId = null,
            )
        )

    private fun expectedItem(canGoBack: Boolean) =
        StandardMenuItem(
            title = MenuItemTitle(Text.Resource(R.string.browser_menu_back)),
            icon = MenuItemIconRes(iconsR.drawable.mozac_ic_back_24),
            onClickEvent = MenuAction.Navigate.Back(viewHistory = false),
            onLongClickEvent = MenuAction.Navigate.Back(viewHistory = true),
            state = if (canGoBack) MenuItemState.DEFAULT else MenuItemState.DISABLED,
        )

    private companion object {
        const val TAB_ID = "tab1"
        val HOME_SCREEN_STORY_URL = "https://mozilla.org/story".markAsOpenedFromHomeScreen()
        val STORIES_SCREEN_STORY_URL = "https://mozilla.org/story".markAsOpenedFromStoriesScreen()
    }
}
