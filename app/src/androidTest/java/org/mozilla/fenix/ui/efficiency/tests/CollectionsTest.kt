/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.MockBrowserDataHelper
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.selectors.CollectionsSelectors

class CollectionsTest : BaseTest() {

    private val mockWebServer get() = fenixTestRule.mockWebServer

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/343422
    @SmokeTest
    @Test
    fun verifyExpandedCollectionItemsTest() {
        val collectionTitle = "First Collection"
        val firstTestPage = mockWebServer.getGenericAsset(1)
        val secondTestPage = mockWebServer.getGenericAsset(2)
        composeRule.activityRule.applySettingsExceptions {
            // Disabling these features to have better visibility of the Collections view
            it.isRecentlyVisitedFeatureEnabled = false
            it.isRecentTabsFeatureEnabled = false
        }

        MockBrowserDataHelper
            .createCollection(
                Pair(firstTestPage.url.toString(), firstTestPage.title),
                Pair(secondTestPage.url.toString(), secondTestPage.title),
                title = collectionTitle,
            )
        on.home.navigateToPage()

        on.collections
            .mozVerify(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozClick(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(firstTestPage.title))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_URL(firstTestPage.url.host.toString()))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(secondTestPage.title))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_URL(secondTestPage.url.host.toString()))
            .mozVerifyElementsByGroup("collectionControls")
             // Collapse collection
            .mozClick(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerifyElementAbsent(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(firstTestPage.title))
            .mozVerifyElementAbsent(CollectionsSelectors.COLLECTION_TAB_WITH_URL(firstTestPage.url.host.toString()))
             // Re-expand collection
            .mozClick(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(firstTestPage.title))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_URL(firstTestPage.url.host.toString()))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(secondTestPage.title))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_URL(secondTestPage.url.host.toString()))
            .mozVerifyElementsByGroup("collectionControls")
             // Collapse collection again
            .mozClick(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerifyElementAbsent(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(firstTestPage.title))
            .mozVerifyElementAbsent(CollectionsSelectors.COLLECTION_TAB_WITH_URL(firstTestPage.url.host.toString()))
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/343425
    @SmokeTest
    @Test
    fun openAllTabsFromACollectionTest() {
        val collectionTitle = "First Collection"
        val firstTestPage = mockWebServer.getGenericAsset(1)
        val secondTestPage = mockWebServer.getGenericAsset(2)
        composeRule.activityRule.applySettingsExceptions {
            // Disabling these features to have better visibility of the Collections view
            it.isRecentlyVisitedFeatureEnabled = false
            it.isRecentTabsFeatureEnabled = false
        }

        MockBrowserDataHelper
            .createCollection(
                Pair(firstTestPage.url.toString(), firstTestPage.title),
                Pair(secondTestPage.url.toString(), secondTestPage.title),
                title = collectionTitle,
            )
        on.home.navigateToPage()
        on.collections
            .mozVerify(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozClick(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(firstTestPage.title))
            .mozClick(CollectionsSelectors.COLLECTION_TAB_MAIN_MENU_BUTTON)
            .mozClick(CollectionsSelectors.OPEN_TABS_BUTTON)

        on.tabDrawer.verifyExistingOpenTabs(firstTestPage.title, secondTestPage.title)
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/343426
    @SmokeTest
    @Test
    fun shareAllTabsFromACollectionTest() {
        val collectionTitle = "First Collection"
        val firstTestPage = mockWebServer.getGenericAsset(1)
        val secondTestPage = mockWebServer.getGenericAsset(2)
        composeRule.activityRule.applySettingsExceptions {
            // Disabling these features to have better visibility of the Collections view
            it.isRecentlyVisitedFeatureEnabled = false
            it.isRecentTabsFeatureEnabled = false
        }

        MockBrowserDataHelper
            .createCollection(
                Pair(firstTestPage.url.toString(), firstTestPage.title),
                Pair(secondTestPage.url.toString(), secondTestPage.title),
                title = collectionTitle,
            )
        on.home.navigateToPage()
        on.collections
            .mozVerify(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozClick(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(firstTestPage.title))
            .mozClick(CollectionsSelectors.COLLECTION_TAB_SHARE_BUTTON)
        on.shareOverlay.mozVerifyElementsByGroup("shareTabLayout")
        on.shareOverlay.verifySharingWithSelectedApp(
            appName = Constants.GMAIL_APP_NAME,
            appPackageName = Constants.PackageName.GMAIL_APP,
            content = "${firstTestPage.url}\n\n${secondTestPage.url}",
            subject = collectionTitle,
        )
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/343428
    @SmokeTest
    @Test
    fun deleteCollectionTest() {
        val collectionTitle = "First Collection"
        val firstTestPage = mockWebServer.getGenericAsset(1)
        val secondTestPage = mockWebServer.getGenericAsset(2)
        composeRule.activityRule.applySettingsExceptions {
            // Disabling these features to have better visibility of the Collections view
            it.isRecentlyVisitedFeatureEnabled = false
            it.isRecentTabsFeatureEnabled = false
        }

        MockBrowserDataHelper
            .createCollection(
                Pair(firstTestPage.url.toString(), firstTestPage.title),
                Pair(secondTestPage.url.toString(), secondTestPage.title),
                title = collectionTitle,
            )
        on.home.navigateToPage()
        on.collections
            .mozVerify(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozClick(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
            .mozVerify(CollectionsSelectors.COLLECTION_TAB_WITH_TITLE(firstTestPage.title))
            .mozClick(CollectionsSelectors.COLLECTION_TAB_MAIN_MENU_BUTTON)
            .mozClick(CollectionsSelectors.DELETE_COLLECTION_BUTTON)
        on.home.mozVerifyElementAbsent(CollectionsSelectors.COLLECTION_WITH_TITLE(collectionTitle))
    }
}
