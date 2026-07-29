/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.SearchBarSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class ToolbarTest : BaseTest(shouldUseExpandedToolbar = true) {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @Ignore("Covered by verifyNavigationReachability[2: ToolbarPage (TBD)")
    @Test
    fun toolbarItemsTest() {
        // Given: App is loaded with default settings
        // on = AndroidComposeTestRule<HomeActivityIntentTestRule, *> with app defaults

        // When: We navigate to the Homepage page with the Toolbar component visible
        on.home.navigateToPage()
        on.toolbar.navigateToPage()

        // Then: the toolbar elements should load
        on.toolbar.mozVerifyElementsByGroup("requiredForPage")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333215
    // Converted from legacy NavigationToolbarExpandedTest.verifyTheExpandedToolbarMainMenuButtonTest
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarMainMenuButtonTest() {
        val website = mockWebServer.getGenericAsset(1)
        on.browserPage.navigateToPage(website.url.toString())
        on.browserPage.verifyPageContent(website.content)
        on.mainMenu.navigateToPage()
            .mozVerifyElementsByGroup("browserViewMainMenuItems")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333214
    // Converted from legacy NavigationToolbarExpandedTest.verifyTheExpandedToolbarTabTrayButtonTest
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarTabTrayButtonTest() {
        val website = mockWebServer.getGenericAsset(1)
        on.browserPage.navigateToPage(website.url.toString())
        on.browserPage.verifyPageContent(website.content)
        on.tabDrawer.navigateToPage()
        on.tabDrawer.verifyExistingOpenTabs(website.title)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333213
    // Converted from legacy NavigationToolbarExpandedTest.verifyTheExpandedToolbarNewTabButtonTest
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarNewTabButtonTest() {
        val website = mockWebServer.getGenericAsset(1)
        on.browserPage.navigateToPage(website.url.toString())
        on.browserPage.verifyPageContent(website.content)
        // Tap the expanded-toolbar "New tab" button (device-level content-desc, present on the browser
        // view because this class launches with shouldUseExpandedToolbar = true) — opens a new tab in
        // the search/edit view.
        on.browserPage.mozClick(ToolbarSelectors.NEW_TAB_BUTTON)
        // The new tab lands in the search view in edit mode: verify it opened (search-engine selector),
        // the address bar is in edit mode showing its placeholder, and the keyboard came up for input.
        on.searchBar.mozVerifyElementsByGroup("requiredForPage")
        on.searchBar.mozVerify(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE)
        on.searchBar.mozVerify(SearchBarSelectors.SEARCH_BAR_PLACEHOLDER)
        on.searchBar.mozVerifyKeyboardVisible()
    }
}
