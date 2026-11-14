/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.bookmarks

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.concept.storage.BookmarksStorage
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction.BookmarkAction
import org.mozilla.fenix.components.bookmarks.BookmarksUseCase
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.components.metrics.MetricsUtils

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkMenuItemProviderTest {
    private val addBookmark: BookmarksUseCase.AddBookmarksUseCase = mockk()
    private val appStore: AppStore = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN the current page is not bookmarked WHEN providing the item THEN offer bookmarking it`() = runTest {
        val provider = provider(bookmarksStorage = storageWith(bookmark = null))

        assertEquals(addBookmarkItem, provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the current page is bookmarked WHEN providing the item THEN offer editing that bookmark`() = runTest {
        val provider = provider(bookmarksStorage = storageWith(bookmark = bookmark))

        // Bookmarking the page is offered before reading from disk, editing the bookmark once it is known.
        assertEquals(addBookmarkItem, provider.itemFlow.value)

        runCurrent()

        assertEquals(editBookmarkItem, provider.itemFlow.value)
    }

    @Test
    fun `GIVEN a custom tab page is bookmarked WHEN providing the item THEN offer editing that bookmark`() = runTest {
        val customTab = createCustomTab(url = TEST_URL, title = TEST_TITLE, id = "customTabId")
        val browserStore = BrowserStore(BrowserState(customTabs = listOf(customTab)))
        val provider =
            provider(
                browserStore = browserStore,
                bookmarksStorage = storageWith(bookmark = bookmark),
                target = MenuTarget.CustomTab("customTabId"),
            )

        assertEquals(addBookmarkItem, provider.itemFlow.value)

        runCurrent()

        assertEquals(editBookmarkItem, provider.itemFlow.value)
    }

    @Test
    fun `GIVEN there is no page shown WHEN providing the item THEN don't offer bookmarking`() = runTest {
        val provider = provider(browserStore = BrowserStore(), bookmarksStorage = storageWith(bookmark = null))

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the current page is not bookmarked WHEN clicking the item THEN bookmark it and close the menu`() =
        runTest {
            coEvery { addBookmark(url = TEST_URL, title = TEST_TITLE) } returns
                BookmarksUseCase.AddBookmarksUseCase.Result(guidToEdit = BOOKMARK_GUID, parentNode = null)
            val provider = provider(bookmarksStorage = storageWith(bookmark = null))

            provider.onEvent(MenuAction.AddBookmark, menu)
            runCurrent()

            verify {
                appStore.dispatch(
                    BookmarkAction.BookmarkAdded(
                        guidToEdit = BOOKMARK_GUID,
                        parentNode = null,
                        source = MetricsUtils.BookmarkAction.Source.MENU_DIALOG,
                    )
                )
            }
            assertTrue(menu.isDismissed)
        }

    @Test
    fun `GIVEN a custom tab WHEN tapping bookmark THEN bookmark the custom tab page URL`() = runTest {
        val customUrl = "https://example.com/custom"
        val customTitle = "Custom Page"
        val customTabId = "customTabId"

        coEvery { addBookmark(url = customUrl, title = customTitle) } returns
            BookmarksUseCase.AddBookmarksUseCase.Result(guidToEdit = BOOKMARK_GUID, parentNode = null)

        val customTab = createCustomTab(url = customUrl, title = customTitle, id = customTabId)
        val store = BrowserStore(BrowserState(customTabs = listOf(customTab)))
        val provider =
            provider(
                browserStore = store,
                bookmarksStorage = storageWith(bookmark = null),
                target = MenuTarget.CustomTab(customTabId),
            )

        provider.onEvent(MenuAction.AddBookmark, menu)
        runCurrent()

        coVerify { addBookmark(url = customUrl, title = customTitle) }
        verify {
            appStore.dispatch(
                BookmarkAction.BookmarkAdded(
                    guidToEdit = BOOKMARK_GUID,
                    parentNode = null,
                    source = MetricsUtils.BookmarkAction.Source.MENU_DIALOG,
                )
            )
        }
        assertTrue(menu.isDismissed)
    }

    @Test
    fun `GIVEN no selected tab WHEN clicking the item THEN keep the menu open`() = runTest {
        val provider = provider(browserStore = BrowserStore(), bookmarksStorage = storageWith(bookmark = null))

        provider.onEvent(MenuAction.AddBookmark, menu)
        runCurrent()

        coVerify(exactly = 0) { addBookmark(any(), any(), any(), any()) }
        assertFalse(menu.isUsed)
    }

    @Test
    fun `GIVEN the current page is bookmarked WHEN clicking the item THEN show the bookmark editor instead`() =
        runTest {
            val provider = provider(bookmarksStorage = storageWith(bookmark = bookmark))

            provider.onEvent(MenuAction.Navigate.EditBookmark(guidToEdit = BOOKMARK_GUID), menu)

            assertEquals(
                NavGraphDirections.actionGlobalBookmarkEditFragment(
                    guidToEdit = BOOKMARK_GUID,
                    requiresSnackbarPaddingForToolbar = true,
                ),
                menu.directions,
            )
        }

    @Test
    fun `GIVEN the bookmark to edit is not known WHEN asked to edit it THEN keep the menu open`() = runTest {
        val provider = provider(bookmarksStorage = storageWith(bookmark = bookmark))

        provider.onEvent(MenuAction.Navigate.EditBookmark(guidToEdit = null), menu)

        assertFalse(menu.isUsed)
    }

    @Test
    fun `WHEN the page is bookmarked or not THEN handle all events the item can dispatch and no others`() = runTest {
        listOf(null, bookmark).forEach { bookmark ->
            val provider = provider(bookmarksStorage = storageWith(bookmark = bookmark))
            runCurrent()

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
            assertFalse(provider.handles(MenuAction.FindInPage))
        }
    }

    private fun TestScope.provider(
        browserStore: BrowserStore = browserStoreWithSelectedTab(),
        bookmarksStorage: BookmarksStorage,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        BookmarkMenuItemProvider(
            browserStore = browserStore,
            target = target,
            bookmarksStorage = bookmarksStorage,
            addBookmark = addBookmark,
            appStore = appStore,
            // The bookmarks are read and added on this scope, which runTest runs and then cancels.
            scope = this,
            applicationScope = this,
        )

    private fun storageWith(bookmark: BookmarkNode?): BookmarksStorage = mockk {
        coEvery { getBookmarksWithUrl(any()) } returns Result.success(listOfNotNull(bookmark))
    }

    private fun browserStoreWithSelectedTab() =
        BrowserStore(
            BrowserState(
                tabs = listOf(createTab(url = TEST_URL, title = TEST_TITLE, id = TAB_ID)),
                selectedTabId = TAB_ID,
            )
        )

    private companion object {
        const val TEST_URL = "https://mozilla.org"
        const val TEST_TITLE = "Mozilla"
        const val TAB_ID = "tab1"
        const val BOOKMARK_GUID = "bookmarkGuid"

        val bookmark =
            BookmarkNode(
                type = BookmarkNodeType.ITEM,
                guid = BOOKMARK_GUID,
                parentGuid = null,
                position = null,
                title = TEST_TITLE,
                url = TEST_URL,
                dateAdded = 0,
                lastModified = 0,
                children = null,
            )

        val addBookmarkItem =
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_bookmark_this_page_2)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_bookmark_24),
                onClickEvent = MenuAction.AddBookmark,
            )

        val editBookmarkItem =
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_edit_bookmark)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_bookmark_fill_24),
                onClickEvent = MenuAction.Navigate.EditBookmark(guidToEdit = BOOKMARK_GUID),
                state = MenuItemState.ACTIVE,
            )
    }
}
