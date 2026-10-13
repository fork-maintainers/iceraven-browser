/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.Critical
import org.mozilla.fenix.helpers.AppAndSystemHelper.assertExternalAppOpens
import org.mozilla.fenix.helpers.Constants.PackageName.YOUTUBE_APP
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithText
import org.mozilla.fenix.helpers.RetryTestRule
import org.mozilla.fenix.helpers.RetryableComposeTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.externalLinksAsset
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.imageAsset
import org.mozilla.fenix.helpers.TestHelper.clickSnackbarButton
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.verifySnackBarText
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.clickContextMenuItem
import org.mozilla.fenix.ui.robots.clickPageObject
import org.mozilla.fenix.ui.robots.downloadRobot
import org.mozilla.fenix.ui.robots.longClickPageObject
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.shareOverlay

/**
 * Tests for verifying basic functionality of content context menus
 *
 * - Verifies long click "Open link in new tab" UI and functionality
 * - Verifies long click "Open link in new Private tab" UI and functionality
 * - Verifies long click "Copy Link" UI and functionality
 * - Verifies long click "Share Link" UI and functionality
 * - Verifies long click "Open image in new tab" UI and functionality
 * - Verifies long click "Save Image" UI and functionality
 * - Verifies long click "Copy image location" UI and functionality
 * - Verifies long click items of mixed hypertext items
 */
class ContextMenusTest {

    @get:Rule(order = 0) val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer
        get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1) val retryTestRule = RetryTestRule(3)

    @get:Rule(order = 2)
    val retryableComposeTestRule = RetryableComposeTestRule {
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule(
                // workaround for toolbar at top position by default
                // remove with https://bugzilla.mozilla.org/show_bug.cgi?id=1917640
                shouldUseBottomToolbar = true
            )
        ) {
            it.activity
        }
    }

    private val composeTestRule
        get() = retryableComposeTestRule.current

    @get:Rule(order = 3) val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243837
    @Converted(
        replacedBy =
            ["org.mozilla.fenix.ui.efficiency.tests.ContextMenusTest#verifyOpenLinkNewTabContextMenuOptionTest"],
        bug = 2079237,
        since = "2026-10",
    )
    @Critical
    @Test
    fun verifyOpenLinkNewTabContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val genericURL = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("Link 1"))
                verifyContextMenuForLocalHostLinks(genericURL.url)
                clickContextMenuItem("Open link in new tab")
                verifySnackBarText("New tab opened")
                clickSnackbarButton(composeTestRule, "SWITCH")
                verifyUrl(genericURL.url.toString())
            }
            .openTabDrawer(composeTestRule) {
                verifyNormalBrowsingButtonIsSelected()
                verifyExistingOpenTabs("Test_Page_1")
                verifyExistingOpenTabs("Test_Page_4")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/244655
    @Test
    fun verifyOpenLinkInNewPrivateTabContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val genericURL = mockWebServer.getGenericAsset(2)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("Link 2"))
                verifyContextMenuForLocalHostLinks(genericURL.url)
                clickContextMenuItem("Open link in private tab")
                verifySnackBarText("New private tab opened")
                clickSnackbarButton(composeTestRule, "SWITCH")
                verifyUrl(genericURL.url.toString())
            }
            .openTabDrawer(composeTestRule) {
                verifyPrivateBrowsingButtonIsSelected()
                verifyExistingOpenTabs("Test_Page_2")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243832
    @Test
    fun verifyCopyLinkContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val genericURL = mockWebServer.getGenericAsset(3)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("Link 3"))
                verifyContextMenuForLocalHostLinks(genericURL.url)
                clickContextMenuItem("Copy link")
                verifySnackBarText("Link copied to clipboard")
            }
            .openNavigationToolbar {}
            .visitLinkFromClipboard {
                verifyUrl(genericURL.url.toString())
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4024997
    @Test
    fun verifyCopyLinkTextContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val genericURL = mockWebServer.getGenericAsset(3)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("Link 3"))
                verifyContextMenuForLocalHostLinks(genericURL.url)
                clickContextMenuItem("Copy link text")
                verifySnackBarText("Link text copied to clipboard")
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243838
    @Test
    fun verifyShareLinkContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val genericURL = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("Link 1"))
                verifyContextMenuForLocalHostLinks(genericURL.url)
                clickContextMenuItem("Share link")
                shareOverlay {
                    verifyShareLinkIntent(genericURL.url)
                    mDevice.pressBack()
                }
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243833
    @Test
    fun verifyOpenImageNewTabContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val imageResource = mockWebServer.imageAsset

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("test_link_image"))
                verifyLinkImageContextMenuItems(imageResource.url)
                clickContextMenuItem("Open image in new tab")
                verifySnackBarText("New tab opened")
                clickSnackbarButton(composeTestRule, "SWITCH")
                verifyUrl(imageResource.url.toString())
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243834
    @Test
    fun verifyCopyImageLocationContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val imageResource = mockWebServer.imageAsset

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("test_link_image"))
                verifyLinkImageContextMenuItems(imageResource.url)
                clickContextMenuItem("Copy image location")
                verifySnackBarText("Link copied to clipboard")
            }
            .openNavigationToolbar {}
            .visitLinkFromClipboard {
                verifyUrl(imageResource.url.toString())
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/243835
    @Test
    fun verifySaveImageContextMenuOptionTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val imageResource = mockWebServer.imageAsset

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("test_link_image"))
                verifyLinkImageContextMenuItems(imageResource.url)
                clickContextMenuItem("Save image")
            }

        downloadRobot(composeTestRule) {
            verifyDownloadCompleteSnackbar(fileName = "rabbit.jpg")
            clickSnackbarButton(composeTestRule = composeTestRule, "OPEN")
            verifyPhotosAppOpens()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/352050
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.ContextMenusTest#verifyContextMenuLinkVariationsTest"],
        bug = 2080182,
        since = "2026-10",
    )
    @Critical
    @Test
    fun verifyContextMenuLinkVariationsTest() {
        val pageLinks = mockWebServer.getGenericAsset(4)
        val genericURL = mockWebServer.getGenericAsset(1)
        val imageResource = mockWebServer.imageAsset

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(pageLinks.url) {
                mDevice.waitForIdle()
                longClickPageObject(composeTestRule, itemWithText("Link 1"))
                verifyContextMenuForLocalHostLinks(genericURL.url)
                dismissContentContextMenu()
                longClickPageObject(composeTestRule, itemWithText("test_link_image"))
                verifyLinkImageContextMenuItems(imageResource.url)
                dismissContentContextMenu()
                longClickPageObject(composeTestRule, itemWithText("test_no_link_image"))
                verifyNoLinkImageContextMenuItems(imageResource.url)
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2333840
    @Test
    fun verifyPDFContextMenuLinkVariationsTest() {
        val genericURL = mockWebServer.getGenericAsset(3)

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(genericURL.url) {
                clickPageObject(composeTestRule, itemWithText("PDF form file"))
                waitForPageToLoad()
                clickPageObject(composeTestRule, itemContainingText("Stay in"))
                longClickPageObject(composeTestRule, itemWithText("Wikipedia link"))
                verifyContextMenuForLinksToOtherHosts("wikipedia.org".toUri())
                dismissContentContextMenu()
                // Some options are missing from the linked and non liked images context menus in PDF files
                // See https://bugzilla.mozilla.org/show_bug.cgi?id=1012805 for more details
                longClickPDFImage()
                verifyContextMenuForLinksToOtherHosts("wikipedia.org".toUri())
                dismissContentContextMenu()
            }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/832094
    @Test
    fun verifyOpenLinkInAppContextMenuOptionTest() {
        val defaultWebPage = mockWebServer.externalLinksAsset

        navigationToolbar(composeTestRule) {}
            .enterURLAndEnterToBrowser(defaultWebPage.url) {
                longClickPageObject(composeTestRule, itemContainingText("Youtube full link"))
                verifyContextMenuForLinksToOtherApps("youtube.com")
                clickContextMenuItem("Open link in external app")
                assertExternalAppOpens(YOUTUBE_APP)
            }
    }
}
