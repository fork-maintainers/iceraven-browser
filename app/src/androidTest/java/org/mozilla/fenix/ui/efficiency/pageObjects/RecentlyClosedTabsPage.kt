/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.HistorySelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.RecentlyClosedTabsSelectors

class RecentlyClosedTabsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "RecentlyClosedTabsPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.HISTORY_BUTTON),
                NavigationStep.Click(HistorySelectors.RECENTLY_CLOSED_TABS_BUTTON),
            ),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return RecentlyClosedTabsSelectors.all.filter { it.groups.contains(group) }
    }

    fun verifyRecentlyClosedItem(title: String, url: String): RecentlyClosedTabsPage {
        mozVerify(RecentlyClosedTabsSelectors.RECENTLY_CLOSED_ITEM(title))
        mozVerify(RecentlyClosedTabsSelectors.RECENTLY_CLOSED_ITEM_URL(url))
        return this
    }

    fun openRecentlyClosedItem(title: String): RecentlyClosedTabsPage {
        mozClick(RecentlyClosedTabsSelectors.RECENTLY_CLOSED_ITEM(title))
        return this
    }

    fun deleteRecentlyClosedItem(): RecentlyClosedTabsPage {
        mozClick(RecentlyClosedTabsSelectors.ITEM_DELETE_BUTTON)
        return this
    }

    fun verifyEmptyRecentlyClosedList(): RecentlyClosedTabsPage {
        mozVerify(RecentlyClosedTabsSelectors.EMPTY_RECENTLY_CLOSED_TABS_LIST)
        return this
    }
}
