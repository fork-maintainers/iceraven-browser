/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.menu

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import org.junit.Test
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.summarization.onboarding.SummarizationFeatureDiscoveryConfiguration
import org.mozilla.fenix.summarization.onboarding.SummarizeDiscoveryEvent

class MoreMenuItemsProviderTest {
    @Test
    fun `WHEN children are supplied THEN preserve their order in the expandable item`() = runTest {
        val children = listOf(translateItem, summarizeItem)

        assertEquals(children, provider().resolve(children).subMenuItems)
    }

    @Test
    fun `GIVEN summarize as a child WHEN this should be highlighted THEN highlight More`() = runTest {
        assertEquals(provider().resolve(listOf(summarizeItem)).icon?.isHighlighted, true)
    }

    @Test
    fun `GIVEN summarize as a child WHEN it is disabled THEN still highlight More`() = runTest {
        val item = summarizeItem.copy(state = MenuItemState.DISABLED)

        assertEquals(provider().resolve(listOf(item)).icon?.isHighlighted, true)
    }

    @Test
    fun `GIVEN summarize is not a child THEN don't highlight More`() = runTest {
        assertNotEquals(provider().resolve(listOf(translateItem)).icon?.isHighlighted, true)
    }

    @Test
    fun `GIVEN summarize as a child WHEN this should not be highlighted THEN don't highlight More`() = runTest {
        assertNotEquals(provider(highlight = false).resolve(listOf(summarizeItem)).icon?.isHighlighted, true)
    }

    @Test
    fun `GIVEN summarize as a child WHEN the current tab is private THEN don't highlight More`() = runTest {
        assertNotEquals(provider(isPrivate = true).resolve(listOf(summarizeItem)).icon?.isHighlighted, true)
    }

    @Test
    fun `GIVEN More is collapsed WHEN a submenu item is highlighted THEN highlight More`() = runTest {
        val highlighted = translateItem.copy(icon = MenuItemIconRes(0, isHighlighted = true))

        assertEquals(provider(highlight = false).resolve(listOf(highlighted)).icon?.isHighlighted, true)
    }

    @Test
    fun `GIVEN More is collapsed WHEN no submenu items are highlighted THEN don't highlight More`() = runTest {
        assertNotEquals(provider(highlight = false).resolve(listOf(translateItem)).icon?.isHighlighted, true)
    }

    @Test
    fun `WHEN adding submenu items to More THEN use the highlight status from the passed More item`() = runTest {
        val provider = provider(highlight = false)
        val captured = provider.itemFlow.value.copy(icon = MenuItemIconRes(123, isHighlighted = true))

        val shown = provider.updateWithSubMenuItems(captured, listOf(summarizeItem))

        assertEquals(true, shown.icon?.isHighlighted)
        assertEquals(listOf(summarizeItem), shown.subMenuItems)
    }

    private fun MoreMenuItemsProvider.resolve(children: List<StandardMenuItem>) =
        updateWithSubMenuItems(itemFlow.value, children)

    @Test
    fun `GIVEN it draws attention to summarizing WHEN expanding it THEN count summarizing as noticed`() = runTest {
        val settings = discoverySettings(highlight = true)
        val provider = provider(settings = settings)
        provider.resolve(listOf(translateItem, summarizeItem))

        provider.onEvent(MenuAction.OnMoreMenuClicked, FakeMenuHost())

        verify { settings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuOverflowInteraction) }
    }

    @Test
    fun `GIVEN it doesn't expand to summarizing WHEN expanding it THEN don't count summarizing as noticed`() = runTest {
        val settings = discoverySettings(highlight = true)
        val provider = provider(settings = settings)
        provider.resolve(listOf(translateItem))

        provider.onEvent(MenuAction.OnMoreMenuClicked, FakeMenuHost())

        verify(exactly = 0) { settings.cacheDiscoveryEvent(any()) }
    }

    @Test
    fun `GIVEN it doesn't draw attention to summarizing WHEN expanding it THEN don't count summarizing as noticed`() =
        runTest {
            val settings = discoverySettings(highlight = false)
            val provider = provider(settings = settings)
            provider.resolve(listOf(summarizeItem))

            provider.onEvent(MenuAction.OnMoreMenuClicked, FakeMenuHost())

            verify(exactly = 0) { settings.cacheDiscoveryEvent(any()) }
        }

    @Test
    fun `GIVEN a private tab WHEN expanding it THEN don't count summarizing as noticed`() = runTest {
        val settings = discoverySettings(highlight = true)
        val provider = provider(isPrivate = true, settings = settings)
        provider.resolve(listOf(summarizeItem))

        provider.onEvent(MenuAction.OnMoreMenuClicked, FakeMenuHost())

        verify(exactly = 0) { settings.cacheDiscoveryEvent(any()) }
    }

    @Test
    fun `WHEN expanding it THEN keep the menu open`() = runTest {
        val menu = FakeMenuHost()
        val provider = provider()
        provider.resolve(listOf(summarizeItem))

        provider.onEvent(MenuAction.OnMoreMenuClicked, menu)

        assertFalse(menu.isUsed)
    }

    @Test
    fun `WHEN building the item THEN handle only the events it dispatches itself`() = runTest {
        val provider = provider()

        val events = provider.itemFlow.value.reachableEvents()

        assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        assertFalse(provider.handles(MenuAction.Navigate.Summarizer))
    }

    private fun discoverySettings(highlight: Boolean): SummarizationFeatureDiscoveryConfiguration =
        mockk(relaxed = true) { every { shouldHighlightOverflowMenuItem } returns highlight }

    private fun TestScope.provider(
        highlight: Boolean = true,
        isPrivate: Boolean = false,
        hasTab: Boolean = true,
        settings: SummarizationFeatureDiscoveryConfiguration = discoverySettings(highlight),
        target: MenuTarget = MenuTarget.BrowserTab,
    ): MoreMenuItemsProvider {
        val tab = createTab(url = "https://mozilla.org", private = isPrivate)
        val browserStore = BrowserStore(BrowserState(tabs = listOf(tab), selectedTabId = tab.id.takeIf { hasTab }))
        return MoreMenuItemsProvider(
            browserStore,
            target,
            settings,
            backgroundScope,
        )
    }

    private companion object {
        val translateItem =
            StandardMenuItem(
                title = MenuItemTitle(Text.String("Translate")),
                onClickEvent = MenuAction.Navigate.Translate,
            )
        val summarizeItem =
            StandardMenuItem(
                title = MenuItemTitle(Text.String("Summarize")),
                onClickEvent = MenuAction.Navigate.Summarizer,
            )
    }
}
