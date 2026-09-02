/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.storageCheckPageAsset
import org.mozilla.fenix.helpers.TestAssetHelper.storageWritePageAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors

class SettingsDeleteBrowsingDataTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @Ignore("Covered by verifyNavigationReachability[1: SettingsDeleteBrowsingDataPage (TBD) — Navigation Reachability]")
    @Test
    fun verifyTheDeleteBrowsingDataSectionTest() {
        on.settingsDeleteBrowsingData.navigateToPage()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/353531
    @SmokeTest
    @Test
    fun deleteOpenTabsBrowsingDataTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(defaultWebPage.url.toString())

        on.settingsDeleteBrowsingData.navigateToPage()
            .verifyAllCheckBoxesAreChecked()
            .selectOnlyOpenTabsCheckBox()
            .clickDeleteBrowsingDataButton()
            .verifyDeleteBrowsingDataDialog()
            .clickDialogCancelButton()
            .verifyOpenTabsCheckBox(true)
            .clickDeleteBrowsingDataButton()
            .verifyDeleteBrowsingDataDialog()
            .confirmDeletionAndAssertSnackbar()
            .verifyOpenTabsDetails("0")

        on.tabDrawer.navigateToPage()
            .verifyNoOpenTabsInNormalBrowsing()
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/378864
    @SmokeTest
    @Test
    fun deleteBrowsingHistoryTest() {
        val genericPage = mockWebServer.getGenericAsset(1)

        on.browserPage.navigateToPage(genericPage.url.toString())

        on.settingsDeleteBrowsingData.navigateToPage()
            .verifyBrowsingHistoryDetails("1")
            .selectOnlyBrowsingHistoryCheckBox()
            .clickDeleteBrowsingDataButton()
            .verifyDeleteBrowsingDataDialog()
            .clickDialogCancelButton()
            .verifyBrowsingHistoryCheckBox(true)
            .clickDeleteBrowsingDataButton()
            .verifyDeleteBrowsingDataDialog()
            .confirmDeletionAndAssertSnackbar()
            .verifyBrowsingHistoryDetails("0")

        on.history.navigateToPage()
            .mozVerifyElementsByGroup("emptyHistoryMenuView")
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/416041
    @SmokeTest
    @Test
    fun deleteCookiesAndSiteDataTest() {
        val genericPage = mockWebServer.getGenericAsset(1)
        val storageWritePage = mockWebServer.storageWritePageAsset.url.toString()
        val storageCheckPage = mockWebServer.storageCheckPageAsset.url.toString()

        // Browse a generic page first so GeckoView is loaded on a fresh run.
        on.browserPage.navigateToPage(genericPage.url.toString())

        on.browserPage.navigateToPage(storageWritePage, forceNavigation = true)
            .verifyPageContent("No cookies set")
            .clickPageObjectUntilContent(
                BrowserPageSelectors.SET_COOKIES_WEB_BUTTON,
                storageWritePage,
                "user=android",
            )

        on.browserPage.navigateToPage(storageCheckPage, forceNavigation = true)
            .verifyPageContent("Session storage has value")
            .verifyPageContent("Local storage has value")

        on.settingsDeleteBrowsingData.navigateToPage()
            .selectOnlyCookiesCheckBox()
            .clickDeleteBrowsingDataButton()
            .verifyDeleteBrowsingDataDialog()
            .clickDialogCancelButton()
            .verifyCookiesCheckBox(true)
            .clickDeleteBrowsingDataButton()
            .verifyDeleteBrowsingDataDialog()
            .confirmDeletionAndAssertSnackbar()

        on.browserPage.navigateToPage(storageCheckPage, forceNavigation = true)
            .verifyPageContent("Session storage empty")
            .verifyPageContent("Local storage empty")

        on.browserPage.navigateToPage(storageWritePage, forceNavigation = true)
            .verifyPageContent("No cookies set")
    }
}
