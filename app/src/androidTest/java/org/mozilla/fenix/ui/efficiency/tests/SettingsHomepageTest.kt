/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors.BOOKMARKS_SECTION_TITLE
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors.CONTINUE_SECTION
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors.BOOKMARK_THIS_PAGE_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.SettingsHomepageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsHomepageSelectors.CONTINUE_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.SettingsHomepageSelectors.RECENT_BOOKMARKS_BUTTON
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSelectors

class SettingsHomepageTest : BaseTest() {

    @Ignore("Covered by verifyNavigationReachability[1: SettingsHomepagePage (TBD) — Navigation Reachability]")
    @Test
    fun verifySettingsHomepageLoadsTest() {
        on.settingsHomepage.navigateToPage()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1564999
    @SmokeTest
    @Test
    fun continueOptionTest() {
        val genericURL = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(genericURL.url.toString())
        on.home.navigateToPage().mozVerifyElementsByGroup(HomeSelectors.Group.CONTINUE)
        on.settingsHomepage.navigateToPage().mozClick(CONTINUE_BUTTON)
        on.home.navigateToPage().mozVerifyElementAbsent(CONTINUE_SECTION)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1565000
    @SmokeTest
    @Test
    fun recentBookmarksOptionTest() {
        val genericURL = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(genericURL.url.toString())
        on.mainMenu.navigateToPage().mozClick(BOOKMARK_THIS_PAGE_BUTTON)
        on.browserPage.navigateToPage()
        on.home.navigateToPage().mozVerifyElementsByGroup(HomeSelectors.Group.RECENT_BOOKMARKS_SECTION)
        on.settingsHomepage.navigateToPage().mozClick(RECENT_BOOKMARKS_BUTTON)
        on.home.navigateToPage().mozVerifyElementAbsent(BOOKMARKS_SECTION_TITLE)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1569831
    @SmokeTest
    @Test
    fun verifyOpeningScreenOptionsTest() {
        val genericURL = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(genericURL.url.toString())
        on.settings
            .navigateToPage()
            .mozVerify(
                SettingsSelectors.SETTING_OPTION_SUMMARY(
                    settingName = "Homepage",
                    settingSummary = "Open on homepage after four hours",
                )
            )
        on.settingsHomepage
            .navigateToPage()
            .mozVerifyElementIsChecked(
                SettingsHomepageSelectors.OPENING_SCREEN_OPTION("Homepage after four hours of inactivity")
            )
            .mozClick(SettingsHomepageSelectors.OPENING_SCREEN_OPTION("Last tab"))
            .mozVerifyElementIsChecked(SettingsHomepageSelectors.OPENING_SCREEN_OPTION("Last tab"))

        restartApp(composeRule.activityRule)

        on.browserPage.navigateToPage().verifyUrl(genericURL.url.toString())
        on.settings
            .navigateToPage()
            .mozVerify(
                SettingsSelectors.SETTING_OPTION_SUMMARY(settingName = "Homepage", settingSummary = "Open on last tab")
            )
        on.settingsHomepage
            .navigateToPage()
            .mozClick(SettingsHomepageSelectors.OPENING_SCREEN_OPTION("Homepage"))
            .mozVerifyElementIsChecked(SettingsHomepageSelectors.OPENING_SCREEN_OPTION("Homepage"))
        on.settings
            .navigateToPage()
            .mozVerify(
                SettingsSelectors.SETTING_OPTION_SUMMARY(settingName = "Homepage", settingSummary = "Open on homepage")
            )

        restartApp(composeRule.activityRule)

        on.home.mozVerify(HomeSelectors.HOMEPAGE_VIEW)
    }
}
