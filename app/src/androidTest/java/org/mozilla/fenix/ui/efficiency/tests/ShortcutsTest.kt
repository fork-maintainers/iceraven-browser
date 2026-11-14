/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.Critical
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.home.topsites.middleware.POPULAR_SITES_LIMIT
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors

class ShortcutsTest : BaseTest() {

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/532598
    @SmokeTest
    @Test
    fun addAWebsiteAsATopSiteTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        on.home.navigateToPage().mozVerifyElementsByGroup(HomeSelectors.Group.TOP_SITES_COMPOSE)
        on.browserPage.navigateToPage(defaultWebPage.url.toString()).verifyPageContent(defaultWebPage.content)
        on.mainMenu
            .navigateToPage()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozVerify(MainMenuSelectors.ADD_TO_SHORTCUTS_BUTTON)
            .mozClick(MainMenuSelectors.ADD_TO_SHORTCUTS_BUTTON)
        on.browserPage.navigateToPage().mozVerify(BrowserPageSelectors.ADDED_TO_SHORTCUTS_SNACKBAR_TEXT)
        on.home
            .navigateToPage()
            .mozVerifyElementsByGroup(HomeSelectors.Group.TOP_SITES_COMPOSE)
            .mozVerify(HomeSelectors.TOP_SITE_ITEM(defaultWebPage.title))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4227423
    @Critical
    @Test
    fun addPopularSiteAsShortcutTest() {
        on.home
            .navigateToPage()
            .mozVerifyElementsByGroup(HomeSelectors.Group.TOP_SITES_COMPOSE)
            .mozClick(HomeSelectors.ADD_SHORTCUT_BUTTON)
            .mozVerifyElementsByGroup(HomeSelectors.Group.ADD_TO_HOMEPAGE_BOTTOM_SHEET)
            .mozVerifyElementCount(
                HomeSelectors.ADD_TO_HOMEPAGE_BOTTOM_SHEET_POPULAR_SITES,
                count = POPULAR_SITES_LIMIT,
            )

        // The suggestions are data-driven, so read the first one's title instead of hardcoding it. Tapping
        // the cell pins it as a top site and closes the sheet, then it must appear on the homepage.
        val firstPopularSiteTitle = on.home.getFirstPopularSiteTitle()

        on.home
            .mozClick(HomeSelectors.ADD_TO_HOMEPAGE_BOTTOM_SHEET_POPULAR_SITE_ITEM(firstPopularSiteTitle))
            .mozVerify(HomeSelectors.ADDED_TO_SHORTCUTS_SNACKBAR_TEXT)
            .mozVerify(HomeSelectors.TOP_SITE_ITEM(firstPopularSiteTitle))
    }
}
