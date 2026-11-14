/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.menu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.ExpandableMenuItem
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.store.MenuEvent
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.ExpandableMenuItemProvider
import org.mozilla.fenix.components.menu.MenuHost
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.summarization.isSummarizePageMenuItem
import org.mozilla.fenix.summarization.onboarding.SummarizationFeatureDiscoveryConfiguration
import org.mozilla.fenix.summarization.onboarding.SummarizeDiscoveryEvent

/**
 * [ExpandableMenuItemProvider] for the menu item expanding to show more general menu items related to the current
 * webpage.
 *
 * @param browserStore [BrowserStore] used to know for which webpage the menu is opened.
 * @param target [MenuTarget] for which this menu item would be shown for.
 * @param summarizationSettings [SummarizationFeatureDiscoveryConfiguration] for checking the summarization feature
 *   status.
 * @param scope [CoroutineScope] used to keep the item up to date for as long as it can be shown.
 */
class MoreMenuItemsProvider(
    private val browserStore: BrowserStore,
    private val target: MenuTarget,
    private val summarizationSettings: SummarizationFeatureDiscoveryConfiguration,
    scope: CoroutineScope,
) : ExpandableMenuItemProvider {
    // What this currently expands to, as last configured by the menu.
    private var lastSubMenuItems: List<StandardMenuItem> = emptyList()

    override val itemFlow: StateFlow<ExpandableMenuItem> =
        browserStore.stateFlow
            .map { state ->
                moreItem(isNormalTab = target.browserSessionFrom(state)?.content?.private == false)
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue =
                    moreItem(isNormalTab = target.browserSessionFrom(browserStore.state)?.content?.private == false),
            )

    private fun moreItem(isNormalTab: Boolean) =
        ExpandableMenuItem(
            title = MenuItemTitle(Text.Resource(R.string.browser_menu_more_settings)),
            icon = icon(isHighlighted = isNormalTab && summarizationSettings.shouldHighlightOverflowMenuItem),
            subMenuItems = emptyList(),
            onClickEvent = MenuAction.OnMoreMenuClicked,
            hideOnExpand = true,
        )

    /**
     * Update the "More" menu item with a new list of submenu items to show when expanded. This supports the scenario in
     * which the list of submenu items is known only later from a separate source.
     *
     * @param item The "More" item header captured alongside the children in the menu builder's snapshot.
     * @param subMenuItems The new list of submenu items that this menu item should show when expanded.
     */
    override fun updateWithSubMenuItems(
        item: ExpandableMenuItem,
        subMenuItems: List<StandardMenuItem>,
    ): ExpandableMenuItem {
        lastSubMenuItems = subMenuItems

        // "Summarize page" has different rules for highlighting itself vs the "More" header.
        val shouldHighlightForSummarize =
            item.icon?.isHighlighted == true && subMenuItems.any { it.isSummarizePageMenuItem() }
        // Highlight "More" when a child other than Summarize is highlighted.
        val shouldHighlightForOtherItems = subMenuItems.any {
            !it.isSummarizePageMenuItem() && it.icon?.isHighlighted == true
        }

        return item.copy(
            subMenuItems = subMenuItems,
            icon = icon(isHighlighted = shouldHighlightForSummarize || shouldHighlightForOtherItems),
        )
    }

    override fun handles(event: MenuEvent) = event == MenuAction.OnMoreMenuClicked

    /** Expanding this while it draws attention to "Summarize page" counts as the user noticing that feature. */
    override fun onEvent(event: MenuEvent, menu: MenuHost) {
        if (
            lastSubMenuItems.any { it.isSummarizePageMenuItem() } &&
                target.browserSessionFrom(browserStore.state)?.content?.private == false &&
                summarizationSettings.shouldHighlightOverflowMenuItem
        ) {
            summarizationSettings.cacheDiscoveryEvent(SummarizeDiscoveryEvent.MenuOverflowInteraction)
        }
    }

    private fun icon(isHighlighted: Boolean) =
        MenuItemIconRes(iconsR.drawable.mozac_ic_ellipsis_horizontal_24, isHighlighted = isHighlighted)
}
