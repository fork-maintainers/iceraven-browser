/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.TestAssetHelper.enhancedTrackingProtectionAsset
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors

class SettingsAddonsTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    // Converted from legacy SettingsAddonsTest.verifyUBlockWorksInPrivateModeTest
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/561594
    @SmokeTest
    @Test
    fun verifyUBlockWorksInPrivateModeTest() {
        val addonName = "uBlock Origin"
        val webPage = "https://mozilla-mobile.github.io/testapp/"

        on.settingsAddonsManager.navigateToPage()
            .installAddon(addonName, allowInPrivateBrowsing = true)
        on.home.navigateToPage()
        on.browserPage.navigateToPage(webPage)
            .verifyPageContent("Lets test!")
        on.mainMenu.navigateToPage()
            .mozClick(MainMenuSelectors.EXTENSIONS_BUTTON_UIAUTOMATOR)
            .mozVerify(MainMenuSelectors.EXTENSIONS_BUTTON_WITH_INSTALLED_EXTENSION(addonName))
    }

    // Converted from legacy SettingsAddonsTest.noCrashWithAddonInstalledTest
    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/561600
    @SmokeTest
    @Test
    fun noCrashWithAddonInstalledTest() {
        // Strict ETP exercises the "add-ons work with strict tracking protection" path (legacy setStrictETP).
        appContext.components.settings.setStrictETP()

        val uBlockAddon = "uBlock Origin"
        val darkReaderAddon = "Dark Reader"
        val trackingProtectionPage = mockWebServer.enhancedTrackingProtectionAsset

        on.settingsAddonsManager.navigateToPage()
            .installAddon(uBlockAddon)
            .installAddon(darkReaderAddon)
        on.home.navigateToPage()
        on.browserPage.navigateToPage(trackingProtectionPage.url.toString())
            .verifyUrl(trackingProtectionPage.url.toString())
        on.home.navigateToPage()
            .mozClick(HomeSelectors.TOP_SITE_ITEM("Wikipedia"))
        on.browserPage
            .mozVerify(BrowserPageSelectors.ENGINE_VIEW)
            .navigateToPage()
        on.settings.navigateToPage()
            .mozVerifyElementsByGroup("settingsView")
    }
}
