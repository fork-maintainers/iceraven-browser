/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

class RecentlyClosedTabsTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @Ignore("Covered by verifyNavigationReachability[0: RecentlyClosedTabsPage (TBD) — Navigation Reachability]")
    @Test
    fun verifyEmptyRecentlyClosedTabsSectionTest() {
        on.recentlyClosedTabs.navigateToPage()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2195812
    // Converted from legacy RecentlyClosedTabsTest.deleteRecentlyClosedTabsItemTest
    @SmokeTest
    @Test
    fun deleteRecentlyClosedTabsItemTest() {
        val website = mockWebServer.getGenericAsset(1)
        on.browserPage.navigateToPage(website.url.toString())
        on.tabDrawer.navigateToPage()
        on.tabDrawer.closeTabWithTitle(website.title)
        // Closing the only tab lands on Home; re-sync page state before routing onward.
        on.home.navigateToPage()
        on.recentlyClosedTabs.navigateToPage()
        on.recentlyClosedTabs.verifyRecentlyClosedItem(website.title, website.url.toString())
        on.recentlyClosedTabs.deleteRecentlyClosedItem()
        on.recentlyClosedTabs.verifyEmptyRecentlyClosedList()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1065414
    // Converted from legacy RecentlyClosedTabsTest.openRecentlyClosedItemTest
    @SmokeTest
    @Test
    fun openRecentlyClosedItemTest() {
        val website = mockWebServer.getGenericAsset(1)
        on.browserPage.navigateToPage(website.url.toString())
        on.tabDrawer.navigateToPage()
        on.tabDrawer.closeTabWithTitle(website.title)
        // Closing the only tab lands on Home; re-sync page state before routing onward.
        on.home.navigateToPage()
        on.recentlyClosedTabs.navigateToPage()
        on.recentlyClosedTabs.verifyRecentlyClosedItem(website.title, website.url.toString())
        on.recentlyClosedTabs.openRecentlyClosedItem(website.title)
        // Tapping the item reopens it as a browser tab.
        on.browserPage.navigateToPage()
        on.browserPage.verifyUrl(website.url.toString())
    }
}
