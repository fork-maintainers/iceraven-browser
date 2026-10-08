/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.Critical
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsPrivateBrowsingSelectors

class SettingsPrivateBrowsingTest : BaseTest() {

    @Ignore("Covered by verifyNavigationReachability[1: SettingsPrivateBrowsingPage (TBD) — Navigation Reachability]")
    @Test
    fun verifyTheSettingsPrivateBrowsingTest() {
        on.settingsPrivateBrowsing.navigateToPage()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/555822
    @Critical
    @Test
    fun verifyPrivateBrowsingMenuItemsTest() {
        on.settingsPrivateBrowsing
            .navigateToPage()
            .mozVerifyElementsByGroup(SettingsPrivateBrowsingSelectors.Group.MENU_ITEMS_VIEW)
            .mozVerifyOptionSwitchIsNotChecked(SettingsPrivateBrowsingSelectors.OPEN_LINKS_IN_PRIVATE_TAB)
            .mozVerifyOptionSwitchIsNotChecked(SettingsPrivateBrowsingSelectors.ALLOW_SCREENSHOTS_IN_PRIVATE_BROWSING)
            .mozVerifyOptionSwitchIsNotChecked(SettingsPrivateBrowsingSelectors.USE_SCREEN_LOCK_TO_HIDE_TABS)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2354877
    @Critical
    @Test
    fun openInNonPrivateTabOptionTest() {
        val testPage = mockWebServer.getGenericAsset(1)

        on.home.navigateToPage()

        on.settingsPrivateBrowsing.navigateToPage()
        on.settingsPrivateBrowsing
            .toggleOpenLinksInAPrivateTab()
            .mozVerifyOptionSwitchIsChecked(SettingsPrivateBrowsingSelectors.OPEN_LINKS_IN_PRIVATE_TAB)

        on.home.navigateToPage()
        on.home.switchToPrivateBrowsingMode()
        on.browserPage.navigateToPage(testPage.url.toString())
        on.mainMenu
            .navigateToPage()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.MOVE_TO_NON_PRIVATE_TAB_BUTTON)
        // Moving the tab is untracked; poll readiness before re-anchoring or navigateToPage checks the stale page.
        on.browserPage.mozVerifyReadiness()
        on.browserPage.navigateToPage()
        on.browserPage.verifyTabCounter("1")
        // Reload-on-retry: the freshly moved tab can briefly show a blank engine view before content settles.
        on.browserPage.verifyPageContentWithReload(testPage.url.toString(), testPage.content)

        // Enter Settings from Home so the exit uses the RETURN_SURFACE_HOME back edge, not a flakier path.
        on.home.navigateToPage()
        on.settingsPrivateBrowsing.navigateToPage()
        on.settingsPrivateBrowsing
            .toggleOpenLinksInAPrivateTab()
            .mozVerifyOptionSwitchIsNotChecked(SettingsPrivateBrowsingSelectors.OPEN_LINKS_IN_PRIVATE_TAB)

        // "Move to non-private tab" stays available in a private tab regardless of the setting.
        on.home.navigateToPage()
        on.home.switchToPrivateBrowsingMode()
        on.browserPage.navigateToPage(testPage.url.toString())
        on.mainMenu
            .navigateToPage()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozVerify(MainMenuSelectors.MOVE_TO_NON_PRIVATE_TAB_BUTTON)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/652556
    @SmokeTest
    @Test
    fun allowScreenshotsInPrivateBrowsingTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        on.home.navigateToPage()
        on.home.switchToPrivateBrowsingMode()
        on.browserPage.navigateToPage(defaultWebPage.url.toString())
        on.browserPage.verifyScreenshotsAllowed(false)

        on.settingsPrivateBrowsing.navigateToPage()
        on.settingsPrivateBrowsing.toggleAllowScreenshotsInPrivateBrowsing()

        on.browserPage.navigateToPage()
        on.browserPage.verifyScreenshotsAllowed(true)
    }
}
