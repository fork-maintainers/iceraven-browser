/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.selectors

import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy

object RecentlyClosedTabsSelectors {

    // Toolbar title is present whether the list is empty or populated, so it (not the empty-state view)
    // is the reliable page-arrival signal for navigateToPage().
    val MENU_TITLE = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT,
        value = "Recently closed tabs",
        description = "Recently closed tabs toolbar title",
        groups = listOf("requiredForPage"),
    )

    // Only present when there are no recently-closed tabs. Regrouped out of requiredForPage so the page
    // is reachable/assertable in the populated state too.
    val EMPTY_RECENTLY_CLOSED_TABS_LIST = Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID,
        value = "recently_closed_empty_view",
        description = "Empty recently closed tabs view",
        groups = listOf("emptyRecentlyClosedTabsList"),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun RECENTLY_CLOSED_ITEM(title: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID_AND_TEXT,
        value = "title",
        secondaryValue = title,
        description = "Recently closed tab item with title: $title",
        groups = listOf(),
    )

    @Suppress("ktlint:standard:function-naming", "FunctionName")
    fun RECENTLY_CLOSED_ITEM_URL(url: String = "") = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
        value = url,
        description = "Recently closed tab item with url: $url",
        groups = listOf(),
    )

    val ITEM_DELETE_BUTTON = Selector(
        strategy = SelectorStrategy.UIAUTOMATOR_WITH_RES_ID,
        value = "overflow_menu",
        description = "Recently closed tab item delete button",
        groups = listOf(),
    )

    val all = listOf(
        MENU_TITLE,
        EMPTY_RECENTLY_CLOSED_TABS_LIST,
        RECENTLY_CLOSED_ITEM(),
        RECENTLY_CLOSED_ITEM_URL(),
        ITEM_DELETE_BUTTON,
    )
}
