/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.junit.Test
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AppAndSystemHelper.enableOrDisableBackGestureNavigationOnDevice
import org.mozilla.fenix.helpers.AppAndSystemHelper.setScreenOrientation
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.BookmarksSelectors
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SearchBarSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class NavigationToolbarExpandedTest : BaseTest(shouldUseExpandedToolbar = true) {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333211
    // Converted from legacy NavigationToolbarExpandedTest.verifyTheExpandedToolbarAddBookmarkButtonTest
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarAddBookmarkButtonTest() {
        val website = mockWebServer.getGenericAsset(1)
        on.browserPage.navigateToPage(website.url.toString())
        on.browserPage.mozClick(ToolbarSelectors.EXPANDED_TOOLBAR_ADD_BOOKMARK_BUTTON)
        on.browserPage.mozWaitUntilAbsent(BrowserPageSelectors.SNACKBAR)
        on.browserPage.mozClick(ToolbarSelectors.EXPANDED_TOOLBAR_EDIT_BOOKMARK_BUTTON)
        on.bookmarks.mozVerifyElementsByGroup("editBookmarksView")
        on.bookmarks.mozClick(BookmarksSelectors.DELETE_BOOKMARK_BUTTON)
        on.browserPage.verifyPageContent(website.content)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333212
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarShareButtonTest() {
        val website = mockWebServer.getGenericAsset(1)

        // Disable the edge back-gesture so it doesn't intercept taps on the bottom navigation bar.
        enableOrDisableBackGestureNavigationOnDevice(backGestureNavigationEnabled = false)

        on.browserPage.navigateToPage(website.url.toString())
        on.browserPage.mozClick(ToolbarSelectors.EXPANDED_TOOLBAR_SHARE_BUTTON)
        on.shareOverlay.mozVerifyElementsByGroup("shareTabLayout")
        on.shareOverlay.verifySharingWithSelectedApp(
            appName = Constants.GMAIL_APP_NAME,
            appPackageName = Constants.PackageName.GMAIL_APP,
            content = website.url.toString(),
            subject = website.title,
        )
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333175
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarItemsInLandscapeModeTest() {
        // setScreenOrientation requires the concretely-typed compose rule; BaseTest exposes it star-projected
        // but the runtime activity is always HomeActivity (see BaseTest's `{ it.activity }`).
        @Suppress("UNCHECKED_CAST")
        val orientationRule = composeRule as AndroidComposeTestRule<HomeActivityIntentTestRule, HomeActivity>

        val website = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(website.url.toString())
            .verifyPageContent(website.content)

        setScreenOrientation(orientationRule, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        on.browserPage.verifyToolbarIsAtTop()
        on.browserPage.verifyUrl(website.url.toString())
        on.browserPage.mozVerify(ToolbarSelectors.SITE_INFO_BUTTON)
        on.browserPage.mozVerify(ToolbarSelectors.EXPANDED_TOOLBAR_BACK_BUTTON)
        on.browserPage.mozVerify(ToolbarSelectors.EXPANDED_TOOLBAR_FORWARD_BUTTON)
        on.browserPage.mozVerify(ToolbarSelectors.EXPANDED_TOOLBAR_REFRESH_BUTTON)
        on.browserPage.mozVerify(ToolbarSelectors.EXPANDED_TOOLBAR_SHARE_BUTTON)
        on.browserPage.mozVerify(ToolbarSelectors.NEW_TAB_BUTTON)
        on.browserPage.mozVerify(ToolbarSelectors.TAB_COUNTER_WITH_COUNT("1"))
        on.browserPage.mozVerify(BrowserPageSelectors.MAIN_MENU_BUTTON)

        setScreenOrientation(orientationRule, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333183
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarNewTabButtonInLandscapeModeTest() {
        // setScreenOrientation requires the concretely-typed compose rule; BaseTest exposes it star-projected
        // but the runtime activity is always HomeActivity (see BaseTest's `{ it.activity }`).
        @Suppress("UNCHECKED_CAST")
        val orientationRule = composeRule as AndroidComposeTestRule<HomeActivityIntentTestRule, HomeActivity>

        val website = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(website.url.toString())
            .verifyPageContent(website.content)

        setScreenOrientation(orientationRule, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        // Tapping New tab opens a fresh tab focused on the search bar.
        on.browserPage.mozClick(ToolbarSelectors.NEW_TAB_BUTTON)
        on.searchBar.mozVerify(SearchBarSelectors.SEARCH_BAR_PLACEHOLDER)
        on.searchBar.mozVerifyKeyboardVisible()

        setScreenOrientation(orientationRule, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333184
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarTabTrayButtonInLandscapeModeTest() {
        // setScreenOrientation requires the concretely-typed compose rule; BaseTest exposes it star-projected
        // but the runtime activity is always HomeActivity (see BaseTest's `{ it.activity }`).
        @Suppress("UNCHECKED_CAST")
        val orientationRule = composeRule as AndroidComposeTestRule<HomeActivityIntentTestRule, HomeActivity>

        val website = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(website.url.toString())
            .verifyPageContent(website.content)

        setScreenOrientation(orientationRule, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        // The tab-counter button opens the tab tray; navigateToPage routes through that edge.
        on.tabDrawer.navigateToPage()
        on.tabDrawer.verifyExistingOpenTabs(website.title)

        setScreenOrientation(orientationRule, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3333185
    @SmokeTest
    @Test
    fun verifyTheExpandedToolbarMainMenuButtonInLandscapeModeTest() {
        // setScreenOrientation requires the concretely-typed compose rule; BaseTest exposes it star-projected
        // but the runtime activity is always HomeActivity (see BaseTest's `{ it.activity }`).
        @Suppress("UNCHECKED_CAST")
        val orientationRule = composeRule as AndroidComposeTestRule<HomeActivityIntentTestRule, HomeActivity>

        val website = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(website.url.toString())
            .verifyPageContent(website.content)

        setScreenOrientation(orientationRule, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        on.browserPage
            .openMainMenu()
        on.mainMenu
            .mozVerifyElementsByGroup("browserViewMainMenuItems")

        setScreenOrientation(orientationRule, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }
}
