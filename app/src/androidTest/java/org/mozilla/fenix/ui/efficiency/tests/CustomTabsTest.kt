/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.CustomTabsSelectors

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
}
