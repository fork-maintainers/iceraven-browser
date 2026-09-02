/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.recentlyclosed

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.RecentlyClosedAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.recover.TabState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.recentlyclosed.RecentlyClosedTabsStorage
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.RecentlyClosedTabs
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.share.ShareSheetChooserAction
import org.mozilla.fenix.components.share.ShareSource
import org.mozilla.fenix.components.usecases.ShareUseCases
import org.mozilla.fenix.ext.directionsEq
import org.mozilla.fenix.ext.optionsEq
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class DefaultRecentlyClosedControllerTest {
    private val navController: NavController = mockk(relaxed = true)
    private var currentMode: BrowsingMode = BrowsingMode.Normal

    private val captureActionsMiddleware = CaptureActionsMiddleware<BrowserState, BrowserAction>()
    private val browserStore: BrowserStore = BrowserStore(middleware = listOf(captureActionsMiddleware))
    private val recentlyClosedStore: RecentlyClosedFragmentStore = mockk(relaxed = true)
    private val tabsUseCases: TabsUseCases = mockk(relaxed = true)
    private val shareUseCases: ShareUseCases = mockk(relaxed = true)

    @get:Rule
    val gleanTestRule = FenixGleanTestRule(testContext)

    @Before
    fun setUp() {
        coJustRun { tabsUseCases.restore.invoke(any(), any(), true) }
    }

    @Test
    fun handleOpen() {
        val item: TabState = mockk(relaxed = true)

        var tabUrl: String? = null

        val controller = createController(
            openToBrowser = { url ->
                tabUrl = url
            },
        )

        controller.handleOpen(item)
        assertEquals(item.url, tabUrl)
    }

    @Test
    fun `open multiple tabs`() {
        val tabs = createFakeTabList(2)

        val tabUrls = mutableListOf<String>()

        currentMode = BrowsingMode.Normal
        var controller = createController(
            openToBrowser = { url -> tabUrls.add(url) },
        )
        assertNull(RecentlyClosedTabs.menuOpenInNormalTab.testGetValue())

        controller.handleOpen(tabs.toSet())

        assertEquals(2, tabUrls.size)
        assertEquals(tabs[0].url, tabUrls[0])
        assertEquals(tabs[1].url, tabUrls[1])
        assertNotNull(RecentlyClosedTabs.menuOpenInNormalTab.testGetValue())
        assertNull(RecentlyClosedTabs.menuOpenInNormalTab.testGetValue()!!.last().extra)

        tabUrls.clear()

        currentMode = BrowsingMode.Private
        controller = createController(
            openToBrowser = { url -> tabUrls.add(url) },
        )
        assertNull(RecentlyClosedTabs.menuOpenInPrivateTab.testGetValue())

        controller.handleOpen(tabs.toSet())

        assertEquals(2, tabUrls.size)
        assertEquals(tabs[0].url, tabUrls[0])
        assertEquals(tabs[1].url, tabUrls[1])
        assertNotNull(RecentlyClosedTabs.menuOpenInPrivateTab.testGetValue())
        assertEquals(1, RecentlyClosedTabs.menuOpenInPrivateTab.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.menuOpenInPrivateTab.testGetValue()!!.single().extra)
    }

    @Test
    fun `handle selecting first tab`() {
        val selectedTab = createFakeTab()
        every { recentlyClosedStore.state.selectedTabs } returns emptySet()
        assertNull(RecentlyClosedTabs.enterMultiselect.testGetValue())

        createController().handleSelect(selectedTab)

        verify { recentlyClosedStore.dispatch(RecentlyClosedFragmentAction.Select(selectedTab)) }
        assertNotNull(RecentlyClosedTabs.enterMultiselect.testGetValue())
        assertEquals(1, RecentlyClosedTabs.enterMultiselect.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.enterMultiselect.testGetValue()!!.single().extra)
    }

    @Test
    fun `handle selecting a successive tab`() {
        val selectedTab = createFakeTab()
        every { recentlyClosedStore.state.selectedTabs } returns setOf(mockk())

        createController().handleSelect(selectedTab)

        verify { recentlyClosedStore.dispatch(RecentlyClosedFragmentAction.Select(selectedTab)) }
        assertNull(RecentlyClosedTabs.enterMultiselect.testGetValue())
    }

    @Test
    fun `handle deselect last tab`() {
        val deselectedTab = createFakeTab()
        every { recentlyClosedStore.state.selectedTabs } returns setOf(deselectedTab)
        assertNull(RecentlyClosedTabs.exitMultiselect.testGetValue())

        createController().handleDeselect(deselectedTab)

        verify { recentlyClosedStore.dispatch(RecentlyClosedFragmentAction.Deselect(deselectedTab)) }
        assertNotNull(RecentlyClosedTabs.exitMultiselect.testGetValue())
        assertEquals(1, RecentlyClosedTabs.exitMultiselect.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.exitMultiselect.testGetValue()!!.single().extra)
    }

    @Test
    fun `handle deselect a tab from others still selected`() {
        val deselectedTab = createFakeTab()
        every { recentlyClosedStore.state.selectedTabs } returns setOf(deselectedTab, mockk())

        createController().handleDeselect(deselectedTab)

        verify { recentlyClosedStore.dispatch(RecentlyClosedFragmentAction.Deselect(deselectedTab)) }
        assertNull(RecentlyClosedTabs.exitMultiselect.testGetValue())
    }

    @Test
    fun handleDelete() {
        val item: TabState = mockk(relaxed = true)
        assertNull(RecentlyClosedTabs.deleteTab.testGetValue())

        createController().handleDelete(item)

        captureActionsMiddleware.assertFirstAction(RecentlyClosedAction.RemoveClosedTabAction::class) { action ->
            assertEquals(item, action.tab)
        }

        assertNotNull(RecentlyClosedTabs.deleteTab.testGetValue())
        assertEquals(1, RecentlyClosedTabs.deleteTab.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.deleteTab.testGetValue()!!.single().extra)
    }

    @Test
    fun `delete multiple tabs`() {
        val tabs = createFakeTabList(2)
        assertNull(RecentlyClosedTabs.menuDelete.testGetValue())

        createController().handleDelete(tabs.toSet())

        captureActionsMiddleware.assertFirstAction(RecentlyClosedAction.RemoveClosedTabAction::class) { action ->
            assertEquals(tabs[0], action.tab)
        }

        captureActionsMiddleware.assertLastAction(RecentlyClosedAction.RemoveClosedTabAction::class) { action ->
            assertEquals(tabs[1], action.tab)
        }

        assertNotNull(RecentlyClosedTabs.menuDelete.testGetValue())
        assertNull(RecentlyClosedTabs.menuDelete.testGetValue()!!.last().extra)
    }

    @Test
    fun handleNavigateToHistory() {
        assertNull(RecentlyClosedTabs.showFullHistory.testGetValue())

        createController().handleNavigateToHistory()

        verify {
            navController.navigate(
                directionsEq(
                    RecentlyClosedFragmentDirections.actionGlobalHistoryFragment(),
                ),
                optionsEq(NavOptions.Builder().setPopUpTo(R.id.historyFragment, true).build()),
            )
        }
        assertNotNull(RecentlyClosedTabs.showFullHistory.testGetValue())
        assertEquals(1, RecentlyClosedTabs.showFullHistory.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.showFullHistory.testGetValue()!!.single().extra)
    }

    @Test
    fun `WHEN a single tab is shared THEN record telemetry and call share use cases with send to devices and QR code chooser actions`() {
        val tabs = createFakeTabList(1)
        assertNull(RecentlyClosedTabs.menuShare.testGetValue())

        createController().handleShare(tabs.toSet())

        val items = listOf(ShareData(url = tabs[0].url, title = tabs[0].title))
        verify {
            shareUseCases.shareItems(
                items = items,
                source = ShareSource.RECENTLY_CLOSED,
                chooserActions = listOf(ShareSheetChooserAction.SEND_TO_DEVICES, ShareSheetChooserAction.QR_CODE),
                navigateToShareFragment = any(),
            )
        }
        assertNotNull(RecentlyClosedTabs.menuShare.testGetValue())
        assertEquals(1, RecentlyClosedTabs.menuShare.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.menuShare.testGetValue()!!.single().extra)
    }

    @Test
    fun `WHEN multiple tabs are shared THEN record telemetry and call share use cases with send to devices chooser action`() {
        val tabs = createFakeTabList(2)
        assertNull(RecentlyClosedTabs.menuShare.testGetValue())

        createController().handleShare(tabs.toSet())

        val items = listOf(
            ShareData(url = tabs[0].url, title = tabs[0].title),
            ShareData(url = tabs[1].url, title = tabs[1].title),
        )
        verify {
            shareUseCases.shareItems(
                items = items,
                source = ShareSource.RECENTLY_CLOSED,
                chooserActions = listOf(ShareSheetChooserAction.SEND_TO_DEVICES),
                navigateToShareFragment = any(),
            )
        }
        assertNotNull(RecentlyClosedTabs.menuShare.testGetValue())
        assertEquals(1, RecentlyClosedTabs.menuShare.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.menuShare.testGetValue()!!.single().extra)
    }

    @Test
    fun handleRestore() = runTest {
        val item: TabState = mockk(relaxed = true)
        currentMode = BrowsingMode.Normal

        assertNull(RecentlyClosedTabs.openTab.testGetValue())

        createController(scope = this).handleRestore(item)
        testScheduler.advanceUntilIdle()

        coVerify { tabsUseCases.restore.invoke(eq(item), any(), true) }
        verify { navController.navigate(R.id.browserFragment) }
        assertNotNull(RecentlyClosedTabs.openTab.testGetValue())
        assertEquals(1, RecentlyClosedTabs.openTab.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.openTab.testGetValue()!!.single().extra)
    }

    @Test
    fun `GIVEN normal browsing mode WHEN handleRestore is called THEN restore and nav to browser`() = runTest {
        val item: TabState = mockk(relaxed = true)
        val controller = createController(scope = this)

        assertNull(RecentlyClosedTabs.openTab.testGetValue())

        currentMode = BrowsingMode.Normal

        controller.handleRestore(item)

        testScheduler.advanceUntilIdle()

        coVerify { tabsUseCases.restore.invoke(eq(item), any(), true) }

        captureActionsMiddleware.assertFirstAction(RecentlyClosedAction.RemoveClosedTabAction::class) { action ->
            assertEquals(item, action.tab)
        }

        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun `GIVEN private browsing mode WHEN handleRestore is called THEN handleOpen is invoked with private mode`() = runTest {
        val item: TabState = mockk(relaxed = true)
        var capturedUrl: String? = null
        currentMode = BrowsingMode.Private
        val controller = createController(
            scope = this,
            openToBrowser = { url ->
                capturedUrl = url
            },
        )

        assertNull(RecentlyClosedTabs.openTab.testGetValue())

        controller.handleRestore(item)

        testScheduler.advanceUntilIdle()

        assertEquals(item.url, capturedUrl)
    }

    @Test
    fun `exist multi-select mode when back is pressed`() {
        every { recentlyClosedStore.state.selectedTabs } returns createFakeTabList(3).toSet()
        assertNull(RecentlyClosedTabs.exitMultiselect.testGetValue())

        createController().handleBackPressed()

        verify { recentlyClosedStore.dispatch(RecentlyClosedFragmentAction.DeselectAll) }
        assertNotNull(RecentlyClosedTabs.exitMultiselect.testGetValue())
        assertEquals(1, RecentlyClosedTabs.exitMultiselect.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.exitMultiselect.testGetValue()!!.single().extra)
    }

    @Test
    fun `report closing the fragment when back is pressed`() {
        every { recentlyClosedStore.state.selectedTabs } returns emptySet()
        assertNull(RecentlyClosedTabs.closed.testGetValue())

        createController().handleBackPressed()

        verify(exactly = 0) { recentlyClosedStore.dispatch(RecentlyClosedFragmentAction.DeselectAll) }
        assertNotNull(RecentlyClosedTabs.closed.testGetValue())
        assertEquals(1, RecentlyClosedTabs.closed.testGetValue()!!.size)
        assertNull(RecentlyClosedTabs.closed.testGetValue()!!.single().extra)
    }

    private fun createController(
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        openToBrowser: (String) -> Unit = { _ -> },
    ): RecentlyClosedController {
        val appStore = AppStore(initialState = AppState(mode = currentMode))
        return DefaultRecentlyClosedController(
            appStore = appStore,
            navController = navController,
            browserStore = browserStore,
            recentlyClosedStore = recentlyClosedStore,
            recentlyClosedTabsStorage = RecentlyClosedTabsStorage(testContext, mockk(), mockk()),
            tabsUseCases = tabsUseCases,
            shareUseCases = shareUseCases,
            lifecycleScope = scope,
            openToBrowser = openToBrowser,
        )
    }

    private fun createFakeTab(id: String = "FakeId", url: String = "www.fake.com"): TabState =
        TabState(id, url)

    private fun createFakeTabList(size: Int): List<TabState> {
        val fakeTabs = mutableListOf<TabState>()
        for (i in 0 until size) {
            fakeTabs.add(createFakeTab(id = "FakeId$i"))
        }

        return fakeTabs
    }
}
