/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.saveLoginAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest

class SettingsPasswordsTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @Ignore("Covered by verifyNavigationReachability[1: SettingsPasswordsPage (TBD) — Navigation Reachability]")
    @Test
    fun verifySettingsPasswordsLoadsTest() {
        on.settingsPasswords.navigateToPage()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/517818
    // Converted from legacy LoginsTest.verifyNeverSaveLoginOptionTest
    @SmokeTest
    @Test
    fun verifyNeverSaveLoginOptionTest() {
        val loginPage = mockWebServer.saveLoginAsset

        on.settingsSavePasswords.navigateToPage()
        on.settingsSavePasswords.clickNeverSaveOption()

        on.browserPage.navigateToPage(loginPage.url.toString())
        on.browserPage.clickSubmitLoginButton()
        on.browserPage.verifySaveLoginPromptIsNotDisplayed()
    }

    @Ignore("Covered by verifyNavigationReachability[1: SettingsSavePasswordsPage (TBD) — Navigation Reachability]")
    @Test
    fun verifySettingsSavePasswordsSectionTest() {
        on.settingsSavePasswords.navigateToPage()
    }
}
