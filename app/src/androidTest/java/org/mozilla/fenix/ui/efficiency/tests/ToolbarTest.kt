/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

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
}
