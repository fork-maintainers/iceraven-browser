/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.CustomTabsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.FindInPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class CustomTabsTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/249644
    // Converted from legacy CustomTabsTest.verifyCustomTabMenuItemsTest
    @SmokeTest
    @Test
    fun verifyCustomTabMenuItemsTest() {
        val customMenuItem = "TestMenuItem"
        val customTabPage = mockWebServer.getGenericAsset(1)

        on.customTabs.launchCustomTab(customTabPage.url.toString(), customMenuItem)
        on.customTabs.mozVerify(CustomTabsSelectors.CLOSE_BUTTON)
        on.customTabs.openMainMenu()
        on.customTabs.mozVerifyElementsByGroup("customTabMainMenuItems")
        on.customTabs.mozVerify(CustomTabsSelectors.MENU_CUSTOM_ITEM(customMenuItem))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/249645
    // Converted from legacy CustomTabsTest.openCustomTabInFirefoxTest
    @SmokeTest
    @Test
    fun openCustomTabInFirefoxTest() {
        val customTabPage = mockWebServer.getGenericAsset(1)

        on.customTabs.launchCustomTab(customTabPage.url.toString())
        on.customTabs.mozVerify(CustomTabsSelectors.CLOSE_BUTTON)
        on.customTabs.openMainMenu()
            .mozClick(CustomTabsSelectors.MENU_OPEN_IN_APP)
        // "Open in Firefox" sends the page to the full browser — re-sync confirms we landed there.
        on.browserPage.navigateToPage()
        on.browserPage.verifyPageContent(customTabPage.content)
        on.browserPage.mozVerify(ToolbarSelectors.TAB_COUNTER_WITH_COUNT("1"))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080096
    // Converted from legacy MainMenuTest.verifyTheMainMenuBackButtonFromCustomTabTest
    @SmokeTest
    @Test
    fun verifyTheMainMenuBackButtonFromCustomTabTest() {
        val customTabPage = mockWebServer.getGenericAsset(4)

        on.customTabs.launchCustomTab(customTabPage.url.toString(), "TestMenuItem")
        on.customTabs.clickWebContent("Link 1")
        on.customTabs.openMainMenu()
            .mozClick(CustomTabsSelectors.MENU_BACK)
        on.customTabs.verifyWebContent(customTabPage.content)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080097
    // Converted from legacy MainMenuTest.verifyTheMainMenuForwardButtonFromCustomTabTest
    @SmokeTest
    @Test
    fun verifyTheMainMenuForwardButtonFromCustomTabTest() {
        val firstCustomTabPage = mockWebServer.getGenericAsset(4)
        val secondCustomTabPage = mockWebServer.getGenericAsset(1)

        on.customTabs.launchCustomTab(firstCustomTabPage.url.toString(), "TestMenuItem")
        on.customTabs.clickWebContent("Link 1")
        on.customTabs.openMainMenu()
            .mozClick(CustomTabsSelectors.MENU_BACK)
        on.customTabs.verifyWebContent(firstCustomTabPage.content)
        on.customTabs.openMainMenu()
            .mozClick(CustomTabsSelectors.MENU_FORWARD)
        on.customTabs.verifyWebContent(secondCustomTabPage.content)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3080101
    // Converted from legacy MainMenuTest.verifyTheFindInPageMenuItemInACustomTabTest
    @SmokeTest
    @Test
    fun verifyTheFindInPageMenuItemInACustomTabTest() {
        val customTabPage = mockWebServer.getGenericAsset(3)

        on.customTabs.launchCustomTab(customTabPage.url.toString())
        on.findInPage.navigateToPage()
            .mozVerify(FindInPageSelectors.FIND_IN_PAGE_NEXT_BUTTON)
            .mozVerify(FindInPageSelectors.FIND_IN_PAGE_PREV_BUTTON)
            .mozVerify(FindInPageSelectors.FIND_IN_PAGE_CLOSE_BUTTON)
        on.findInPage.verifyFindInPageElement("a", 3)
        on.findInPage.mozVerifyElementAbsent(FindInPageSelectors.FIND_IN_PAGE_BAR)
        // Parity gap: the legacy test then reopened find-in-page, searched "3" for a 1/1 result and closed
        // with the device back button. Reopening is not expressible yet — closing the bar returns to the
        // custom tab but leaves PageStateTracker on FindInPagePage, and there is no return edge to re-sync
        // it, so navigateToPage() would look for a FindInPagePage -> FindInPagePage path. Same stateful-nav
        // gap as the BookmarksPage -> BrowserPage one.
    }
}
