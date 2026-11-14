/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.utils.Settings

@RunWith(AndroidJUnit4::class)
class SaveToCollectionMenuItemProviderTest {
    private val browserStore =
        BrowserStore(BrowserState(tabs = listOf(createTab("https://mozilla.org", id = TAB_ID)), selectedTabId = TAB_ID))
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN the collections feature is disabled WHEN building the menu button THEN return null`() {
        val settings: Settings = mockk {
            every { collections } returns false
        }

        val provider = SaveToCollectionMenuItemProvider(settings, mockk(), browserStore, MenuTarget.BrowserTab)

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the collections feature is enabled and collections exist WHEN building the menu button THEN return a menu item`() {
        val settings: Settings = mockk {
            every { collections } returns true
        }
        val tabCollectionStorage: TabCollectionStorage = mockk {
            every { cachedTabCollections } returns listOf(mockk())
        }

        val provider =
            SaveToCollectionMenuItemProvider(
                settings,
                tabCollectionStorage,
                browserStore,
                MenuTarget.BrowserTab,
            )

        assertEquals(expectedMenuItem(true), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the collections feature is enabled and collections don't exist WHEN building the menu button THEN return a menu item`() {
        val settings: Settings = mockk {
            every { collections } returns true
        }
        val tabCollectionStorage: TabCollectionStorage = mockk {
            every { cachedTabCollections } returns emptyList()
        }

        val provider =
            SaveToCollectionMenuItemProvider(
                settings,
                tabCollectionStorage,
                browserStore,
                MenuTarget.BrowserTab,
            )

        assertEquals(expectedMenuItem(false), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN collections already exist WHEN clicking the item THEN ask to which one to add the current tab`() {
        val provider = createProvider(collectionsExist = true)

        provider.onEvent(MenuAction.Navigate.SaveToCollection(hasCollection = true), menu)

        assertCollectionCreationShown(SaveCollectionStep.SelectCollection)
    }

    @Test
    fun `GIVEN no collections exist WHEN clicking the item THEN ask for a name for a new one with the current tab`() {
        val provider = createProvider(collectionsExist = false)

        provider.onEvent(MenuAction.Navigate.SaveToCollection(hasCollection = false), menu)

        assertCollectionCreationShown(SaveCollectionStep.NameCollection)
    }

    @Test
    fun `GIVEN no selected tab WHEN clicking the item THEN keep the menu open`() {
        val provider = createProvider(collectionsExist = true, browserStore = BrowserStore())

        provider.onEvent(MenuAction.Navigate.SaveToCollection(hasCollection = true), menu)

        assertFalse(menu.isUsed)
    }

    @Test
    fun `WHEN collections exist or not THEN handle all events the item can dispatch and no others`() {
        listOf(true, false).forEach { collectionsExist ->
            val provider = createProvider(collectionsExist = collectionsExist)

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
            assertFalse(provider.handles(MenuAction.Navigate.Settings))
        }
    }

    private fun assertCollectionCreationShown(step: SaveCollectionStep) {
        val directions = requireNotNull(menu.directions)
        assertEquals(R.id.action_global_collectionCreationFragment, directions.actionId)
        val args = CollectionCreationFragmentArgs.fromBundle(directions.arguments)
        assertContentEquals(arrayOf(TAB_ID), args.tabIds)
        assertContentEquals(arrayOf(TAB_ID), args.selectedTabIds)
        assertEquals(step, args.saveCollectionStep)
    }

    @Test
    fun `GIVEN the menu was opened from home WHEN using the item THEN ignore the selected tab`() {
        val provider = createProvider(collectionsExist = true, target = MenuTarget.Home)

        provider.onEvent(MenuAction.Navigate.SaveToCollection(hasCollection = true), menu)

        assertFalse(menu.isUsed)
    }

    private fun createProvider(
        collectionsExist: Boolean,
        browserStore: BrowserStore = this.browserStore,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        SaveToCollectionMenuItemProvider(
            settings = mockk { every { collections } returns true },
            tabCollectionStorage =
                mockk { every { cachedTabCollections } returns if (collectionsExist) listOf(mockk()) else emptyList() },
            browserStore = browserStore,
            target = target,
        )

    private fun expectedMenuItem(collectionsAlreadyExist: Boolean) =
        StandardMenuItem(
            title = MenuItemTitle(Text.Resource(R.string.browser_menu_save_to_collection_2)),
            icon = MenuItemIconRes(iconsR.drawable.mozac_ic_collection_24),
            onClickEvent = MenuAction.Navigate.SaveToCollection(hasCollection = collectionsAlreadyExist),
        )

    private companion object {
        const val TAB_ID = "tab1"
    }
}
