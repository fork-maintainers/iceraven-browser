/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.AddToHomeScreenSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.PWASelectors

class PWATest : BaseTest() {

    private val pwaPage = "https://mozilla-mobile.github.io/testapp/loginForm"
    private val shortcutTitle = "TEST_APP"

    // Converted from legacy PwaTest.installPWAFromTheMainMenuTest
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/834200
    @SmokeTest
    @Test
    fun installPWAFromTheMainMenuTest() {
        on.browserPage.navigateToPage(pwaPage)
        on.browserPage.verifyUrl("mozilla-mobile.github.io/testapp/loginForm")
        on.browserPage.verifyPageContentWithReload(pwaPage, "Login Form")
        on.mainMenu.navigateToPage()
            .mozClick(MainMenuSelectors.MORE_BUTTON)
            .mozClick(MainMenuSelectors.ADD_APP_TO_HOMESCREEN_BUTTON)
        on.addToHomescreen.mozClick(AddToHomeScreenSelectors.SYSTEM_PROMPT_ADD_TO_HOME_SCREEN_BUTTON)
        on.addToHomescreen.mozClickIfPresent(
            AddToHomeScreenSelectors.HOME_SCREEN_SHORTCUT(shortcutTitle),
            timeout = waitingTime,
        )
        on.browserPage.mozVerify(PWASelectors.PWA_SCREEN)
        on.browserPage.mozVerifyElementAbsent(PWASelectors.NAV_URL_BAR)
    }
}
